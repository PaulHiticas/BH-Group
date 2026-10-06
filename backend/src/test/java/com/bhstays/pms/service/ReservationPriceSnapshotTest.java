package com.bhstays.pms.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bhstays.pms.domain.Reservation;
import com.bhstays.pms.dto.property.PriceQuoteResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ReservationPriceSnapshotTest {

    private static PriceQuoteResponse quote(String total, String cleaning, String extraGuest) {
        return new PriceQuoteResponse(true, null, LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 4), 3,
                new BigDecimal("400.00"), new BigDecimal(extraGuest), new BigDecimal(cleaning),
                null, BigDecimal.ZERO, new BigDecimal(total), "RON", null, null);
    }

    private static Reservation reservation(String total, String currency) {
        return Reservation.builder()
                .totalAmount(total != null ? new BigDecimal(total) : null)
                .currency(currency)
                .accommodationAmount(new BigDecimal("1.00"))
                .cleaningFeeAmount(new BigDecimal("1.00"))
                .extraGuestFeeAmount(new BigDecimal("1.00"))
                .build();
    }

    @Test
    void matchingQuote_storesABreakdownThatAddsUpToTheTotal() {
        Reservation reservation = reservation("550.00", "RON");

        ReservationPriceSnapshot.apply(reservation, quote("550.00", "100.00", "50.00"));

        assertThat(reservation.getAccommodationAmount()).isEqualByComparingTo("400.00");
        assertThat(reservation.getCleaningFeeAmount()).isEqualByComparingTo("100.00");
        assertThat(reservation.getExtraGuestFeeAmount()).isEqualByComparingTo("50.00");
        assertThat(reservation.getAccommodationAmount()
                .add(reservation.getCleaningFeeAmount())
                .add(reservation.getExtraGuestFeeAmount()))
                .isEqualByComparingTo(reservation.getTotalAmount());
    }

    @Test
    void totalThatDiffersFromTheQuote_clearsTheBreakdown() {
        Reservation reservation = reservation("480.00", "RON");

        ReservationPriceSnapshot.apply(reservation, quote("550.00", "100.00", "50.00"));

        assertThat(reservation.getAccommodationAmount()).isNull();
        assertThat(reservation.getCleaningFeeAmount()).isNull();
        assertThat(reservation.getExtraGuestFeeAmount()).isNull();
    }

    @Test
    void otherCurrency_clearsTheBreakdown() {
        Reservation reservation = reservation("550.00", "EUR");

        ReservationPriceSnapshot.apply(reservation, quote("550.00", "100.00", "50.00"));

        assertThat(reservation.getAccommodationAmount()).isNull();
    }

    @Test
    void missingOrUnavailableQuote_clearsTheBreakdown() {
        Reservation noQuote = reservation("550.00", "RON");
        ReservationPriceSnapshot.apply(noQuote, null);
        assertThat(noQuote.getAccommodationAmount()).isNull();

        Reservation unavailable = reservation(null, "RON");
        ReservationPriceSnapshot.apply(unavailable, new PriceQuoteResponse(false, "No price", null, null, 0,
                null, null, null, null, null, null, "RON", null, null));
        assertThat(unavailable.getAccommodationAmount()).isNull();
        assertThat(unavailable.getCleaningFeeAmount()).isNull();
    }
}
