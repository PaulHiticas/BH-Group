package com.bhgroup.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bhgroup.pms.common.exception.BadRequestException;
import com.bhgroup.pms.domain.Address;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private DynamicPricingConfigRepository dynamicPricingConfigRepository;

    @Mock
    private LocalEventRepository localEventRepository;

    private PricingService pricingService;
    private Property property;

    @BeforeEach
    void setUp() {
        pricingService = new PricingService(
                seasonalRateRepository, reservationRepository, dynamicPricingConfigRepository, localEventRepository);
        Address address = new Address();
        address.setCity("Brasov");
        property = Property.builder()
                .name("Test Apartment")
                .basePricePerNight(new BigDecimal("100.00"))
                .address(address)
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
    void validateGuestCount_rejectsBelowOne() {
        assertThat(catchBadRequest(() -> pricingService.validateGuestCount(property, 0))).isTrue();
    }

    @Test
    void validateGuestCount_rejectsAboveMaxGuests() {
        property.setMaxGuests(4);
        assertThat(catchBadRequest(() -> pricingService.validateGuestCount(property, 5))).isTrue();
        pricingService.validateGuestCount(property, 4); // does not throw
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

    // ------------------------------------------------------------------
    // Dynamic pricing
    // ------------------------------------------------------------------

    @Test
    void quote_dynamicPricingDisabled_isIdenticalToPlainCalculationAndSkipsExtraQueries() {
        when(seasonalRateRepository.findOverlappingRange(any(), any(), any())).thenReturn(List.of());
        DynamicPricingConfig disabledConfig = enabledConfig(5, "0.90", "1.30", 7, "1.20", null, null);
        disabledConfig.setEnabled(false);
        when(dynamicPricingConfigRepository.findByPropertyId(any())).thenReturn(Optional.of(disabledConfig));

        LocalDate checkIn = LocalDate.now().plusDays(90);
        PriceQuoteResponse quote = pricingService.quote(property, checkIn, checkIn.plusDays(3), 1);

        // Same 300.00 (3 nights * 100 base) as the disabled/no-config tests above - proves
        // enabled=false produces byte-identical results, not just "close".
        assertThat(quote.subtotal()).isEqualByComparingTo("300.00");
        assertThat(quote.totalAmount()).isEqualByComparingTo("300.00");
        verifyNoInteractions(reservationRepository, localEventRepository);
    }

    @Test
    void priceBreakdown_zeroCommercialDemand_occupancyFactorEqualsMinimumMultiplier() {
        when(seasonalRateRepository.findOverlappingRange(any(), any(), any())).thenReturn(List.of());
        DynamicPricingConfig config = enabledConfig(3, "0.90", "1.30", 0, "1.00", null, null);
        when(dynamicPricingConfigRepository.findByPropertyId(any())).thenReturn(Optional.of(config));
        when(reservationRepository.findCalendarEntries(any(), any(), any(), any())).thenReturn(List.of());
        when(localEventRepository.findRelevantForPricing(any(), any(), any(), any())).thenReturn(List.of());

        LocalDate checkIn = LocalDate.now().plusDays(90);
        NightlyPriceBreakdown night = onlyNight(pricingService.priceBreakdown(property, checkIn, checkIn.plusDays(1), 1));

        assertThat(night.sellableNights()).isEqualTo(7); // windowDays=3 -> 7 nights, no maintenance
        assertThat(night.commerciallyBookedNights()).isEqualTo(0);
        assertThat(night.occupancyFactor()).isEqualByComparingTo("0.90");
        assertThat(night.rateAfterClamp()).isEqualByComparingTo("90.00");
    }

    @Test
    void priceBreakdown_highCommercialOccupancy_increasesPriceProportionally() {
        when(seasonalRateRepository.findOverlappingRange(any(), any(), any())).thenReturn(List.of());
        DynamicPricingConfig config = enabledConfig(3, "0.90", "1.30", 0, "1.00", null, null);
        when(dynamicPricingConfigRepository.findByPropertyId(any())).thenReturn(Optional.of(config));
        when(localEventRepository.findRelevantForPricing(any(), any(), any(), any())).thenReturn(List.of());

        LocalDate checkIn = LocalDate.now().plusDays(90);
        // window = [checkIn-3, checkIn+3] = 7 nights; 3 of them commercially booked.
        when(reservationRepository.findCalendarEntries(any(), any(), any(), any())).thenReturn(List.of(
                reservation(checkIn.minusDays(3), checkIn.minusDays(2), ReservationSource.DIRECT, ReservationStatus.CONFIRMED),
                reservation(checkIn.minusDays(2), checkIn.minusDays(1), ReservationSource.AIRBNB, ReservationStatus.CONFIRMED),
                reservation(checkIn.minusDays(1), checkIn, ReservationSource.BOOKING_COM, ReservationStatus.CONFIRMED)));

        NightlyPriceBreakdown night = onlyNight(pricingService.priceBreakdown(property, checkIn, checkIn.plusDays(1), 1));

        assertThat(night.sellableNights()).isEqualTo(7);
        assertThat(night.commerciallyBookedNights()).isEqualTo(3);
        // occupancy = 3/7 -> factor = 0.90 + (1.30-0.90)*3/7 = 1.0714
        assertThat(night.occupancyFactor()).isEqualByComparingTo("1.0714");
        assertThat(night.rateAfterClamp()).isEqualByComparingTo("107.14");
    }

    @Test
    void priceBreakdown_maintenanceReservations_areRemovedFromBothNumeratorAndDenominator() {
        when(seasonalRateRepository.findOverlappingRange(any(), any(), any())).thenReturn(List.of());
        DynamicPricingConfig config = enabledConfig(7, "0.90", "1.30", 0, "1.00", null, null);
        when(dynamicPricingConfigRepository.findByPropertyId(any())).thenReturn(Optional.of(config));
        when(localEventRepository.findRelevantForPricing(any(), any(), any(), any())).thenReturn(List.of());

        LocalDate checkIn = LocalDate.now().plusDays(90);
        // window = [checkIn-7, checkIn+7] = 15 nights total.
        // 8 nights (checkIn-7..checkIn) are under maintenance; 3 of the remaining 7 sellable nights are commercially booked.
        when(reservationRepository.findCalendarEntries(any(), any(), any(), any())).thenReturn(List.of(
                reservation(checkIn.minusDays(7), checkIn.plusDays(1), ReservationSource.MAINTENANCE, ReservationStatus.CONFIRMED),
                reservation(checkIn.plusDays(1), checkIn.plusDays(2), ReservationSource.DIRECT, ReservationStatus.CONFIRMED),
                reservation(checkIn.plusDays(2), checkIn.plusDays(3), ReservationSource.AIRBNB, ReservationStatus.CONFIRMED),
                reservation(checkIn.plusDays(3), checkIn.plusDays(4), ReservationSource.BOOKING_COM, ReservationStatus.CONFIRMED)));

        NightlyPriceBreakdown night = onlyNight(pricingService.priceBreakdown(property, checkIn, checkIn.plusDays(1), 1));

        assertThat(night.sellableNights()).isEqualTo(7); // 15 total - 8 maintenance, NOT 15
        assertThat(night.commerciallyBookedNights()).isEqualTo(3);
        // Same 3/7 ratio as the highOccupancy test above - proves maintenance nights never dilute the denominator.
        assertThat(night.occupancyFactor()).isEqualByComparingTo("1.0714");
    }

    @Test
    void priceBreakdown_pendingReservations_doNotCountAsCommercialDemand() {
        when(seasonalRateRepository.findOverlappingRange(any(), any(), any())).thenReturn(List.of());
        DynamicPricingConfig config = enabledConfig(3, "0.90", "1.30", 0, "1.00", null, null);
        when(dynamicPricingConfigRepository.findByPropertyId(any())).thenReturn(Optional.of(config));
        when(localEventRepository.findRelevantForPricing(any(), any(), any(), any())).thenReturn(List.of());

        LocalDate checkIn = LocalDate.now().plusDays(90);
        when(reservationRepository.findCalendarEntries(any(), any(), any(), any())).thenReturn(List.of(
                reservation(checkIn.minusDays(3), checkIn.minusDays(2), ReservationSource.DIRECT, ReservationStatus.PENDING),
                reservation(checkIn.minusDays(2), checkIn.minusDays(1), ReservationSource.DIRECT, ReservationStatus.PENDING),
                reservation(checkIn.minusDays(1), checkIn, ReservationSource.OTHER, ReservationStatus.CONFIRMED)));

        NightlyPriceBreakdown night = onlyNight(pricingService.priceBreakdown(property, checkIn, checkIn.plusDays(1), 1));

        // Unconfirmed holds (and OTHER-source rows) are not real commercial demand - a visitor
        // starting and abandoning checkouts must not be able to push the price up for everyone else.
        assertThat(night.commerciallyBookedNights()).isEqualTo(0);
        assertThat(night.occupancyFactor()).isEqualByComparingTo("0.90");
    }

    @Test
    void priceBreakdown_otaConfirmedReservations_countAsCommercialDemand() {
        when(seasonalRateRepository.findOverlappingRange(any(), any(), any())).thenReturn(List.of());
        DynamicPricingConfig config = enabledConfig(3, "0.90", "1.30", 0, "1.00", null, null);
        when(dynamicPricingConfigRepository.findByPropertyId(any())).thenReturn(Optional.of(config));
        when(localEventRepository.findRelevantForPricing(any(), any(), any(), any())).thenReturn(List.of());

        LocalDate checkIn = LocalDate.now().plusDays(90);
        when(reservationRepository.findCalendarEntries(any(), any(), any(), any())).thenReturn(List.of(
                reservation(checkIn.minusDays(1), checkIn, ReservationSource.AIRBNB, ReservationStatus.CONFIRMED)));

        NightlyPriceBreakdown night = onlyNight(pricingService.priceBreakdown(property, checkIn, checkIn.plusDays(1), 1));

        assertThat(night.commerciallyBookedNights()).isEqualTo(1);
    }

    @Test
    void priceBreakdown_windowFullyUnderMaintenance_occupancyFactorIsExactlyOneNotMinimum() {
        when(seasonalRateRepository.findOverlappingRange(any(), any(), any())).thenReturn(List.of());
        DynamicPricingConfig config = enabledConfig(1, "0.50", "1.50", 0, "1.00", null, null);
        when(dynamicPricingConfigRepository.findByPropertyId(any())).thenReturn(Optional.of(config));
        when(localEventRepository.findRelevantForPricing(any(), any(), any(), any())).thenReturn(List.of());

        LocalDate checkIn = LocalDate.now().plusDays(90);
        // windowDays=1 -> window = [checkIn-1, checkIn+1] = 3 nights, entirely under maintenance.
        when(reservationRepository.findCalendarEntries(any(), any(), any(), any())).thenReturn(List.of(
                reservation(checkIn.minusDays(1), checkIn.plusDays(2), ReservationSource.MAINTENANCE, ReservationStatus.CONFIRMED)));

        NightlyPriceBreakdown night = onlyNight(pricingService.priceBreakdown(property, checkIn, checkIn.plusDays(1), 1));

        assertThat(night.sellableNights()).isEqualTo(0);
        assertThat(night.occupancyFactor()).isEqualByComparingTo("1.00");
        assertThat(night.rateAfterClamp()).isEqualByComparingTo("100.00"); // neutral, not pushed to the 0.50 minimum
    }

    @Test
    void priceBreakdown_withinLeadTimeWindow_appliesLeadTimeMultiplier() {
        when(seasonalRateRepository.findOverlappingRange(any(), any(), any())).thenReturn(List.of());
        DynamicPricingConfig config = enabledConfig(3, "1.00", "1.00", 10, "1.25", null, null);
        when(dynamicPricingConfigRepository.findByPropertyId(any())).thenReturn(Optional.of(config));
        when(reservationRepository.findCalendarEntries(any(), any(), any(), any())).thenReturn(List.of());
        when(localEventRepository.findRelevantForPricing(any(), any(), any(), any())).thenReturn(List.of());

        LocalDate checkIn = LocalDate.now().plusDays(5); // within the 10-day lead time window
        NightlyPriceBreakdown night = onlyNight(pricingService.priceBreakdown(property, checkIn, checkIn.plusDays(1), 1));

        assertThat(night.leadTimeFactor()).isEqualByComparingTo("1.25");
        assertThat(night.rateAfterClamp()).isEqualByComparingTo("125.00");
    }

    @Test
    void priceBreakdown_outsideLeadTimeWindow_leadTimeFactorIsNeutral() {
        when(seasonalRateRepository.findOverlappingRange(any(), any(), any())).thenReturn(List.of());
        DynamicPricingConfig config = enabledConfig(3, "1.00", "1.00", 10, "1.25", null, null);
        when(dynamicPricingConfigRepository.findByPropertyId(any())).thenReturn(Optional.of(config));
        when(reservationRepository.findCalendarEntries(any(), any(), any(), any())).thenReturn(List.of());
        when(localEventRepository.findRelevantForPricing(any(), any(), any(), any())).thenReturn(List.of());

        LocalDate checkIn = LocalDate.now().plusDays(90); // well outside the 10-day lead time window
        NightlyPriceBreakdown night = onlyNight(pricingService.priceBreakdown(property, checkIn, checkIn.plusDays(1), 1));

        assertThat(night.leadTimeFactor()).isEqualByComparingTo("1.00");
        assertThat(night.rateAfterClamp()).isEqualByComparingTo("100.00");
    }

    @Test
    void priceBreakdown_twoOverlappingEvents_theHigherMultiplierWins() {
        when(seasonalRateRepository.findOverlappingRange(any(), any(), any())).thenReturn(List.of());
        DynamicPricingConfig config = enabledConfig(3, "1.00", "1.00", 0, "1.00", null, null);
        when(dynamicPricingConfigRepository.findByPropertyId(any())).thenReturn(Optional.of(config));
        when(reservationRepository.findCalendarEntries(any(), any(), any(), any())).thenReturn(List.of());

        LocalDate checkIn = LocalDate.now().plusDays(90);
        LocalEvent smallEvent = LocalEvent.builder()
                .city("Brasov").label("Local fair")
                .startDate(checkIn.minusDays(1)).endDate(checkIn.plusDays(1))
                .priceMultiplier(new BigDecimal("1.20")).build();
        LocalEvent bigEvent = LocalEvent.builder()
                .city("Brasov").label("Music festival")
                .startDate(checkIn).endDate(checkIn)
                .priceMultiplier(new BigDecimal("2.00")).build();
        when(localEventRepository.findRelevantForPricing(any(), any(), any(), any()))
                .thenReturn(List.of(smallEvent, bigEvent));

        NightlyPriceBreakdown night = onlyNight(pricingService.priceBreakdown(property, checkIn, checkIn.plusDays(1), 1));

        assertThat(night.eventFactor()).isEqualByComparingTo("2.00");
        assertThat(night.rateAfterClamp()).isEqualByComparingTo("200.00");
    }

    @Test
    void priceBreakdown_noOverlappingEvent_eventFactorIsNeutral() {
        when(seasonalRateRepository.findOverlappingRange(any(), any(), any())).thenReturn(List.of());
        DynamicPricingConfig config = enabledConfig(3, "1.00", "1.00", 0, "1.00", null, null);
        when(dynamicPricingConfigRepository.findByPropertyId(any())).thenReturn(Optional.of(config));
        when(reservationRepository.findCalendarEntries(any(), any(), any(), any())).thenReturn(List.of());
        when(localEventRepository.findRelevantForPricing(any(), any(), any(), any())).thenReturn(List.of());

        LocalDate checkIn = LocalDate.now().plusDays(90);
        NightlyPriceBreakdown night = onlyNight(pricingService.priceBreakdown(property, checkIn, checkIn.plusDays(1), 1));

        assertThat(night.eventFactor()).isEqualByComparingTo("1.00");
    }

    @Test
    void priceBreakdown_resultAboveMaxPrice_isClampedDown() {
        when(seasonalRateRepository.findOverlappingRange(any(), any(), any())).thenReturn(List.of());
        DynamicPricingConfig config = enabledConfig(3, "1.00", "1.00", 0, "1.00", null, "130.00");
        when(dynamicPricingConfigRepository.findByPropertyId(any())).thenReturn(Optional.of(config));
        when(reservationRepository.findCalendarEntries(any(), any(), any(), any())).thenReturn(List.of());

        LocalDate checkIn = LocalDate.now().plusDays(90);
        LocalEvent bigEvent = LocalEvent.builder()
                .city("Brasov").label("Festival")
                .startDate(checkIn).endDate(checkIn)
                .priceMultiplier(new BigDecimal("2.00")).build();
        when(localEventRepository.findRelevantForPricing(any(), any(), any(), any())).thenReturn(List.of(bigEvent));

        NightlyPriceBreakdown night = onlyNight(pricingService.priceBreakdown(property, checkIn, checkIn.plusDays(1), 1));

        assertThat(night.rateBeforeClamp()).isEqualByComparingTo("200.00");
        assertThat(night.rateAfterClamp()).isEqualByComparingTo("130.00");
    }

    @Test
    void priceBreakdown_resultBelowMinPrice_isClampedUp() {
        when(seasonalRateRepository.findOverlappingRange(any(), any(), any())).thenReturn(List.of());
        DynamicPricingConfig config = enabledConfig(3, "0.50", "0.50", 0, "1.00", "80.00", null);
        when(dynamicPricingConfigRepository.findByPropertyId(any())).thenReturn(Optional.of(config));
        when(reservationRepository.findCalendarEntries(any(), any(), any(), any())).thenReturn(List.of());
        when(localEventRepository.findRelevantForPricing(any(), any(), any(), any())).thenReturn(List.of());

        LocalDate checkIn = LocalDate.now().plusDays(90);
        NightlyPriceBreakdown night = onlyNight(pricingService.priceBreakdown(property, checkIn, checkIn.plusDays(1), 1));

        assertThat(night.rateBeforeClamp()).isEqualByComparingTo("50.00");
        assertThat(night.rateAfterClamp()).isEqualByComparingTo("80.00");
    }

    @Test
    void priceBreakdown_unavailableWhenNoPriceConfigured() {
        when(seasonalRateRepository.findOverlappingRange(any(), any(), any())).thenReturn(List.of());
        Property noPriceProperty = Property.builder().name("No price").build();
        noPriceProperty.setId(UUID.randomUUID());
        when(dynamicPricingConfigRepository.findByPropertyId(any())).thenReturn(Optional.empty());

        DynamicPriceBreakdownResponse breakdown = pricingService.priceBreakdown(
                noPriceProperty, LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 4), 1);

        assertThat(breakdown.available()).isFalse();
        assertThat(breakdown.unavailableReason()).isNotBlank();
    }

    private NightlyPriceBreakdown onlyNight(DynamicPriceBreakdownResponse breakdown) {
        assertThat(breakdown.available()).isTrue();
        assertThat(breakdown.nightlyBreakdown()).hasSize(1);
        return breakdown.nightlyBreakdown().get(0);
    }

    private DynamicPricingConfig enabledConfig(int occupancyWindowDays, String occupancyMultiplierMin,
                                                String occupancyMultiplierMax, int leadTimeDays,
                                                String leadTimeMultiplier, String minPrice, String maxPrice) {
        return DynamicPricingConfig.builder()
                .enabled(true)
                .occupancyWindowDays(occupancyWindowDays)
                .occupancyMultiplierMin(new BigDecimal(occupancyMultiplierMin))
                .occupancyMultiplierMax(new BigDecimal(occupancyMultiplierMax))
                .leadTimeDays(leadTimeDays)
                .leadTimeMultiplier(new BigDecimal(leadTimeMultiplier))
                .minPrice(minPrice == null ? null : new BigDecimal(minPrice))
                .maxPrice(maxPrice == null ? null : new BigDecimal(maxPrice))
                .build();
    }

    private Reservation reservation(LocalDate checkIn, LocalDate checkOut, ReservationSource source,
                                     ReservationStatus status) {
        return Reservation.builder()
                .property(property)
                .guestFirstName("Test")
                .guestLastName("Guest")
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .source(source)
                .status(status)
                .build();
    }
}
