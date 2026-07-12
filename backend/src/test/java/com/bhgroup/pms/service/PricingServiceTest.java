package com.bhgroup.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bhgroup.pms.common.exception.BadRequestException;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.SeasonalRate;
import com.bhgroup.pms.dto.property.PriceQuoteResponse;
import com.bhgroup.pms.repository.SeasonalRateRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock
    private SeasonalRateRepository seasonalRateRepository;

    private PricingService pricingService;
    private Property property;

    @BeforeEach
    void setUp() {
        pricingService = new PricingService(seasonalRateRepository);
        property = Property.builder()
                .name("Test Apartment")
                .basePricePerNight(new BigDecimal("100.00"))
                .build();
        property.setId(UUID.randomUUID());
    }

    @Test
    void quote_flatBasePriceAcrossWeekdays() {
        when(seasonalRateRepository.findOverlappingRange(any(), any(), any())).thenReturn(List.of());

        // Monday 2026-08-03 -> Thursday 2026-08-06: 3 weekday nights, no weekend pricing configured
        LocalDate checkIn = LocalDate.of(2026, 8, 3);
        LocalDate checkOut = LocalDate.of(2026, 8, 6);

        PriceQuoteResponse quote = pricingService.quote(property, checkIn, checkOut, 1);

        assertThat(quote.available()).isTrue();
        assertThat(quote.nights()).isEqualTo(3);
        assertThat(quote.subtotal()).isEqualByComparingTo("300.00");
        assertThat(quote.totalAmount()).isEqualByComparingTo("300.00");
    }

    @Test
    void quote_appliesWeekendRateOnFridayAndSaturday() {
        when(seasonalRateRepository.findOverlappingRange(any(), any(), any())).thenReturn(List.of());
        property.setWeekendPricePerNight(new BigDecimal("150.00"));

        // Friday 2026-08-07 -> Monday 2026-08-10: Fri, Sat (weekend) + Sun (weekday)
        LocalDate checkIn = LocalDate.of(2026, 8, 7);
        LocalDate checkOut = LocalDate.of(2026, 8, 10);

        PriceQuoteResponse quote = pricingService.quote(property, checkIn, checkOut, 1);

        // Fri 150 + Sat 150 + Sun 100 = 400
        assertThat(quote.subtotal()).isEqualByComparingTo("400.00");
    }

    @Test
    void quote_seasonalRateOverridesBaseAndWeekendPricing() {
        SeasonalRate summerSeason = SeasonalRate.builder()
                .property(property)
                .label("Summer")
                .startDate(LocalDate.of(2026, 8, 1))
                .endDate(LocalDate.of(2026, 8, 31))
                .pricePerNight(new BigDecimal("250.00"))
                .build();
        when(seasonalRateRepository.findOverlappingRange(any(), any(), any())).thenReturn(List.of(summerSeason));
        property.setWeekendPricePerNight(new BigDecimal("150.00"));

        LocalDate checkIn = LocalDate.of(2026, 8, 7);
        LocalDate checkOut = LocalDate.of(2026, 8, 9);

        PriceQuoteResponse quote = pricingService.quote(property, checkIn, checkOut, 1);

        // both nights fall in the season, so seasonal 250/night wins over weekend 150/night
        assertThat(quote.subtotal()).isEqualByComparingTo("500.00");
    }

    @Test
    void quote_addsExtraGuestFeeOnlyForGuestsAboveIncluded() {
        when(seasonalRateRepository.findOverlappingRange(any(), any(), any())).thenReturn(List.of());
        property.setBaseGuestsIncluded(2);
        property.setExtraGuestFee(new BigDecimal("20.00"));

        LocalDate checkIn = LocalDate.of(2026, 8, 3);
        LocalDate checkOut = LocalDate.of(2026, 8, 6); // 3 nights

        PriceQuoteResponse quoteWithinIncluded = pricingService.quote(property, checkIn, checkOut, 2);
        assertThat(quoteWithinIncluded.extraGuestFee()).isEqualByComparingTo("0.00");

        PriceQuoteResponse quoteWithExtra = pricingService.quote(property, checkIn, checkOut, 4);
        // 2 extra guests * 20/night * 3 nights = 120
        assertThat(quoteWithExtra.extraGuestFee()).isEqualByComparingTo("120.00");
        assertThat(quoteWithExtra.totalAmount()).isEqualByComparingTo("420.00");
    }

    @Test
    void quote_addsCleaningFeeOnceRegardlessOfStayLength() {
        when(seasonalRateRepository.findOverlappingRange(any(), any(), any())).thenReturn(List.of());
        property.setCleaningFee(new BigDecimal("75.00"));

        LocalDate checkIn = LocalDate.of(2026, 8, 3);
        LocalDate checkOut = LocalDate.of(2026, 8, 10); // 7 nights

        PriceQuoteResponse quote = pricingService.quote(property, checkIn, checkOut, 1);

        assertThat(quote.cleaningFee()).isEqualByComparingTo("75.00");
        // subtotal 700 + cleaning 75, weekly discount also applies at exactly 7 nights (see next test)
    }

    @Test
    void quote_appliesWeeklyDiscountAtSevenNightsButNotBelow() {
        when(seasonalRateRepository.findOverlappingRange(any(), any(), any())).thenReturn(List.of());
        property.setWeeklyDiscountPercent(new BigDecimal("10"));

        LocalDate checkIn = LocalDate.of(2026, 8, 3);

        PriceQuoteResponse sixNights = pricingService.quote(property, checkIn, checkIn.plusDays(6), 1);
        assertThat(sixNights.discountPercent()).isNull();
        assertThat(sixNights.discountAmount()).isEqualByComparingTo("0.00");

        PriceQuoteResponse sevenNights = pricingService.quote(property, checkIn, checkIn.plusDays(7), 1);
        assertThat(sevenNights.discountPercent()).isEqualByComparingTo("10");
        // subtotal 700 * 10% = 70
        assertThat(sevenNights.discountAmount()).isEqualByComparingTo("70.00");
        assertThat(sevenNights.totalAmount()).isEqualByComparingTo("630.00");
    }

    @Test
    void quote_monthlyDiscountTakesPrecedenceOverWeeklyAt28Nights() {
        when(seasonalRateRepository.findOverlappingRange(any(), any(), any())).thenReturn(List.of());
        property.setWeeklyDiscountPercent(new BigDecimal("10"));
        property.setMonthlyDiscountPercent(new BigDecimal("25"));

        LocalDate checkIn = LocalDate.of(2026, 8, 1);
        PriceQuoteResponse quote = pricingService.quote(property, checkIn, checkIn.plusDays(28), 1);

        assertThat(quote.discountPercent()).isEqualByComparingTo("25");
    }

    @Test
    void quote_unavailableWhenNoPriceConfiguredForANight() {
        when(seasonalRateRepository.findOverlappingRange(any(), any(), any())).thenReturn(List.of());
        Property propertyWithNoBasePrice = Property.builder().name("No price").build();
        propertyWithNoBasePrice.setId(UUID.randomUUID());

        PriceQuoteResponse quote = pricingService.quote(
                propertyWithNoBasePrice, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 4), 1);

        assertThat(quote.available()).isFalse();
        assertThat(quote.unavailableReason()).isNotBlank();
        assertThat(quote.totalAmount()).isNull();
    }

    @Test
    void validateStayLength_rejectsBelowMinimum() {
        property.setMinStayNights(3);
        assertThat(catchBadRequest(() -> pricingService.validateStayLength(property, 2))).isTrue();
        pricingService.validateStayLength(property, 3); // does not throw
    }

    @Test
    void validateStayLength_rejectsAboveMaximum() {
        property.setMaxStayNights(14);
        assertThat(catchBadRequest(() -> pricingService.validateStayLength(property, 15))).isTrue();
        pricingService.validateStayLength(property, 14); // does not throw
    }

    private boolean catchBadRequest(Runnable action) {
        try {
            action.run();
            return false;
        } catch (BadRequestException ex) {
            return true;
        }
    }
}
