package com.bhgroup.pms.service;

import com.bhgroup.pms.common.exception.BadRequestException;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.SeasonalRate;
import com.bhgroup.pms.dto.property.PriceQuoteResponse;
import com.bhgroup.pms.repository.SeasonalRateRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Computes an itemized price quote for a stay: seasonal/weekend nightly
 * rates, extra-guest fee, cleaning fee, and weekly/monthly discounts.
 * Replaces the old flat {@code basePricePerNight * nights} calculation.
 */
@Service
@RequiredArgsConstructor
public class PricingService {

    private static final int WEEKLY_DISCOUNT_THRESHOLD_NIGHTS = 7;
    private static final int MONTHLY_DISCOUNT_THRESHOLD_NIGHTS = 28;
    private static final String DEFAULT_CURRENCY = "RON";

    private final SeasonalRateRepository seasonalRateRepository;

    @Transactional(readOnly = true)
    public PriceQuoteResponse quote(Property property, LocalDate checkIn, LocalDate checkOut, int guests) {
        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
        List<SeasonalRate> seasons = seasonalRateRepository
                .findOverlappingRange(property.getId(), checkIn, checkOut.minusDays(1));

        BigDecimal subtotal = BigDecimal.ZERO;
        for (LocalDate date = checkIn; date.isBefore(checkOut); date = date.plusDays(1)) {
            BigDecimal nightlyRate = nightlyRate(property, seasons, date);
            if (nightlyRate == null) {
                return unavailable(checkIn, checkOut, nights, property, "No price configured for " + date);
            }
            subtotal = subtotal.add(nightlyRate);
        }

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

        return new PriceQuoteResponse(
                true, null, checkIn, checkOut, nights,
                subtotal.setScale(2, RoundingMode.HALF_UP),
                extraGuestFeeTotal.setScale(2, RoundingMode.HALF_UP),
                cleaningFee.setScale(2, RoundingMode.HALF_UP),
                discountPercent, discountAmount.setScale(2, RoundingMode.HALF_UP),
                total, DEFAULT_CURRENCY,
                property.getMinStayNights(), property.getMaxStayNights());
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

    private PriceQuoteResponse unavailable(LocalDate checkIn, LocalDate checkOut, int nights, Property property,
                                            String reason) {
        return new PriceQuoteResponse(
                false, reason, checkIn, checkOut, nights,
                null, null, null, null, null, null, DEFAULT_CURRENCY,
                property.getMinStayNights(), property.getMaxStayNights());
    }
}
