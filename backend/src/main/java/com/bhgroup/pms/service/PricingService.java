package com.bhgroup.pms.service;

import com.bhgroup.pms.common.exception.BadRequestException;
import com.bhgroup.pms.domain.DynamicPricingConfig;
import com.bhgroup.pms.domain.LocalEvent;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.Reservation;
import com.bhgroup.pms.domain.ReservationSource;
import com.bhgroup.pms.domain.ReservationStatus;
import com.bhgroup.pms.domain.SeasonalRate;
import com.bhgroup.pms.dto.pricing.DynamicPriceBreakdownResponse;
import com.bhgroup.pms.dto.pricing.NightlyPriceBreakdown;
import com.bhgroup.pms.dto.property.PriceQuoteResponse;
import com.bhgroup.pms.repository.DynamicPricingConfigRepository;
import com.bhgroup.pms.repository.LocalEventRepository;
import com.bhgroup.pms.repository.ReservationRepository;
import com.bhgroup.pms.repository.SeasonalRateRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Computes an itemized price quote for a stay: seasonal/weekend nightly
 * rates, an optional dynamic-pricing layer on top, extra-guest fee,
 * cleaning fee, and weekly/monthly discounts. Replaces the old flat
 * {@code basePricePerNight * nights} calculation.
 *
 * <p>Dynamic pricing (see {@link DynamicPricingConfig}) is opt-in per
 * property. When disabled (the default), every factor below is 1.0 and no
 * clamp is applied, so the result is byte-for-byte identical to the plain
 * seasonal/weekend/base calculation - the dynamic layer never runs its
 * (more expensive) reservation/event lookups in that case either.
 *
 * <p>For each sellable night, the nightly rate is:
 * {@code base * occupancyFactor * leadTimeFactor * eventFactor}, then
 * clamped to {@code [minPrice, maxPrice]} if configured - the guardrails
 * always have the last word, after every multiplicative factor.
 */
@Service
@RequiredArgsConstructor
public class PricingService {

    private static final int WEEKLY_DISCOUNT_THRESHOLD_NIGHTS = 7;
    private static final int MONTHLY_DISCOUNT_THRESHOLD_NIGHTS = 28;
    private static final String DEFAULT_CURRENCY = "RON";

    /**
     * Sources counted as genuine commercial demand for the occupancy
     * factor. MAINTENANCE is handled separately (removed from both the
     * numerator and the denominator - see {@link #occupancyStats}).
     * OTHER is excluded until its semantics are clarified - it could be
     * anything from a courtesy stay to a data-migration placeholder, and
     * treating it as demand risks inflating prices for reasons a guest
     * never caused.
     */
    private static final Set<ReservationSource> COMMERCIAL_SOURCES =
            Set.of(ReservationSource.DIRECT, ReservationSource.AIRBNB, ReservationSource.BOOKING_COM);

    /**
     * PENDING is deliberately excluded: it's an unconfirmed hold anyone
     * can create (including by testing the booking flow repeatedly), so
     * counting it as demand would let a single visitor manipulate the
     * price up for everyone else just by starting checkouts and abandoning
     * them.
     */
    private static final Set<ReservationStatus> COMMERCIAL_STATUSES =
            Set.of(ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN, ReservationStatus.CHECKED_OUT);

    private final SeasonalRateRepository seasonalRateRepository;
    private final ReservationRepository reservationRepository;
    private final DynamicPricingConfigRepository dynamicPricingConfigRepository;
    private final LocalEventRepository localEventRepository;

    @Transactional(readOnly = true)
    public PriceQuoteResponse quote(Property property, LocalDate checkIn, LocalDate checkOut, int guests) {
        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        List<SeasonalRate> seasons = seasonalRateRepository
                .findOverlappingRange(property.getId(), checkIn, checkOut.minusDays(1));
        DynamicPricingContext context = loadDynamicPricingContext(property, checkIn, checkOut);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            BigDecimal baseRate = nightlyRate(property, seasons, date);
            if (baseRate == null) {
                return unavailable(checkIn, checkOut, nights, property, "No price configured for " + date);
            }
            NightResolution resolution = resolveNight(date, checkIn, baseRate, context);
            subtotal = subtotal.add(resolution.rateAfterClamp());
        }

        FeesAndTotal feesAndTotal = computeFeesAndTotal(property, guests, nights, subtotal);

        return new PriceQuoteResponse(
                true, null, checkIn, checkOut, nights,
                subtotal.setScale(2, RoundingMode.HALF_UP),
                feesAndTotal.extraGuestFee().setScale(2, RoundingMode.HALF_UP),
                feesAndTotal.cleaningFee().setScale(2, RoundingMode.HALF_UP),
                feesAndTotal.discountPercent(), feesAndTotal.discountAmount().setScale(2, RoundingMode.HALF_UP),
                feesAndTotal.total(), DEFAULT_CURRENCY,
                property.getMinStayNights(), property.getMaxStayNights());
    }

    /**
     * Same nightly computation as {@link #quote}, but returns every factor
     * for every night instead of just the total - so an admin/owner can
     * see exactly why a given night costs what it costs.
     */
    @Transactional(readOnly = true)
    public DynamicPriceBreakdownResponse priceBreakdown(Property property, LocalDate checkIn, LocalDate checkOut,
                                                          int guests) {
        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        List<SeasonalRate> seasons = seasonalRateRepository
                .findOverlappingRange(property.getId(), checkIn, checkOut.minusDays(1));
        DynamicPricingContext context = loadDynamicPricingContext(property, checkIn, checkOut);

        List<NightlyPriceBreakdown> nightlyBreakdown = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            BigDecimal baseRate = nightlyRate(property, seasons, date);
            if (baseRate == null) {
                return new DynamicPriceBreakdownResponse(
                        false, "No price configured for " + date, checkIn, checkOut, nights,
                        List.of(), null, null, null, null, null, null, DEFAULT_CURRENCY);
            }
            NightResolution resolution = resolveNight(date, checkIn, baseRate, context);
            nightlyBreakdown.add(new NightlyPriceBreakdown(
                    date, resolution.baseRate(), resolution.occupancyFactor(),
                    resolution.commerciallyBookedNights(), resolution.sellableNights(),
                    resolution.leadTimeFactor(), resolution.eventFactor(),
                    resolution.rateBeforeClamp(), resolution.rateAfterClamp()));
            subtotal = subtotal.add(resolution.rateAfterClamp());
        }

        FeesAndTotal feesAndTotal = computeFeesAndTotal(property, guests, nights, subtotal);

        return new DynamicPriceBreakdownResponse(
                true, null, checkIn, checkOut, nights, nightlyBreakdown,
                subtotal.setScale(2, RoundingMode.HALF_UP),
                feesAndTotal.extraGuestFee().setScale(2, RoundingMode.HALF_UP),
                feesAndTotal.cleaningFee().setScale(2, RoundingMode.HALF_UP),
                feesAndTotal.discountPercent(), feesAndTotal.discountAmount().setScale(2, RoundingMode.HALF_UP),
                feesAndTotal.total(), DEFAULT_CURRENCY);
    }

    /** Throws if the guest count is below 1 or exceeds the property's configured maximum. */
    public void validateGuestCount(Property property, int guests) {
        if (guests < 1) {
            throw new BadRequestException("Number of guests must be at least 1");
        }
        if (guests > property.getMaxGuests()) {
            throw new BadRequestException("This property accepts a maximum of " + property.getMaxGuests() + " guests");
        }
    }

    /** Throws if the stay length violates the property's configured min/max stay. */
    public void validateStayLength(Property property, int nights) {
        Integer min = property.getMinStayNights();
        Integer max = property.getMaxStayNights();
        if (min != null && nights < min) {
            throw new BadRequestException("Minimum stay for this property is " + min + " nights");
        }
        if (max != null && nights > max) {
            throw new BadRequestException("Maximum stay for this property is " + max + " nights");
        }
    }

    private BigDecimal nightlyRate(Property property, List<SeasonalRate> seasons, LocalDate date) {
        for (SeasonalRate season : seasons) {
            if (season.covers(date)) {
                return season.getPricePerNight();
            }
        }
        boolean isWeekendNight = date.getDayOfWeek() == DayOfWeek.FRIDAY || date.getDayOfWeek() == DayOfWeek.SATURDAY;
        if (isWeekendNight && property.getWeekendPricePerNight() != null) {
            return property.getWeekendPricePerNight();
        }
        return property.getBasePricePerNight();
    }

    private BigDecimal extraGuestFee(Property property, int guests, int nights) {
        Integer included = property.getBaseGuestsIncluded();
        BigDecimal fee = property.getExtraGuestFee();
        if (included == null || fee == null || guests <= included) {
            return BigDecimal.ZERO;
        }
        int extraGuests = guests - included;
        return fee.multiply(BigDecimal.valueOf(extraGuests)).multiply(BigDecimal.valueOf(nights));
    }

    private BigDecimal discountPercentFor(Property property, int nights) {
        if (nights >= MONTHLY_DISCOUNT_THRESHOLD_NIGHTS && property.getMonthlyDiscountPercent() != null) {
            return property.getMonthlyDiscountPercent();
        }
        if (nights >= WEEKLY_DISCOUNT_THRESHOLD_NIGHTS && property.getWeeklyDiscountPercent() != null) {
            return property.getWeeklyDiscountPercent();
        }
        return null;
    }

    private record FeesAndTotal(BigDecimal extraGuestFee, BigDecimal discountPercent, BigDecimal discountAmount,
                                 BigDecimal cleaningFee, BigDecimal total) {
    }

    private FeesAndTotal computeFeesAndTotal(Property property, int guests, int nights, BigDecimal subtotal) {
        BigDecimal extraGuestFeeTotal = extraGuestFee(property, guests, nights);

        BigDecimal discountPercent = discountPercentFor(property, nights);
        BigDecimal discountAmount = discountPercent != null
                ? subtotal.multiply(discountPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal cleaningFee = property.getCleaningFee() != null ? property.getCleaningFee() : BigDecimal.ZERO;

        BigDecimal total = subtotal
                .subtract(discountAmount)
                .add(extraGuestFeeTotal)
                .add(cleaningFee)
                .setScale(2, RoundingMode.HALF_UP);

        return new FeesAndTotal(extraGuestFeeTotal, discountPercent, discountAmount, cleaningFee, total);
    }

    private PriceQuoteResponse unavailable(LocalDate checkIn, LocalDate checkOut, int nights, Property property,
                                            String reason) {
        return new PriceQuoteResponse(
                false, reason, checkIn, checkOut, nights,
                null, null, null, null, null, null, DEFAULT_CURRENCY,
                property.getMinStayNights(), property.getMaxStayNights());
    }

    // ------------------------------------------------------------------
    // Dynamic pricing layer
    // ------------------------------------------------------------------

    private record DynamicPricingContext(DynamicPricingConfig config, List<Reservation> windowReservations,
                                          List<LocalEvent> events) {

        static DynamicPricingContext disabled() {
            return new DynamicPricingContext(null, List.of(), List.of());
        }

        boolean isEnabled() {
            return config != null && config.isEnabled();
        }
    }

    private record OccupancyWindowStats(int sellableNights, int commerciallyBookedNights) {
    }

    private record NightResolution(BigDecimal baseRate, BigDecimal occupancyFactor, int commerciallyBookedNights,
                                    int sellableNights, BigDecimal leadTimeFactor, BigDecimal eventFactor,
                                    BigDecimal rateBeforeClamp, BigDecimal rateAfterClamp) {
    }

    /**
     * Loads everything the dynamic layer needs for the whole stay in one
     * pass - the config, every reservation overlapping the combined
     * stay+occupancy-window span, and every event that could apply -
     * instead of one query per night. Returns a disabled context (and
     * skips those queries entirely) when the property has no config or
     * hasn't opted in.
     */
    private DynamicPricingContext loadDynamicPricingContext(Property property, LocalDate checkIn, LocalDate checkOut) {
        DynamicPricingConfig config = dynamicPricingConfigRepository.findByPropertyId(property.getId()).orElse(null);
        if (config == null || !config.isEnabled()) {
            return DynamicPricingContext.disabled();
        }

        LocalDate windowFrom = checkIn.minusDays(config.getOccupancyWindowDays());
        LocalDate windowTo = checkOut.plusDays(config.getOccupancyWindowDays());
        List<Reservation> windowReservations = reservationRepository.findCalendarEntries(
                property.getId(), windowFrom, windowTo, ReservationStatus.NON_BLOCKING);

        String normalizedCity = normalizeCity(property.getAddress().getCity());
        List<LocalEvent> events = localEventRepository.findRelevantForPricing(
                property.getId(), normalizedCity, checkIn, checkOut.minusDays(1));

        return new DynamicPricingContext(config, windowReservations, events);
    }

    private NightResolution resolveNight(LocalDate night, LocalDate checkIn, BigDecimal baseRate,
                                          DynamicPricingContext context) {
        if (!context.isEnabled()) {
            return new NightResolution(baseRate, BigDecimal.ONE, 0, 0, BigDecimal.ONE, BigDecimal.ONE,
                    baseRate, baseRate);
        }

        DynamicPricingConfig config = context.config();

        OccupancyWindowStats stats = occupancyStats(night, config.getOccupancyWindowDays(), context.windowReservations());
        BigDecimal occupancyFactor = occupancyFactor(stats, config);
        BigDecimal leadTimeFactor = leadTimeFactor(checkIn, config);
        BigDecimal eventFactor = eventFactor(night, context.events());

        BigDecimal rateBeforeClamp = baseRate
                .multiply(occupancyFactor)
                .multiply(leadTimeFactor)
                .multiply(eventFactor)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal rateAfterClamp = clamp(rateBeforeClamp, config);

        return new NightResolution(baseRate, occupancyFactor, stats.commerciallyBookedNights(), stats.sellableNights(),
                leadTimeFactor, eventFactor, rateBeforeClamp, rateAfterClamp);
    }

    /**
     * Only nights that are actually sellable count toward occupancy:
     * maintenance-blocked nights are removed from both the numerator and
     * the denominator, so a property closed for renovation doesn't read
     * as "fully booked" and doesn't get priced up for it. PENDING holds
     * and OTHER-source rows are simply not counted anywhere here (they're
     * neither maintenance nor commercial demand for this purpose).
     */
    private OccupancyWindowStats occupancyStats(LocalDate night, int windowDays, List<Reservation> reservations) {
        LocalDate windowStart = night.minusDays(windowDays);
        LocalDate windowEnd = night.plusDays(windowDays);
        int totalWindowNights = windowDays * 2 + 1;

        Set<LocalDate> maintenanceBlocked = new HashSet<>();
        Set<LocalDate> commerciallyBooked = new HashSet<>();

        for (Reservation reservation : reservations) {
            LocalDate from = reservation.getCheckInDate().isAfter(windowStart) ? reservation.getCheckInDate() : windowStart;
            LocalDate toExclusive = reservation.getCheckOutDate().isBefore(windowEnd.plusDays(1))
                    ? reservation.getCheckOutDate() : windowEnd.plusDays(1);

            for (LocalDate date = from; date.isBefore(toExclusive); date = date.plusDays(1)) {
                if (reservation.getSource() == ReservationSource.MAINTENANCE) {
                    maintenanceBlocked.add(date);
                } else if (COMMERCIAL_SOURCES.contains(reservation.getSource())
                        && COMMERCIAL_STATUSES.contains(reservation.getStatus())) {
                    commerciallyBooked.add(date);
                }
            }
        }

        // Defensive: the DB's overlap exclusion constraint already keeps blocking
        // reservations from sharing a date, so this should be a no-op in practice.
        commerciallyBooked.removeAll(maintenanceBlocked);

        int sellableNights = totalWindowNights - maintenanceBlocked.size();
        return new OccupancyWindowStats(sellableNights, commerciallyBooked.size());
    }

    /**
     * No sellable nights in the window (e.g. the whole thing is under
     * maintenance) means there's no meaningful occupancy signal - stay
     * neutral (1.00) rather than dividing by zero or collapsing to the
     * minimum multiplier.
     */
    private BigDecimal occupancyFactor(OccupancyWindowStats stats, DynamicPricingConfig config) {
        if (stats.sellableNights() <= 0) {
            return BigDecimal.ONE;
        }

        BigDecimal occupancy = BigDecimal.valueOf(stats.commerciallyBookedNights())
                .divide(BigDecimal.valueOf(stats.sellableNights()), 6, RoundingMode.HALF_UP);
        if (occupancy.compareTo(BigDecimal.ZERO) < 0) {
            occupancy = BigDecimal.ZERO;
        } else if (occupancy.compareTo(BigDecimal.ONE) > 0) {
            occupancy = BigDecimal.ONE;
        }

        BigDecimal min = config.getOccupancyMultiplierMin();
        BigDecimal range = config.getOccupancyMultiplierMax().subtract(min);
        return min.add(range.multiply(occupancy)).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal leadTimeFactor(LocalDate checkIn, DynamicPricingConfig config) {
        long daysUntilCheckIn = ChronoUnit.DAYS.between(LocalDate.now(), checkIn);
        if (daysUntilCheckIn <= config.getLeadTimeDays()) {
            return config.getLeadTimeMultiplier();
        }
        return BigDecimal.ONE;
    }

    /** When multiple events overlap a night, the highest multiplier wins. */
    private BigDecimal eventFactor(LocalDate night, List<LocalEvent> events) {
        BigDecimal max = BigDecimal.ONE;
        for (LocalEvent event : events) {
            if (!night.isBefore(event.getStartDate()) && !night.isAfter(event.getEndDate())
                    && event.getPriceMultiplier().compareTo(max) > 0) {
                max = event.getPriceMultiplier();
            }
        }
        return max;
    }

    /** Guardrails applied last, after every multiplicative factor - they always have the final word. */
    private BigDecimal clamp(BigDecimal rate, DynamicPricingConfig config) {
        BigDecimal result = rate;
        if (config.getMinPrice() != null && result.compareTo(config.getMinPrice()) < 0) {
            result = config.getMinPrice();
        }
        if (config.getMaxPrice() != null && result.compareTo(config.getMaxPrice()) > 0) {
            result = config.getMaxPrice();
        }
        return result.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeCity(String city) {
        return city == null ? "" : city.trim().toLowerCase(Locale.ROOT);
    }
}
