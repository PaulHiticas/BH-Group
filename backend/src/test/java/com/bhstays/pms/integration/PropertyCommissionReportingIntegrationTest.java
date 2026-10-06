package com.bhstays.pms.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bhstays.pms.domain.Address;
import com.bhstays.pms.domain.AuditAction;
import com.bhstays.pms.domain.AuditLog;
import com.bhstays.pms.domain.Payment;
import com.bhstays.pms.domain.PaymentMethod;
import com.bhstays.pms.domain.PaymentProvider;
import com.bhstays.pms.domain.PaymentStatus;
import com.bhstays.pms.domain.Property;
import com.bhstays.pms.domain.PropertyStatus;
import com.bhstays.pms.domain.PropertyType;
import com.bhstays.pms.domain.Reservation;
import com.bhstays.pms.domain.ReservationSource;
import com.bhstays.pms.domain.ReservationStatus;
import com.bhstays.pms.domain.Role;
import com.bhstays.pms.domain.User;
import com.bhstays.pms.domain.UserStatus;
import com.bhstays.pms.dto.report.CommissionSummaryCurrencyTotals;
import com.bhstays.pms.dto.report.CommissionSummaryResponse;
import com.bhstays.pms.dto.report.PropertyCommissionCurrencyResponse;
import com.bhstays.pms.dto.report.PropertyCommissionReportResponse;
import com.bhstays.pms.dto.report.UnconfiguredPropertyResponse;
import com.bhstays.pms.repository.AuditLogRepository;
import com.bhstays.pms.repository.PaymentRepository;
import com.bhstays.pms.repository.PropertyRepository;
import com.bhstays.pms.repository.ReservationRepository;
import com.bhstays.pms.repository.UserRepository;
import com.bhstays.pms.security.SecureTokenGenerator;
import com.bhstays.pms.security.UserPrincipal;
import com.bhstays.pms.service.EmailService;
import com.bhstays.pms.service.PropertyCommissionReportService;
import com.bhstays.pms.service.StripeWebhookService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Commission reporting against a real Postgres: the V40 migration and its
 * CHECK constraints, the JPQL aggregate the reports run on, the payment
 * statuses that count as collected, duplicate Stripe webhook deliveries,
 * and who may read the reports or change a property's commission.
 *
 * <p>The database is shared with the other integration test classes, so
 * every test works on its own properties and its own check-in window and
 * asserts only on those.
 */
@TestPropertySource(properties = {
        "app.stripe.secret-key=sk_test_integration_dummy",
        "app.stripe.publishable-key=pk_test_integration_dummy",
        "app.stripe.webhook-secret=" + PropertyCommissionReportingIntegrationTest.WEBHOOK_SECRET
})
@AutoConfigureMockMvc
class PropertyCommissionReportingIntegrationTest extends AbstractIntegrationTest {

    static final String WEBHOOK_SECRET = "whsec_commission_reporting_test_secret";

    @Autowired private PropertyCommissionReportService reportService;
    @Autowired private StripeWebhookService stripeWebhookService;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private SecureTokenGenerator secureTokenGenerator;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private MockMvc mockMvc;

    @MockitoBean private EmailService emailService;

    // ------------------------------------------------------------------
    // fixtures
    // ------------------------------------------------------------------

    private Property property(String name, String commissionPercent) {
        return propertyRepository.saveAndFlush(Property.builder()
                .name(name + " " + UUID.randomUUID())
                .propertyType(PropertyType.APARTMENT)
                .status(PropertyStatus.ACTIVE)
                .address(new Address("Str. Comisionului 1", "Cluj-Napoca", null, null, "România", null, null))
                .bedrooms(1)
                .bathrooms(1)
                .maxGuests(4)
                .basePricePerNight(new BigDecimal("100.00"))
                .commissionPercent(commissionPercent != null ? new BigDecimal(commissionPercent) : null)
                .build());
    }

    /** total = accommodation + fees; pass a null accommodation for a reservation without breakdown. */
    private Reservation reservation(Property property, LocalDate checkIn, String currency, String total,
                                    String accommodation, String cleaning, String extraGuest) {
        return reservationRepository.saveAndFlush(Reservation.builder()
                .property(property)
                .guestFirstName("Ana")
                .guestLastName("Popescu")
                .guestEmail("ana@example.com")
                .checkInDate(checkIn)
                .checkOutDate(checkIn.plusDays(2))
                .numberOfGuests(2)
                .status(ReservationStatus.CONFIRMED)
                .source(ReservationSource.DIRECT)
                .totalAmount(new BigDecimal(total))
                .currency(currency)
                .accommodationAmount(accommodation != null ? new BigDecimal(accommodation) : null)
                .cleaningFeeAmount(accommodation != null ? new BigDecimal(cleaning) : null)
                .extraGuestFeeAmount(accommodation != null ? new BigDecimal(extraGuest) : null)
                .lateCheckoutFeeAmount(accommodation != null ? BigDecimal.ZERO : null)
                .taxAmount(accommodation != null ? BigDecimal.ZERO : null)
                .addonAmount(accommodation != null ? BigDecimal.ZERO : null)
                .build());
    }

    private Payment payment(Reservation reservation, PaymentStatus status, String amount, String refunded) {
        return paymentRepository.saveAndFlush(Payment.builder()
                .reservation(reservation)
                .provider(PaymentProvider.MANUAL)
                .method(PaymentMethod.BANK_TRANSFER)
                .status(status)
                .amount(new BigDecimal(amount))
                .refundedAmount(new BigDecimal(refunded))
                .currency(reservation.getCurrency())
                .build());
    }

    private User staff(Role role) {
        return userRepository.save(User.builder()
                .email(role.name().toLowerCase() + "-commission-" + System.nanoTime() + "@bhstays.ro")
                .passwordHash("irrelevant-for-this-test")
                .firstName("Test")
                .lastName(role.name())
                .role(role)
                .status(UserStatus.ACTIVE)
                .mfaEnabled(true)
                .build());
    }

    private RequestPostProcessor as(User user) {
        return user(new UserPrincipal(user));
    }

    private static PropertyCommissionCurrencyResponse line(PropertyCommissionReportResponse report, String currency) {
        return report.currencies().stream().filter(l -> l.currency().equals(currency)).findFirst().orElseThrow();
    }

    private static CommissionSummaryCurrencyTotals totals(CommissionSummaryResponse summary, String currency) {
        return summary.totals().stream().filter(t -> t.currency().equals(currency)).findFirst().orElseThrow();
    }

    // ------------------------------------------------------------------
    // migration
    // ------------------------------------------------------------------

    @Test
    void v40MigrationIsAppliedAndItsConstraintsHold() {
        Integer applied = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where version = '40' and success", Integer.class);
        assertThat(applied).isEqualTo(1);

        Property property = property("Constraint check", null);
        assertThatThrownBy(() -> jdbcTemplate.update(
                "update properties set commission_percent = 100.01 where id = ?", property.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                "update properties set commission_percent = -1 where id = ?", property.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);

        Reservation reservation = reservation(property, LocalDate.of(2044, 1, 10), "RON", "500.00", null, null, null);
        String breakdown = "update reservations set accommodation_amount = ?, cleaning_fee_amount = ?, "
                + "extra_guest_fee_amount = ?, late_checkout_fee_amount = ?, tax_amount = ?, addon_amount = ? "
                + "where id = ?";
        // parts that do not add up to the total, by a single cent
        assertThatThrownBy(() -> jdbcTemplate.update(breakdown,
                new BigDecimal("300.00"), new BigDecimal("50.00"), new BigDecimal("40.00"), new BigDecimal("30.00"),
                new BigDecimal("40.00"), new BigDecimal("39.99"), reservation.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        // a partial breakdown (late checkout, tax and add-ons missing)
        assertThatThrownBy(() -> jdbcTemplate.update(
                "update reservations set accommodation_amount = 400, cleaning_fee_amount = 100, "
                        + "extra_guest_fee_amount = 0 where id = ?", reservation.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        // a negative part
        assertThatThrownBy(() -> jdbcTemplate.update(breakdown,
                new BigDecimal("520.00"), new BigDecimal("0"), new BigDecimal("0"), new BigDecimal("0"),
                new BigDecimal("-20.00"), new BigDecimal("0"), reservation.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        // a breakdown that reconciles exactly is accepted
        assertThat(jdbcTemplate.update(breakdown,
                new BigDecimal("300.00"), new BigDecimal("50.00"), new BigDecimal("40.00"), new BigDecimal("30.00"),
                new BigDecimal("40.00"), new BigDecimal("40.00"), reservation.getId())).isEqualTo(1);

        // owner statements: rows issued before V40 are LEGACY_GROSS; a new-formula row must reconcile
        User owner = staff(Role.OWNER);
        UUID legacyId = UUID.randomUUID();
        jdbcTemplate.update("insert into owner_statements (id, owner_id, period_start, period_end, currency, "
                        + "gross_revenue, commission_amount, expenses_total, net_payout) "
                        + "values (?, ?, '2044-01-01', '2044-01-31', 'RON', 500, 100, 0, 400)",
                legacyId, owner.getId());
        assertThat(jdbcTemplate.queryForObject("select calculation_method from owner_statements where id = ?",
                String.class, legacyId)).isEqualTo("LEGACY_GROSS");
        assertThatThrownBy(() -> jdbcTemplate.update("insert into owner_statements (id, owner_id, period_start, "
                        + "period_end, currency, gross_revenue, commission_amount, expenses_total, net_payout, "
                        + "calculation_method, captured_total, refunded_total, commissionable_base, owner_amount, "
                        + "unallocated_net_revenue, unallocated_reservation_count) values (?, ?, '2044-02-01', "
                        + "'2044-02-28', 'RON', 500, 80, 0, 400, 'CAPTURED_ACCOMMODATION', 500, 0, 400, 420, 0, 0)",
                UUID.randomUUID(), owner.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ------------------------------------------------------------------
    // calculations on real data
    // ------------------------------------------------------------------

    @Test
    void propertyReport_countsOnlyCapturedMoneyAndCommissionsOnlyAccommodation() {
        Property property = property("Apartament 20%", "20.00");
        LocalDate base = LocalDate.of(2042, 3, 1);

        // 500 = 400 accommodation + 100 cleaning, fully paid; the other attempts never captured anything
        Reservation paid = reservation(property, base, "RON", "500.00", "400.00", "100.00", "0.00");
        payment(paid, PaymentStatus.SUCCEEDED, "500.00", "0");
        payment(paid, PaymentStatus.PENDING, "500.00", "0");
        payment(paid, PaymentStatus.PROCESSING, "500.00", "0");
        payment(paid, PaymentStatus.FAILED, "500.00", "0");
        payment(paid, PaymentStatus.CANCELLED, "500.00", "0");

        // 1000 = 800 accommodation + 150 cleaning + 50 extra guest; 250 refunded
        Reservation partlyRefunded = reservation(property, base.plusDays(3), "RON", "1000.00", "800.00", "150.00", "50.00");
        payment(partlyRefunded, PaymentStatus.PARTIALLY_REFUNDED, "1000.00", "250.00");

        // a booking hold that was never paid
        Reservation hold = reservation(property, base.plusDays(6), "RON", "300.00", "300.00", "0.00", "0.00");
        hold.setStatus(ReservationStatus.PENDING);
        hold.setHoldExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
        reservationRepository.saveAndFlush(hold);
        payment(hold, PaymentStatus.PENDING, "300.00", "0");

        // EUR: one fully refunded, one paid
        Reservation refunded = reservation(property, base.plusDays(9), "EUR", "200.00", "160.00", "40.00", "0.00");
        payment(refunded, PaymentStatus.REFUNDED, "200.00", "200.00");
        Reservation eurPaid = reservation(property, base.plusDays(12), "EUR", "300.00", "300.00", "0.00", "0.00");
        payment(eurPaid, PaymentStatus.SUCCEEDED, "300.00", "0");

        // outside the period
        Reservation later = reservation(property, base.plusMonths(2), "RON", "900.00", "900.00", "0.00", "0.00");
        payment(later, PaymentStatus.SUCCEEDED, "900.00", "0");

        PropertyCommissionReportResponse report =
                reportService.propertyReport(property.getId(), base, base.plusMonths(1).minusDays(1));

        assertThat(report.commissionConfigured()).isTrue();
        assertThat(report.currencies()).extracting(PropertyCommissionCurrencyResponse::currency)
                .containsExactly("EUR", "RON");

        PropertyCommissionCurrencyResponse ron = line(report, "RON");
        assertThat(ron.capturedTotal()).isEqualByComparingTo("1500.00");
        assertThat(ron.refundedTotal()).isEqualByComparingTo("250.00");
        assertThat(ron.netRevenue()).isEqualByComparingTo("1250.00");
        // 400 + 800 * 750 / 1000
        assertThat(ron.commissionableBase()).isEqualByComparingTo("1000.00");
        assertThat(ron.bhStaysRevenue()).isEqualByComparingTo("200.00");
        assertThat(ron.ownerAmount()).isEqualByComparingTo("1050.00");
        assertThat(ron.paidReservationCount()).isEqualTo(2);

        PropertyCommissionCurrencyResponse eur = line(report, "EUR");
        assertThat(eur.capturedTotal()).isEqualByComparingTo("500.00");
        assertThat(eur.refundedTotal()).isEqualByComparingTo("200.00");
        assertThat(eur.netRevenue()).isEqualByComparingTo("300.00");
        assertThat(eur.commissionableBase()).isEqualByComparingTo("300.00");
        assertThat(eur.bhStaysRevenue()).isEqualByComparingTo("60.00");
        assertThat(eur.ownerAmount()).isEqualByComparingTo("240.00");
    }

    @Test
    void summary_aggregatesPropertiesWithDifferentPercentsPerCurrency() {
        LocalDate base = LocalDate.of(2043, 6, 1);
        Property twenty = property("Comision 20%", "20.00");
        Property zero = property("Comision 0%", "0.00");
        Property hundred = property("Comision 100%", "100.00");
        Property unconfigured = property("Comision neconfigurat", null);

        payment(reservation(twenty, base, "RON", "500.00", "400.00", "100.00", "0.00"),
                PaymentStatus.SUCCEEDED, "500.00", "0");                               // BH 80, owner 420
        payment(reservation(twenty, base.plusDays(3), "EUR", "100.00", "100.00", "0.00", "0.00"),
                PaymentStatus.SUCCEEDED, "100.00", "0");                               // BH 20, owner 80
        payment(reservation(zero, base, "RON", "400.00", "300.00", "100.00", "0.00"),
                PaymentStatus.SUCCEEDED, "400.00", "0");                               // BH 0, owner 400
        payment(reservation(hundred, base, "RON", "500.00", "350.00", "150.00", "0.00"),
                PaymentStatus.SUCCEEDED, "500.00", "0");                               // BH 350, owner 150
        payment(reservation(unconfigured, base, "RON", "600.00", "500.00", "100.00", "0.00"),
                PaymentStatus.SUCCEEDED, "600.00", "0");                               // not split

        CommissionSummaryResponse summary = reportService.summary(base, base.plusDays(29));

        assertThat(summary.totals()).extracting(CommissionSummaryCurrencyTotals::currency)
                .containsExactly("EUR", "RON");
        CommissionSummaryCurrencyTotals ron = totals(summary, "RON");
        assertThat(ron.propertiesNetRevenue()).isEqualByComparingTo("2000.00");
        assertThat(ron.bhStaysRevenue()).isEqualByComparingTo("430.00");
        assertThat(ron.ownersAmount()).isEqualByComparingTo("970.00");
        assertThat(ron.includedPropertyCount()).isEqualTo(4);
        assertThat(ron.unconfiguredPropertyCount()).isEqualTo(1);
        assertThat(ron.unconfiguredNetRevenue()).isEqualByComparingTo("600.00");

        CommissionSummaryCurrencyTotals eur = totals(summary, "EUR");
        assertThat(eur.propertiesNetRevenue()).isEqualByComparingTo("100.00");
        assertThat(eur.bhStaysRevenue()).isEqualByComparingTo("20.00");
        assertThat(eur.ownersAmount()).isEqualByComparingTo("80.00");
        assertThat(eur.includedPropertyCount()).isEqualTo(1);

        assertThat(summary.unconfiguredProperties()).extracting(UnconfiguredPropertyResponse::propertyId)
                .contains(unconfigured.getId())
                .doesNotContain(twenty.getId(), zero.getId(), hundred.getId());
    }

    @Test
    void duplicateStripeWebhookDeliveriesAreCountedOnce() {
        Property property = property("Webhook duplicat", "20.00");
        LocalDate checkIn = LocalDate.of(2045, 2, 10);
        Reservation reservation = reservationRepository.saveAndFlush(Reservation.builder()
                .property(property)
                .guestFirstName("Ion")
                .guestLastName("Ionescu")
                .guestEmail("ion@example.com")
                .guestPhone("0700000000")
                .checkInDate(checkIn)
                .checkOutDate(checkIn.plusDays(4))
                .numberOfGuests(2)
                .status(ReservationStatus.PENDING)
                .source(ReservationSource.DIRECT)
                .totalAmount(new BigDecimal("500.00"))
                .accommodationAmount(new BigDecimal("400.00"))
                .cleaningFeeAmount(new BigDecimal("100.00"))
                .extraGuestFeeAmount(BigDecimal.ZERO)
                .lateCheckoutFeeAmount(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .addonAmount(BigDecimal.ZERO)
                .currency("RON")
                .managementToken(secureTokenGenerator.generateRawToken())
                .holdExpiresAt(Instant.now().plus(35, ChronoUnit.MINUTES))
                .build());
        Payment payment = paymentRepository.saveAndFlush(Payment.builder()
                .reservation(reservation)
                .provider(PaymentProvider.STRIPE)
                .method(PaymentMethod.ONLINE_CARD)
                .status(PaymentStatus.PENDING)
                .amount(new BigDecimal("500.00"))
                .currency("RON")
                .checkoutSessionId("cs_test_" + UUID.randomUUID())
                .checkoutUrl("https://checkout.stripe.com/c/pay/test")
                .checkoutExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES))
                .build());

        String eventId = "evt_" + UUID.randomUUID().toString().replace("-", "");
        String payload = paidEvent(eventId, payment);
        stripeWebhookService.handle(payload, sign(payload));
        stripeWebhookService.handle(payload, sign(payload));
        // a second, distinct event for the same capture (e.g. async_payment_succeeded after completed)
        String secondPayload = paidEvent("evt_" + UUID.randomUUID().toString().replace("-", ""), payment);
        stripeWebhookService.handle(secondPayload, sign(secondPayload));

        assertThat(paymentRepository.findById(payment.getId()).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.SUCCEEDED);
        PropertyCommissionCurrencyResponse ron =
                line(reportService.propertyReport(property.getId(), checkIn, checkIn), "RON");
        assertThat(ron.capturedTotal()).isEqualByComparingTo("500.00");
        assertThat(ron.netRevenue()).isEqualByComparingTo("500.00");
        assertThat(ron.bhStaysRevenue()).isEqualByComparingTo("80.00");
        assertThat(ron.ownerAmount()).isEqualByComparingTo("420.00");
    }

    // ------------------------------------------------------------------
    // endpoints and authorization
    // ------------------------------------------------------------------

    @Test
    void reportsAreReadableOnlyByAdministratorsAndAccountants() throws Exception {
        Property property = property("Autorizare rapoarte", "15.00");
        String summaryUrl = "/api/v1/reports/financial/commission-summary?from=2046-01-01&to=2046-01-31";
        String propertyUrl = "/api/v1/reports/financial/properties/" + property.getId()
                + "/commission?from=2046-01-01&to=2046-01-31";

        for (Role allowed : List.of(Role.SUPER_ADMIN, Role.ADMINISTRATOR, Role.ACCOUNTANT)) {
            User user = staff(allowed);
            mockMvc.perform(get(summaryUrl).with(as(user))).andExpect(status().isOk());
            mockMvc.perform(get(propertyUrl).with(as(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.commissionConfigured").value(true))
                    .andExpect(jsonPath("$.data.commissionPercent").value(15.00));
        }

        // an owner must not see BH Stays's internal revenue, not even for their own property
        User owner = staff(Role.OWNER);
        property.setOwner(owner);
        propertyRepository.saveAndFlush(property);
        for (Role denied : List.of(Role.OWNER, Role.CLEANER, Role.MAINTENANCE, Role.SUPPORT_AGENT)) {
            User user = denied == Role.OWNER ? owner : staff(denied);
            mockMvc.perform(get(summaryUrl).with(as(user))).andExpect(status().isForbidden());
            mockMvc.perform(get(propertyUrl).with(as(user))).andExpect(status().isForbidden());
        }

        mockMvc.perform(get(summaryUrl))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(401, 403));
    }

    @Test
    void reportEndpoints_validateThePeriodAndTheProperty() throws Exception {
        User admin = staff(Role.ADMINISTRATOR);
        mockMvc.perform(get("/api/v1/reports/financial/commission-summary?from=2046-02-01&to=2046-01-01")
                        .with(as(admin)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/reports/financial/properties/" + UUID.randomUUID() + "/commission")
                        .with(as(admin)))
                .andExpect(status().isNotFound());
    }

    @Test
    void commissionCanBeChangedOnlyByAdministratorsWithinZeroToHundred() throws Exception {
        Property property = property("Setare comision", null);
        String url = "/api/v1/properties/" + property.getId() + "/management-commission";

        for (Role denied : List.of(Role.ACCOUNTANT, Role.OWNER, Role.SUPPORT_AGENT, Role.CLEANER)) {
            mockMvc.perform(patch(url).with(as(staff(denied)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"commissionPercent\": 20}"))
                    .andExpect(status().isForbidden());
        }
        assertThat(propertyRepository.findById(property.getId()).orElseThrow().getCommissionPercent()).isNull();

        User admin = staff(Role.ADMINISTRATOR);
        for (String invalid : List.of("-0.01", "100.01", "150", "12.345")) {
            mockMvc.perform(patch(url).with(as(admin))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"commissionPercent\": " + invalid + "}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }

        mockMvc.perform(patch(url).with(as(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"commissionPercent\": 17.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.commissionPercent").value(17.50));
        assertThat(propertyRepository.findById(property.getId()).orElseThrow().getCommissionPercent())
                .isEqualByComparingTo("17.50");

        User superAdmin = staff(Role.SUPER_ADMIN);
        mockMvc.perform(patch(url).with(as(superAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"commissionPercent\": null}"))
                .andExpect(status().isOk());
        assertThat(propertyRepository.findById(property.getId()).orElseThrow().getCommissionPercent()).isNull();

        List<AuditLog> audit = auditLogRepository.findAll().stream()
                .filter(log -> AuditAction.PROPERTY_COMMISSION_CHANGED.name().equals(log.getAction()))
                .filter(log -> property.getId().toString().equals(log.getEntityId()))
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .toList();
        assertThat(audit).extracting(AuditLog::getDescription).containsExactly(
                "Management commission: not configured -> 17.50%",
                "Management commission: 17.50% -> not configured");
        assertThat(audit).extracting(AuditLog::getEntityName).containsOnly("Property");
        assertThat(audit).extracting(AuditLog::getActorEmail).containsExactly(admin.getEmail(), superAdmin.getEmail());
    }

    // ------------------------------------------------------------------
    // Stripe webhook helpers (signed exactly like Stripe signs deliveries)
    // ------------------------------------------------------------------

    private static String paidEvent(String eventId, Payment payment) {
        return """
                {
                  "id": "%s",
                  "object": "event",
                  "type": "checkout.session.completed",
                  "data": { "object": {
                    "id": "%s",
                    "object": "checkout.session",
                    "payment_intent": "pi_%s",
                    "payment_status": "paid",
                    "amount_total": 50000,
                    "currency": "ron",
                    "metadata": { "reservationId": "%s" }
                  }}
                }
                """.formatted(eventId, payment.getCheckoutSessionId(), payment.getId().toString().replace("-", ""),
                payment.getReservation().getId());
    }

    private static String sign(String payload) {
        try {
            long timestamp = Instant.now().getEpochSecond();
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8));
            return "t=" + timestamp + ",v1=" + HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
