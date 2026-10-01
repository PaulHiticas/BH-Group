package com.bhstays.pms.service;

import com.bhstays.pms.common.exception.ApiException;
import com.bhstays.pms.common.exception.ResourceNotFoundException;
import com.bhstays.pms.config.AppProperties;
import com.bhstays.pms.domain.AuditAction;
import com.bhstays.pms.domain.Property;
import com.bhstays.pms.domain.Reservation;
import com.bhstays.pms.domain.ReservationSource;
import com.bhstays.pms.domain.ReservationStatus;
import com.bhstays.pms.dto.pricing.AiPricingRecommendationResponse;
import com.bhstays.pms.dto.pricing.DynamicPricingConfigResponse;
import com.bhstays.pms.repository.LocalEventRepository;
import com.bhstays.pms.repository.PropertyRepository;
import com.bhstays.pms.repository.ReservationRepository;
import com.bhstays.pms.repository.SeasonalRateRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

/**
 * Suggests a dynamic-pricing configuration for a property: gathers the real
 * occupancy and rate signals, asks Claude to turn them into settings, and
 * hands back the suggestion together with the numbers behind it.
 *
 * <p>Purely advisory. Nothing here writes to the property's configuration -
 * the admin applies a recommendation, if they want it, through the existing
 * {@code PUT /pricing/config}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PricingRecommendationService {

    /** Occupancy horizon when the property has never been configured. */
    private static final int FALLBACK_WINDOW_DAYS = 30;
    /** Guard rails the model's numbers are clamped into before anyone sees them. */
    private static final BigDecimal MIN_MULTIPLIER = new BigDecimal("0.50");
    private static final BigDecimal MAX_MULTIPLIER = new BigDecimal("3.00");
    private static final int MAX_WINDOW_DAYS = 365;
    private static final int MAX_LEAD_TIME_DAYS = 365;

    private static final String SYSTEM_PROMPT = """
            You are a revenue manager for a Romanian short-term rental company.
            Given one property's occupancy and rate signals, recommend a dynamic
            pricing configuration.

            Reply with ONLY a JSON object, no prose and no code fences, shaped:
            {
              "enabled": boolean,
              "minPrice": number,
              "maxPrice": number,
              "occupancyWindowDays": integer,
              "occupancyMultiplierMin": number,
              "occupancyMultiplierMax": number,
              "leadTimeDays": integer,
              "leadTimeMultiplier": number,
              "rationale": "two or three sentences, in Romanian, explaining the call"
            }

            Rules:
            - minPrice and maxPrice are absolute nightly floor and ceiling in the
              property's currency; keep minPrice below the base rate and maxPrice above it.
            - occupancyMultiplierMin applies when the window is empty (discount, so < 1.00)
              and occupancyMultiplierMax when it is full (premium, so > 1.00).
            - leadTimeDays is the last-minute horizon; leadTimeMultiplier is applied
              inside it (< 1.00 to fill gaps, > 1.00 only when demand is strong).
            - Multipliers must stay between 0.50 and 3.00.
            - Be conservative when there is little booking history: narrow multipliers,
              and leave "enabled" false if the signals are too thin to price on.
            """;

    private final PropertyRepository propertyRepository;
    private final ReservationRepository reservationRepository;
    private final SeasonalRateRepository seasonalRateRepository;
    private final LocalEventRepository localEventRepository;
    private final DynamicPricingConfigService dynamicPricingConfigService;
    private final AuditService auditService;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    // Field name matches the bean name on purpose: there are two RestClient
    // beans, and without a lombok.config copying @Qualifier onto the generated
    // constructor, by-name resolution is what keeps them apart.
    private final RestClient pricingAiRestClient;

    @Transactional(readOnly = true)
    public AiPricingRecommendationResponse recommend(UUID propertyId, UUID actorId, String actorEmail) {
        // Same resolution the rest of this controller uses: an unknown id is a
        // 404, never a recommendation built on nothing.
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        DynamicPricingConfigResponse currentConfig = dynamicPricingConfigService.getOrDefault(propertyId);
        int windowDays = currentConfig.occupancyWindowDays() > 0
                ? currentConfig.occupancyWindowDays()
                : FALLBACK_WINDOW_DAYS;

        PricingSignalData signals = collectSignals(property, windowDays);
        String prompt = buildPrompt(property, currentConfig, signals);
        JsonNode answer = askClaude(prompt, propertyId);

        AiPricingRecommendationResponse.RecommendedConfig recommendation = toRecommendation(answer, signals);
        String rationale = answer.path("rationale").asText("");

        auditService.recordForUserId(
                AuditAction.PRICING_AI_RECOMMENDATION_REQUESTED, actorId, actorEmail,
                "Recomandare AI de preț dinamic pentru proprietatea %s (ocupare %s%% pe %d zile)"
                        .formatted(property.getName(),
                                signals.occupancyRate().multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP),
                                windowDays),
                null, null);

        return new AiPricingRecommendationResponse(
                propertyId,
                signals.currency(),
                new AiPricingRecommendationResponse.PricingSignals(
                        windowDays, signals.bookedNights(), signals.windowNights(),
                        signals.occupancyRate(), signals.averageDailyRate(),
                        property.getBasePricePerNight(),
                        signals.seasonalRates(), signals.upcomingEvents()),
                recommendation,
                rationale,
                Instant.now());
    }

    /**
     * Occupancy and achieved rate over the window. Cancellations and no-shows
     * ({@link ReservationStatus#NON_BLOCKING}) never occupied the calendar, and
     * MAINTENANCE blocks are ours rather than sold nights - counting either
     * would overstate demand and push prices up on an empty property.
     */
    private PricingSignalData collectSignals(Property property, int windowDays) {
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(windowDays);

        List<Reservation> sold = reservationRepository
                .findCalendarEntries(property.getId(), from, to, ReservationStatus.NON_BLOCKING).stream()
                .filter(r -> r.getSource() != ReservationSource.MAINTENANCE)
                .toList();

        long bookedNights = 0;
        BigDecimal revenue = BigDecimal.ZERO;
        long revenueNights = 0;
        String currency = null;

        for (Reservation reservation : sold) {
            // Only the part of the stay inside the window counts towards it.
            LocalDate start = reservation.getCheckInDate().isBefore(from) ? from : reservation.getCheckInDate();
            LocalDate end = reservation.getCheckOutDate().isAfter(to) ? to : reservation.getCheckOutDate();
            long nightsInWindow = Math.max(0, ChronoUnit.DAYS.between(start, end));
            bookedNights += nightsInWindow;

            long stayNights = Math.max(1,
                    ChronoUnit.DAYS.between(reservation.getCheckInDate(), reservation.getCheckOutDate()));
            if (reservation.getTotalAmount() != null && nightsInWindow > 0) {
                // Pro-rate the stay's total over the nights that fall inside.
                revenue = revenue.add(reservation.getTotalAmount()
                        .multiply(BigDecimal.valueOf(nightsInWindow))
                        .divide(BigDecimal.valueOf(stayNights), 2, RoundingMode.HALF_UP));
                revenueNights += nightsInWindow;
                if (currency == null) {
                    currency = reservation.getCurrency();
                }
            }
        }

        BigDecimal occupancyRate = windowDays > 0
                ? BigDecimal.valueOf(bookedNights).divide(BigDecimal.valueOf(windowDays), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal adr = revenueNights > 0
                ? revenue.divide(BigDecimal.valueOf(revenueNights), 2, RoundingMode.HALF_UP)
                : null;

        int seasonalRates = seasonalRateRepository.findByPropertyIdOrderByStartDateAsc(property.getId()).size();
        int upcomingEvents = (int) localEventRepository.findByPropertyIdOrderByStartDateAsc(property.getId()).stream()
                .filter(event -> !event.getEndDate().isBefore(from))
                .count();

        return new PricingSignalData(
                (int) bookedNights, windowDays, occupancyRate, adr,
                // No booking has priced this property yet - fall back to the
                // currency reservations are created with.
                currency != null ? currency : "RON",
                seasonalRates, upcomingEvents);
    }

    private String buildPrompt(Property property, DynamicPricingConfigResponse config, PricingSignalData signals) {
        return """
                Property: %s (%s)
                Currency: %s
                Base rate per night: %s
                Max guests: %d

                Next %d days:
                - nights booked: %d of %d (occupancy %s)
                - average nightly rate achieved: %s
                - seasonal rates configured: %d
                - upcoming local events: %d

                Current dynamic pricing configuration:
                - enabled: %s
                - minPrice: %s, maxPrice: %s
                - occupancyWindowDays: %d
                - occupancyMultiplierMin: %s, occupancyMultiplierMax: %s
                - leadTimeDays: %d, leadTimeMultiplier: %s
                """.formatted(
                property.getName(),
                city(property),
                signals.currency(),
                property.getBasePricePerNight() != null ? property.getBasePricePerNight().toPlainString() : "nesetat",
                property.getMaxGuests(),
                signals.windowNights(), signals.bookedNights(), signals.windowNights(),
                signals.occupancyRate().toPlainString(),
                signals.averageDailyRate() != null ? signals.averageDailyRate().toPlainString() : "fără rezervări",
                signals.seasonalRates(), signals.upcomingEvents(),
                config.enabled(),
                config.minPrice() != null ? config.minPrice().toPlainString() : "nesetat",
                config.maxPrice() != null ? config.maxPrice().toPlainString() : "nesetat",
                config.occupancyWindowDays(),
                config.occupancyMultiplierMin().toPlainString(),
                config.occupancyMultiplierMax().toPlainString(),
                config.leadTimeDays(),
                config.leadTimeMultiplier().toPlainString());
    }

    private String city(Property property) {
        if (property.getAddress() == null || property.getAddress().getCity() == null) {
            return "oraș necunoscut";
        }
        return property.getAddress().getCity();
    }

    private JsonNode askClaude(String prompt, UUID propertyId) {
        String apiKey = appProperties.getAssistant().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "PRICING_AI_UNAVAILABLE",
                    "Recomandările AI de preț nu sunt configurate (lipsește cheia Anthropic).");
        }

        String text;
        try {
            text = callModel(apiKey, prompt);
        } catch (Exception ex) {
            // Unlike the chat assistant, a recommendation has no useful
            // fallback: inventing numbers would be worse than saying so.
            log.error("Pricing recommendation call failed for property {}", propertyId, ex);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "PRICING_AI_UNAVAILABLE",
                    "Recomandarea de preț nu a putut fi generată. Încearcă din nou în câteva momente.");
        }

        return parseJson(text, propertyId);
    }

    /**
     * The raw Anthropic round trip, kept as its own seam so tests can exercise
     * the signal gathering, clamping and failure handling around it without
     * standing up the RestClient fluent chain.
     */
    String callModel(String apiKey, String prompt) {
        AnthropicResponse response = pricingAiRestClient.post()
                .uri("/v1/messages")
                .header("x-api-key", apiKey)
                .body(new AnthropicRequest(
                        appProperties.getPricingAi().getModel(),
                        appProperties.getPricingAi().getMaxTokens(),
                        SYSTEM_PROMPT,
                        List.of(new AnthropicMessage("user", prompt))))
                .retrieve()
                .body(AnthropicResponse.class);

        return response == null || response.content() == null || response.content().isEmpty()
                ? null
                : response.content().get(0).text();
    }

    private JsonNode parseJson(String text, UUID propertyId) {
        if (text == null || text.isBlank()) {
            log.error("Pricing recommendation for property {} came back empty", propertyId);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "PRICING_AI_UNAVAILABLE",
                    "Recomandarea de preț nu a putut fi generată. Încearcă din nou în câteva momente.");
        }
        // Tolerate a model that wraps its JSON in prose or a code fence.
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            text = text.substring(start, end + 1);
        }
        try {
            return objectMapper.readTree(text);
        } catch (Exception ex) {
            log.error("Pricing recommendation for property {} was not valid JSON", propertyId, ex);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "PRICING_AI_UNAVAILABLE",
                    "Recomandarea de preț nu a putut fi interpretată. Încearcă din nou.");
        }
    }

    /**
     * Clamps every number into the range the update endpoint would accept, so
     * a recommendation can always be applied as-is and a hallucinated 40x
     * multiplier never reaches an admin as a suggestion.
     */
    private AiPricingRecommendationResponse.RecommendedConfig toRecommendation(
            JsonNode node, PricingSignalData signals) {

        BigDecimal minPrice = decimalOrNull(node, "minPrice");
        BigDecimal maxPrice = decimalOrNull(node, "maxPrice");
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            BigDecimal swap = minPrice;
            minPrice = maxPrice;
            maxPrice = swap;
        }

        BigDecimal occupancyMin = clampMultiplier(decimalOrNull(node, "occupancyMultiplierMin"), new BigDecimal("0.90"));
        BigDecimal occupancyMax = clampMultiplier(decimalOrNull(node, "occupancyMultiplierMax"), new BigDecimal("1.20"));
        if (occupancyMin.compareTo(occupancyMax) > 0) {
            BigDecimal swap = occupancyMin;
            occupancyMin = occupancyMax;
            occupancyMax = swap;
        }

        return new AiPricingRecommendationResponse.RecommendedConfig(
                node.path("enabled").asBoolean(false),
                minPrice,
                maxPrice,
                clampInt(node.path("occupancyWindowDays").asInt(signals.windowNights()), 1, MAX_WINDOW_DAYS),
                occupancyMin,
                occupancyMax,
                clampInt(node.path("leadTimeDays").asInt(7), 0, MAX_LEAD_TIME_DAYS),
                clampMultiplier(decimalOrNull(node, "leadTimeMultiplier"), BigDecimal.ONE));
    }

    private BigDecimal decimalOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.decimalValue().setScale(2, RoundingMode.HALF_UP) : null;
    }

    private BigDecimal clampMultiplier(BigDecimal value, BigDecimal fallback) {
        BigDecimal candidate = value != null ? value : fallback;
        if (candidate.compareTo(MIN_MULTIPLIER) < 0) return MIN_MULTIPLIER;
        if (candidate.compareTo(MAX_MULTIPLIER) > 0) return MAX_MULTIPLIER;
        return candidate.setScale(2, RoundingMode.HALF_UP);
    }

    private int clampInt(int value, int min, int max) {
        return Math.min(max, Math.max(min, value));
    }

    private record PricingSignalData(
            int bookedNights,
            int windowNights,
            BigDecimal occupancyRate,
            BigDecimal averageDailyRate,
            String currency,
            int seasonalRates,
            int upcomingEvents
    ) {
    }

    // Mirrors the assistant's Anthropic wire records, minus the tool plumbing
    // a single structured answer does not need.
    record AnthropicMessage(String role, String content) {
    }

    record AnthropicRequest(
            String model,
            @JsonProperty("max_tokens") int maxTokens,
            String system,
            List<AnthropicMessage> messages
    ) {
    }

    record AnthropicContentBlock(String type, String text) {
    }

    record AnthropicResponse(List<AnthropicContentBlock> content) {
    }
}
