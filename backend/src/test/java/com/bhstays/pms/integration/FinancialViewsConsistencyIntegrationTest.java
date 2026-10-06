package com.bhstays.pms.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bhstays.pms.common.exception.BadRequestException;
import com.bhstays.pms.domain.Address;
import com.bhstays.pms.domain.DynamicPricingConfig;
import com.bhstays.pms.domain.OwnerStatement;
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
import com.bhstays.pms.domain.SeasonalRate;
import com.bhstays.pms.domain.User;
import com.bhstays.pms.domain.UserStatus;
import com.bhstays.pms.dto.owner.OwnerRevenueLine;
import com.bhstays.pms.dto.ownerstatement.OwnerStatementLineResponse;
import com.bhstays.pms.dto.ownerstatement.OwnerStatementResponse;
import com.bhstays.pms.dto.payment.ManualPaymentCreateRequest;
import com.bhstays.pms.dto.payment.PaymentResponse;
import com.bhstays.pms.dto.payment.RefundCreateRequest;
import com.bhstays.pms.dto.report.CommissionSummaryCurrencyTotals;
import com.bhstays.pms.dto.report.FinancialReportCurrencyTotals;
import com.bhstays.pms.dto.report.FinancialReportRowResponse;
import com.bhstays.pms.dto.report.PropertyCommissionCurrencyResponse;
import com.bhstays.pms.repository.DynamicPricingConfigRepository;
import com.bhstays.pms.repository.PaymentRepository;
import com.bhstays.pms.repository.PropertyRepository;
import com.bhstays.pms.repository.ReservationRepository;
import com.bhstays.pms.repository.SeasonalRateRepository;
import com.bhstays.pms.repository.UserRepository;
import com.bhstays.pms.security.UserPrincipal;
import com.bhstays.pms.service.EmailService;
import com.bhstays.pms.service.FinancialReportService;
import com.bhstays.pms.service.OwnerService;
import com.bhstays.pms.service.OwnerStatementService;
import com.bhstays.pms.service.PaymentService;
import com.bhstays.pms.service.PropertyCommissionReportService;
import com.bhstays.pms.service.PropertyService;
import com.bhstays.pms.service.ReservationService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The same reservations must produce the same figures in every financial
 * view - property report, dashboard, /finance, owner statements and the
 * owner dashboard - against a real Postgres. Each test uses its own owner,
 * properties and check-in year, because the database is shared with the
 * other integration test classes.
 */
@AutoConfigureMockMvc
class FinancialViewsConsistencyIntegrationTest extends AbstractIntegrationTest {

    @Autowired private PropertyCommissionReportService commissionReportService;
    @Autowired private FinancialReportService financialReportService;
    @Autowired private OwnerStatementService ownerStatementService;
    @Autowired private OwnerService ownerService;
    @Autowired private PropertyService propertyService;
    @Autowired private ReservationService reservationService;
    @Autowired private PaymentService paymentService;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private SeasonalRateRepository seasonalRateRepository;
    @Autowired private DynamicPricingConfigRepository dynamicPricingConfigRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private MockMvc mockMvc;

    @MockitoBean private EmailService emailService;

    // ------------------------------------------------------------------
    // fixtures
    // ------------------------------------------------------------------

    private User account(Role role) {
        return userRepository.save(User.builder()
                .email(role.name().toLowerCase() + "-views-" + System.nanoTime() + "@bhstays.ro")
                .passwordHash("irrelevant-for-this-test")
                .firstName("Test")
                .lastName(role.name())
                .role(role)
                .status(UserStatus.ACTIVE)
                .mfaEnabled(true)
                .build());
    }

    private Property property(User owner, String commissionPercent) {
        return propertyRepository.saveAndFlush(Property.builder()
                .name("Apartament " + UUID.randomUUID())
                .propertyType(PropertyType.APARTMENT)
                .status(PropertyStatus.ACTIVE)
                .address(new Address("Str. Consecvenței 1", "Brașov", null, null, "România", null, null))
                .bedrooms(2)
                .bathrooms(1)
                .maxGuests(4)
                .basePricePerNight(new BigDecimal("100.00"))
                .weekendPricePerNight(new BigDecimal("150.00"))
                .cleaningFee(new BigDecimal("80.00"))
                .extraGuestFee(new BigDecimal("20.00"))
                .baseGuestsIncluded(2)
                .weeklyDiscountPercent(new BigDecimal("10.00"))
                .owner(owner)
                .commissionPercent(commissionPercent != null ? new BigDecimal(commissionPercent) : null)
                .build());
    }

    private Reservation direct(Property property, LocalDate checkIn, String currency, String total,
                               String accommodation, String cleaning, String lateCheckout, String tax, String addon) {
        boolean breakdown = accommodation != null;
        return reservationRepository.saveAndFlush(Reservation.builder()
                .property(property)
                .guestFirstName("Ana")
                .guestLastName("Pop")
                .guestEmail("ana@example.com")
                .checkInDate(checkIn)
                .checkOutDate(checkIn.plusDays(2))
                .numberOfGuests(2)
                .status(ReservationStatus.CONFIRMED)
                .source(ReservationSource.DIRECT)
                .totalAmount(new BigDecimal(total))
                .currency(currency)
                .accommodationAmount(breakdown ? new BigDecimal(accommodation) : null)
                .cleaningFeeAmount(breakdown ? new BigDecimal(cleaning) : null)
                .extraGuestFeeAmount(breakdown ? BigDecimal.ZERO : null)
                .lateCheckoutFeeAmount(breakdown ? new BigDecimal(lateCheckout) : null)
                .taxAmount(breakdown ? new BigDecimal(tax) : null)
                .addonAmount(breakdown ? new BigDecimal(addon) : null)
                .build());
    }

    private Payment payment(Reservation reservation, PaymentStatus status, String amount) {
        return paymentRepository.saveAndFlush(Payment.builder()
                .reservation(reservation)
                .provider(PaymentProvider.MANUAL)
                .method(PaymentMethod.BANK_TRANSFER)
                .status(status)
                .amount(new BigDecimal(amount))
                .currency(reservation.getCurrency())
                .build());
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------
    // the same money, everywhere
    // ------------------------------------------------------------------

    @Test
    void sameReservationsProduceTheSameFiguresInEveryView() {
        User admin = account(Role.ADMINISTRATOR);
        User owner = account(Role.OWNER);
        Property property = property(owner, "20.00");
        LocalDate from = LocalDate.of(2048, 1, 1);
        LocalDate to = LocalDate.of(2048, 12, 31);

        // --- a real booking through the pricing engine: weekend + seasonal + dynamic pricing + weekly discount
        LocalDate checkIn = LocalDate.of(2048, 3, 2);
        LocalDate seasonalNight = checkIn.plusDays(3);
        seasonalRateRepository.saveAndFlush(SeasonalRate.builder()
                .property(property).label("Festival").startDate(seasonalNight).endDate(seasonalNight)
                .pricePerNight(new BigDecimal("200.00")).build());
        dynamicPricingConfigRepository.saveAndFlush(DynamicPricingConfig.builder()
                .property(property).enabled(true).build());

        Reservation booked = reservationService.createGuestBooking(property.getId(), "Ion", "Popescu",
                "ion@example.com", "0700000000", checkIn, checkIn.plusDays(7), 3, null, null);

        // expected price, night by night: nobody else is booked, so occupancy is 0 -> factor 0.90
        BigDecimal subtotal = BigDecimal.ZERO;
        for (LocalDate night = checkIn; night.isBefore(checkIn.plusDays(7)); night = night.plusDays(1)) {
            BigDecimal rate = night.equals(seasonalNight) ? new BigDecimal("200.00")
                    : (night.getDayOfWeek() == DayOfWeek.FRIDAY || night.getDayOfWeek() == DayOfWeek.SATURDAY)
                            ? new BigDecimal("150.00") : new BigDecimal("100.00");
            subtotal = subtotal.add(rate.multiply(new BigDecimal("0.9000")).setScale(2, RoundingMode.HALF_UP));
        }
        BigDecimal discount = subtotal.multiply(new BigDecimal("10.00")).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal accommodation = subtotal.subtract(discount);
        BigDecimal extraGuest = money("140.00");   // 20 x 1 extra guest x 7 nights
        BigDecimal cleaning = money("80.00");
        BigDecimal total = accommodation.add(extraGuest).add(cleaning);

        Reservation snapshot = reservationRepository.findById(booked.getId()).orElseThrow();
        assertThat(snapshot.getTotalAmount()).isEqualByComparingTo(total);
        assertThat(snapshot.getAccommodationAmount()).isEqualByComparingTo(accommodation);
        assertThat(snapshot.getCleaningFeeAmount()).isEqualByComparingTo(cleaning);
        assertThat(snapshot.getExtraGuestFeeAmount()).isEqualByComparingTo(extraGuest);
        assertThat(snapshot.getLateCheckoutFeeAmount()).isEqualByComparingTo("0");
        assertThat(snapshot.getTaxAmount()).isEqualByComparingTo("0");
        assertThat(snapshot.getAddonAmount()).isEqualByComparingTo("0");
        assertThat(snapshot.getAccommodationAmount().add(snapshot.getCleaningFeeAmount())
                .add(snapshot.getExtraGuestFeeAmount()).add(snapshot.getLateCheckoutFeeAmount())
                .add(snapshot.getTaxAmount()).add(snapshot.getAddonAmount()))
                .isEqualByComparingTo(snapshot.getTotalAmount());

        // paid in full, then 100 refunded; a pending and a failed attempt never count
        PaymentResponse paid = paymentService.recordManualPayment(
                new ManualPaymentCreateRequest(booked.getId(), total, PaymentMethod.BANK_TRANSFER, null));
        paymentService.refund(paid.id(), new RefundCreateRequest(new BigDecimal("100.00"), "Rambursare parțială"));
        payment(snapshot, PaymentStatus.PENDING, "999.00");
        payment(snapshot, PaymentStatus.FAILED, "999.00");

        // --- a stay paid in full plus a separate 50 late-checkout payment: the extra 50 is never commissioned
        Reservation lateCheckout = direct(property, LocalDate.of(2048, 4, 10), "RON", "500.00", "400.00", "100.00",
                "0", "0", "0");
        payment(lateCheckout, PaymentStatus.SUCCEEDED, "500.00");
        payment(lateCheckout, PaymentStatus.SUCCEEDED, "50.00");

        // --- a historical reservation without breakdown: counted in the net revenue, never commissioned
        Reservation historical = direct(property, LocalDate.of(2048, 5, 10), "RON", "250.00", null, null, null, null, null);
        payment(historical, PaymentStatus.SUCCEEDED, "250.00");

        // --- an unpaid booking hold
        Reservation hold = direct(property, LocalDate.of(2048, 6, 10), "RON", "300.00", "300.00", "0", "0", "0", "0");
        hold.setStatus(ReservationStatus.PENDING);
        hold.setHoldExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
        reservationRepository.saveAndFlush(hold);
        payment(hold, PaymentStatus.PENDING, "300.00");

        // --- EUR: 300 = 200 accommodation + 30 cleaning + 20 late checkout + 30 taxes + 20 add-ons
        Reservation eur = direct(property, LocalDate.of(2048, 7, 10), "EUR", "300.00", "200.00", "30.00",
                "20.00", "30.00", "20.00");
        payment(eur, PaymentStatus.SUCCEEDED, "300.00");

        // ---------------- expected (independently derived) ----------------
        BigDecimal bookingNet = total.subtract(money("100.00"));
        BigDecimal bookingBase = accommodation.multiply(bookingNet).divide(total, 2, RoundingMode.HALF_UP);
        BigDecimal ronCaptured = total.add(money("500.00")).add(money("50.00")).add(money("250.00"));
        BigDecimal ronNet = ronCaptured.subtract(money("100.00"));
        BigDecimal ronBase = bookingBase.add(money("400.00"));
        BigDecimal ronBhStays = ronBase.multiply(new BigDecimal("20.00")).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal ronOwner = ronNet.subtract(ronBhStays);

        // 1. property report
        var report = commissionReportService.propertyReport(property.getId(), from, to);
        PropertyCommissionCurrencyResponse ron = report.currencies().stream()
                .filter(l -> l.currency().equals("RON")).findFirst().orElseThrow();
        PropertyCommissionCurrencyResponse eurLine = report.currencies().stream()
                .filter(l -> l.currency().equals("EUR")).findFirst().orElseThrow();
        assertThat(report.currencies()).hasSize(2);
        assertThat(ron.capturedTotal()).isEqualByComparingTo(ronCaptured);
        assertThat(ron.refundedTotal()).isEqualByComparingTo("100.00");
        assertThat(ron.netRevenue()).isEqualByComparingTo(ronNet);
        assertThat(ron.commissionableBase()).isEqualByComparingTo(ronBase);
        assertThat(ron.bhStaysRevenue()).isEqualByComparingTo(ronBhStays);
        assertThat(ron.ownerAmount()).isEqualByComparingTo(ronOwner);
        assertThat(ron.unallocatedNetRevenue()).isEqualByComparingTo("250.00");
        assertThat(ron.unallocatedReservationCount()).isEqualTo(1);
        assertThat(eurLine.netRevenue()).isEqualByComparingTo("300.00");
        assertThat(eurLine.commissionableBase()).isEqualByComparingTo("200.00");
        assertThat(eurLine.bhStaysRevenue()).isEqualByComparingTo("40.00");
        assertThat(eurLine.ownerAmount()).isEqualByComparingTo("260.00");

        // 2. dashboard (this test is alone in its check-in year)
        List<CommissionSummaryCurrencyTotals> dashboard = commissionReportService.summary(from, to).totals();
        assertThat(dashboard).extracting(CommissionSummaryCurrencyTotals::currency).containsExactly("EUR", "RON");
        assertSame(dashboard.get(1), ron);
        assertSame(dashboard.get(0), eurLine);

        // 3. /finance - per property and for the whole portfolio
        List<FinancialReportRowResponse> financeRows = financialReportService.summary(property.getId(), from, to).rows();
        assertThat(financeRows).hasSize(2);
        for (FinancialReportRowResponse row : financeRows) {
            PropertyCommissionCurrencyResponse expected = row.currency().equals("RON") ? ron : eurLine;
            assertThat(row.capturedTotal()).isEqualByComparingTo(expected.capturedTotal());
            assertThat(row.refundedTotal()).isEqualByComparingTo(expected.refundedTotal());
            assertThat(row.netRevenue()).isEqualByComparingTo(expected.netRevenue());
            assertThat(row.commissionableBase()).isEqualByComparingTo(expected.commissionableBase());
            assertThat(row.bhStaysRevenue()).isEqualByComparingTo(expected.bhStaysRevenue());
            assertThat(row.ownerAmount()).isEqualByComparingTo(expected.ownerAmount());
            assertThat(row.unallocatedReservationCount()).isEqualTo(expected.unallocatedReservationCount());
        }
        List<FinancialReportCurrencyTotals> financeTotals = financialReportService.summary(null, from, to).totals();
        assertThat(financeTotals).extracting(t -> t.revenue()).containsExactlyElementsOf(dashboard);

        // 4. owner statements - one per currency, same figures
        List<OwnerStatementResponse> statements = ownerStatementService.generate(owner.getId(), from, to, admin);
        assertThat(statements).extracting(OwnerStatementResponse::currency).containsExactly("EUR", "RON");
        for (OwnerStatementResponse statement : statements) {
            PropertyCommissionCurrencyResponse expected = statement.currency().equals("RON") ? ron : eurLine;
            assertThat(statement.calculationMethod()).isEqualTo(OwnerStatement.CALCULATION_CAPTURED_ACCOMMODATION);
            assertThat(statement.capturedTotal()).isEqualByComparingTo(expected.capturedTotal());
            assertThat(statement.refundedTotal()).isEqualByComparingTo(expected.refundedTotal());
            assertThat(statement.grossRevenue()).isEqualByComparingTo(expected.netRevenue());
            assertThat(statement.commissionableBase()).isEqualByComparingTo(expected.commissionableBase());
            assertThat(statement.commissionAmount()).isEqualByComparingTo(expected.bhStaysRevenue());
            assertThat(statement.ownerAmount()).isEqualByComparingTo(expected.ownerAmount());
            assertThat(statement.netPayout()).isEqualByComparingTo(expected.ownerAmount());
            assertThat(statement.unallocatedReservationCount()).isEqualTo(expected.unallocatedReservationCount());
            OwnerStatementLineResponse line = statement.lines().get(0);
            assertThat(line.commissionPercent()).isEqualByComparingTo("20.00");
            assertThat(line.ownerAmount()).isEqualByComparingTo(expected.ownerAmount());
        }

        // 5. owner dashboard (all-time; this owner has only this property)
        List<OwnerRevenueLine> ownerView = ownerService.getMyDashboardSummary(owner.getId()).revenueByCurrency();
        assertThat(ownerView).extracting(OwnerRevenueLine::currency).containsExactly("EUR", "RON");
        OwnerRevenueLine ownerRon = ownerView.get(1);
        assertThat(ownerRon.netRevenue()).isEqualByComparingTo(ron.netRevenue());
        assertThat(ownerRon.bhStaysCommission()).isEqualByComparingTo(ron.bhStaysRevenue());
        assertThat(ownerRon.ownerAmount()).isEqualByComparingTo(ron.ownerAmount());
        assertThat(ownerService.getMyProperty(owner.getId(), property.getId()).revenueByCurrency().get(1).ownerAmount())
                .isEqualByComparingTo(ron.ownerAmount());
    }

    private static void assertSame(CommissionSummaryCurrencyTotals totals, PropertyCommissionCurrencyResponse line) {
        assertThat(totals.capturedTotal()).isEqualByComparingTo(line.capturedTotal());
        assertThat(totals.refundedTotal()).isEqualByComparingTo(line.refundedTotal());
        assertThat(totals.propertiesNetRevenue()).isEqualByComparingTo(line.netRevenue());
        assertThat(totals.bhStaysRevenue()).isEqualByComparingTo(line.bhStaysRevenue());
        assertThat(totals.ownersAmount()).isEqualByComparingTo(line.ownerAmount());
        assertThat(totals.unallocatedReservationCount()).isEqualTo(line.unallocatedReservationCount());
        assertThat(totals.includedPropertyCount()).isEqualTo(1);
    }

    @Test
    void statementIsRefusedUntilTheCommissionIsConfigured() {
        User admin = account(Role.ADMINISTRATOR);
        User owner = account(Role.OWNER);
        Property property = property(owner, null);
        LocalDate from = LocalDate.of(2049, 1, 1);
        LocalDate to = LocalDate.of(2049, 12, 31);
        payment(direct(property, LocalDate.of(2049, 2, 10), "RON", "500.00", "400.00", "100.00", "0", "0", "0"),
                PaymentStatus.SUCCEEDED, "500.00");

        assertThatThrownBy(() -> ownerStatementService.generate(owner.getId(), from, to, admin))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(property.getName());
        OwnerRevenueLine pending = ownerService.getMyDashboardSummary(owner.getId()).revenueByCurrency().get(0);
        assertThat(pending.unconfiguredNetRevenue()).isEqualByComparingTo("500.00");
        assertThat(pending.ownerAmount()).isNull();

        propertyService.updateCommission(property.getId(), new BigDecimal("15"));

        OwnerStatementResponse statement = ownerStatementService.generate(owner.getId(), from, to, admin).get(0);
        assertThat(statement.commissionAmount()).isEqualByComparingTo("60.00");
        assertThat(statement.ownerAmount()).isEqualByComparingTo("440.00");
    }

    @Test
    void financeAndStatementsAreNotVisibleToOwnersOrOperationalRoles() throws Exception {
        for (Role denied : List.of(Role.OWNER, Role.CLEANER, Role.MAINTENANCE, Role.SUPPORT_AGENT)) {
            UserPrincipal principal = new UserPrincipal(account(denied));
            mockMvc.perform(get("/api/v1/reports/financial/summary").with(user(principal)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/api/v1/owner-statements").with(user(principal)))
                    .andExpect(status().isForbidden());
        }
        for (Role allowed : List.of(Role.SUPER_ADMIN, Role.ADMINISTRATOR, Role.ACCOUNTANT)) {
            UserPrincipal principal = new UserPrincipal(account(allowed));
            mockMvc.perform(get("/api/v1/reports/financial/summary?from=2050-01-01&to=2050-01-31")
                            .with(user(principal)))
                    .andExpect(status().isOk());
        }
    }
}
