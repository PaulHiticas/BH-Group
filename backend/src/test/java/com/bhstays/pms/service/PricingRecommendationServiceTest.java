package com.bhstays.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bhstays.pms.common.exception.ApiException;
import com.bhstays.pms.common.exception.ResourceNotFoundException;
import com.bhstays.pms.config.AppProperties;
import com.bhstays.pms.domain.Address;
import com.bhstays.pms.domain.AuditAction;
import com.bhstays.pms.domain.Property;
import com.bhstays.pms.domain.Reservation;
import com.bhstays.pms.domain.ReservationSource;
import com.bhstays.pms.domain.ReservationStatus;
import com.bhstays.pms.domain.SeasonalRate;
import com.bhstays.pms.dto.pricing.AiPricingRecommendationResponse;
import com.bhstays.pms.dto.pricing.DynamicPricingConfigResponse;
import com.bhstays.pms.repository.LocalEventRepository;
import com.bhstays.pms.repository.PropertyRepository;
import com.bhstays.pms.repository.ReservationRepository;
import com.bhstays.pms.repository.SeasonalRateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.RestClient;

/**
 * The AI call itself is stubbed at the RestClient boundary - no Anthropic
 * request is made. What is worth pinning is everything around it: which
 * reservations count towards occupancy, that a wild model answer is clamped
 * before an admin sees it, and that a failed call says so instead of
 * inventing prices.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PricingRecommendationServiceTest {

    @Mock private PropertyRepository propertyRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private SeasonalRateRepository seasonalRateRepository;
    @Mock private LocalEventRepository localEventRepository;
    @Mock private DynamicPricingConfigService dynamicPricingConfigService;
    @Mock private AuditService auditService;
    @Mock private RestClient pricingAiRestClient;

    /** What the stubbed model returns for the next call. */
    private String modelAnswer;
    private PricingRecommendationService service;
    private Property property;
    private final UUID propertyId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        AppProperties appProperties = new AppProperties();
        appProperties.getAssistant().setApiKey("sk-ant-test");
        appProperties.getPricingAi().setModel("claude-sonnet-5");
        appProperties.getPricingAi().setMaxTokens(1200);
        appProperties.getPricingAi().setTimeoutMs(45000);

        service = serviceWith(appProperties);

        Address address = new Address();
        address.setCity("Cluj");
        property = Property.builder()
                .name("Apartament cluj")
                .address(address)
                .basePricePerNight(new BigDecimal("200.00"))
                .maxGuests(2)
                .build();
        property.setId(propertyId);

        when(propertyRepository.findById(propertyId)).thenReturn(Optional.of(property));
        when(dynamicPricingConfigService.getOrDefault(propertyId)).thenReturn(defaultConfig());
        when(seasonalRateRepository.findByPropertyIdOrderByStartDateAsc(propertyId)).thenReturn(List.of());
        when(localEventRepository.findByPropertyIdOrderByStartDateAsc(propertyId)).thenReturn(List.of());
    }

    /** Real service, with only the Anthropic round trip replaced. */
    private PricingRecommendationService serviceWith(AppProperties appProperties) {
        return new PricingRecommendationService(
                propertyRepository, reservationRepository, seasonalRateRepository, localEventRepository,
                dynamicPricingConfigService, auditService, appProperties, new ObjectMapper(),
                pricingAiRestClient) {
            @Override
            String callModel(String apiKey, String prompt) {
                return modelAnswer;
            }
        };
    }

    private DynamicPricingConfigResponse defaultConfig() {
        return new DynamicPricingConfigResponse(propertyId, false, null, null, 30,
                new BigDecimal("0.90"), new BigDecimal("1.20"), 7, new BigDecimal("0.95"));
    }

    private Reservation reservation(LocalDate in, LocalDate out, ReservationStatus status,
                                     ReservationSource source, String total) {
        Reservation reservation = Reservation.builder()
                .property(property)
                .checkInDate(in)
                .checkOutDate(out)
                .status(status)
                .source(source)
                .totalAmount(new BigDecimal(total))
                .currency("RON")
                .build();
        reservation.setId(UUID.randomUUID());
        return reservation;
    }

    private void stubClaude(String json) {
        modelAnswer = json;
    }

    @Test
    void recommend_countsOnlySoldNights_excludingMaintenanceAndNonBlocking() {
        LocalDate today = LocalDate.now();
        // Only this one is a real sold stay: 4 nights at 800 => ADR 200.
        Reservation sold = reservation(today.plusDays(1), today.plusDays(5),
                ReservationStatus.CONFIRMED, ReservationSource.DIRECT, "800.00");
        // Ours, not sold - must not inflate occupancy.
        Reservation maintenance = reservation(today.plusDays(6), today.plusDays(9),
                ReservationStatus.CONFIRMED, ReservationSource.MAINTENANCE, "0.00");

        // NON_BLOCKING (cancelled/no-show) is filtered by the query itself, so
        // the repository is stubbed as already excluding it.
        when(reservationRepository.findCalendarEntries(any(), any(), any(), any()))
                .thenReturn(List.of(sold, maintenance));
        stubClaude("""
                {"enabled":true,"minPrice":150,"maxPrice":320,"occupancyWindowDays":30,
                 "occupancyMultiplierMin":0.90,"occupancyMultiplierMax":1.25,
                 "leadTimeDays":7,"leadTimeMultiplier":0.95,"rationale":"Ocupare redusă."}
                """);

        AiPricingRecommendationResponse result = service.recommend(propertyId, actorId, "admin@bhstays.ro");

        assertThat(result.metricsUsed().bookedNights()).isEqualTo(4);
        assertThat(result.metricsUsed().windowNights()).isEqualTo(30);
        assertThat(result.metricsUsed().averageDailyRate()).isEqualByComparingTo("200.00");
        assertThat(result.metricsUsed().occupancyRate()).isEqualByComparingTo("0.13");
        assertThat(result.currency()).isEqualTo("RON");

        // The query must be asked to exclude cancellations and no-shows.
        verify(reservationRepository).findCalendarEntries(
                org.mockito.ArgumentMatchers.eq(propertyId), any(), any(),
                org.mockito.ArgumentMatchers.eq(ReservationStatus.NON_BLOCKING));
    }

    @Test
    void recommend_returnsTheConfigShapeTheUpdateEndpointAccepts() {
        when(reservationRepository.findCalendarEntries(any(), any(), any(), any())).thenReturn(List.of());
        stubClaude("""
                {"enabled":true,"minPrice":150,"maxPrice":320,"occupancyWindowDays":45,
                 "occupancyMultiplierMin":0.85,"occupancyMultiplierMax":1.40,
                 "leadTimeDays":10,"leadTimeMultiplier":0.90,"rationale":"Fără rezervări."}
                """);

        var recommendation = service.recommend(propertyId, actorId, "admin@bhstays.ro").recommendation();

        assertThat(recommendation.enabled()).isTrue();
        assertThat(recommendation.minPrice()).isEqualByComparingTo("150");
        assertThat(recommendation.maxPrice()).isEqualByComparingTo("320");
        assertThat(recommendation.occupancyWindowDays()).isEqualTo(45);
        assertThat(recommendation.occupancyMultiplierMin()).isEqualByComparingTo("0.85");
        assertThat(recommendation.occupancyMultiplierMax()).isEqualByComparingTo("1.40");
        assertThat(recommendation.leadTimeDays()).isEqualTo(10);
        assertThat(recommendation.leadTimeMultiplier()).isEqualByComparingTo("0.90");
    }

    @Test
    void recommend_clampsMultipliersAndOrdersThem_whenTheModelReturnsNonsense() {
        when(reservationRepository.findCalendarEntries(any(), any(), any(), any())).thenReturn(List.of());
        stubClaude("""
                {"enabled":true,"minPrice":900,"maxPrice":100,"occupancyWindowDays":5000,
                 "occupancyMultiplierMin":40,"occupancyMultiplierMax":0.10,
                 "leadTimeDays":-5,"leadTimeMultiplier":99,"rationale":"x"}
                """);

        var recommendation = service.recommend(propertyId, actorId, "admin@bhstays.ro").recommendation();

        // Multipliers land inside the allowed band, and min/max are put in order.
        assertThat(recommendation.occupancyMultiplierMin()).isEqualByComparingTo("0.50");
        assertThat(recommendation.occupancyMultiplierMax()).isEqualByComparingTo("3.00");
        assertThat(recommendation.leadTimeMultiplier()).isEqualByComparingTo("3.00");
        assertThat(recommendation.occupancyWindowDays()).isEqualTo(365);
        assertThat(recommendation.leadTimeDays()).isEqualTo(0);
        // Prices swapped back into floor/ceiling order.
        assertThat(recommendation.minPrice()).isEqualByComparingTo("100");
        assertThat(recommendation.maxPrice()).isEqualByComparingTo("900");
    }

    @Test
    void recommend_recordsAnAuditEntry() {
        when(reservationRepository.findCalendarEntries(any(), any(), any(), any())).thenReturn(List.of());
        stubClaude("{\"enabled\":false,\"rationale\":\"Prea puține date.\"}");

        service.recommend(propertyId, actorId, "admin@bhstays.ro");

        verify(auditService).recordForUserId(
                org.mockito.ArgumentMatchers.eq(AuditAction.PRICING_AI_RECOMMENDATION_REQUESTED),
                org.mockito.ArgumentMatchers.eq(actorId),
                org.mockito.ArgumentMatchers.eq("admin@bhstays.ro"),
                anyString(), any(), any());
    }

    @Test
    void recommend_failsLoudly_whenTheModelAnswerIsNotJson() {
        when(reservationRepository.findCalendarEntries(any(), any(), any(), any())).thenReturn(List.of());
        stubClaude("Nu pot genera acum.");

        assertThatThrownBy(() -> service.recommend(propertyId, actorId, "admin@bhstays.ro"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Recomandarea de preț");
    }

    @Test
    void recommend_failsLoudly_whenNoApiKeyIsConfigured() {
        AppProperties withoutKey = new AppProperties();
        withoutKey.getPricingAi().setModel("claude-sonnet-5");
        PricingRecommendationService noKey = serviceWith(withoutKey);
        when(reservationRepository.findCalendarEntries(any(), any(), any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> noKey.recommend(propertyId, actorId, "admin@bhstays.ro"))
                .isInstanceOf(ApiException.class);

        verify(auditService, never()).recordForUserId(any(), any(), any(), anyString(), any(), any());
    }

    @Test
    void recommend_passesThroughTheModelsSummaryReasonsAndWarnings() {
        when(reservationRepository.findCalendarEntries(any(), any(), any(), any())).thenReturn(List.of());
        stubClaude("""
                {"enabled":false,"summary":"Ocupare zero, nu activăm încă.",
                 "reasons":["Nicio rezervare în 30 de zile.","  ",null,"Tarif de bază peste piață."],
                 "warnings":["Prețul minim poate scădea sub costuri."]}
                """);

        var result = service.recommend(propertyId, actorId, "admin@bhstays.ro");

        assertThat(result.summary()).isEqualTo("Ocupare zero, nu activăm încă.");
        // Blank and null entries are dropped rather than rendered as empty rows.
        assertThat(result.reasons())
                .containsExactly("Nicio rezervare în 30 de zile.", "Tarif de bază peste piață.");
        assertThat(result.warnings()).contains("Prețul minim poate scădea sub costuri.");
    }

    /** Older prompts called the summary "rationale"; it must still surface. */
    @Test
    void recommend_fallsBackToRationale_whenSummaryIsAbsent() {
        when(reservationRepository.findCalendarEntries(any(), any(), any(), any())).thenReturn(List.of());
        stubClaude("{\"enabled\":false,\"rationale\":\"Prea puține date.\"}");

        assertThat(service.recommend(propertyId, actorId, "admin@bhstays.ro").summary())
                .isEqualTo("Prea puține date.");
    }

    @Test
    void recommend_reportsLowConfidenceAndMissingData_whenNothingIsBooked() {
        when(reservationRepository.findCalendarEntries(any(), any(), any(), any())).thenReturn(List.of());
        stubClaude("{\"enabled\":false,\"summary\":\"x\",\"warnings\":[]}");

        var result = service.recommend(propertyId, actorId, "admin@bhstays.ro");

        assertThat(result.confidence()).isEqualTo(AiPricingRecommendationResponse.Confidence.LOW);
        assertThat(result.missingData()).contains(
                "Nicio noapte rezervată în fereastra analizată.",
                "Nicio perioadă sezonieră configurată.",
                "Niciun eveniment local viitor înregistrat.");
        // The model said nothing was wrong; thin data is the server's to flag.
        assertThat(result.warnings()).isNotEmpty();
    }

    @Test
    void recommend_reportsHighConfidence_whenTheWindowIsWellBooked() {
        LocalDate today = LocalDate.now();
        when(reservationRepository.findCalendarEntries(any(), any(), any(), any()))
                .thenReturn(List.of(reservation(today.plusDays(1), today.plusDays(13),
                        ReservationStatus.CONFIRMED, ReservationSource.DIRECT, "2400.00")));
        when(seasonalRateRepository.findByPropertyIdOrderByStartDateAsc(propertyId))
                .thenReturn(List.of(new SeasonalRate()));
        stubClaude("{\"enabled\":true,\"minPrice\":150,\"maxPrice\":320,\"summary\":\"Cerere bună.\"}");

        var result = service.recommend(propertyId, actorId, "admin@bhstays.ro");

        assertThat(result.metricsUsed().bookedNights()).isEqualTo(12);
        assertThat(result.confidence()).isEqualTo(AiPricingRecommendationResponse.Confidence.HIGH);
    }

    @Test
    void recommend_reportsMediumConfidence_whenBookingsAreThinButReal() {
        LocalDate today = LocalDate.now();
        when(reservationRepository.findCalendarEntries(any(), any(), any(), any()))
                .thenReturn(List.of(reservation(today.plusDays(1), today.plusDays(3),
                        ReservationStatus.CONFIRMED, ReservationSource.DIRECT, "400.00")));
        stubClaude("{\"enabled\":false,\"summary\":\"Semnal slab.\"}");

        assertThat(service.recommend(propertyId, actorId, "admin@bhstays.ro").confidence())
                .isEqualTo(AiPricingRecommendationResponse.Confidence.MEDIUM);
    }

    @Test
    void recommend_exposesTheMetricsBehindTheSuggestion() {
        when(reservationRepository.findCalendarEntries(any(), any(), any(), any())).thenReturn(List.of());
        stubClaude("{\"enabled\":false,\"summary\":\"x\"}");

        var metrics = service.recommend(propertyId, actorId, "admin@bhstays.ro").metricsUsed();

        assertThat(metrics.windowDays()).isEqualTo(30);
        assertThat(metrics.basePricePerNight()).isEqualByComparingTo("200.00");
        assertThat(metrics.averageDailyRate()).isNull();
        assertThat(metrics.occupancyRate()).isEqualByComparingTo("0.00");
    }

    @Test
    void recommend_throwsNotFound_forAnUnknownProperty() {
        UUID unknown = UUID.randomUUID();
        when(propertyRepository.findById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.recommend(unknown, actorId, "admin@bhstays.ro"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
