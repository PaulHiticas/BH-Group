package com.bhstays.pms.service.mapper;

import com.bhstays.pms.domain.SeasonalRate;
import com.bhstays.pms.dto.property.SeasonalRateResponse;
import org.springframework.stereotype.Component;

@Component
public class SeasonalRateMapper {

    public SeasonalRateResponse toResponse(SeasonalRate rate) {
        return new SeasonalRateResponse(
                rate.getId(), rate.getLabel(), rate.getStartDate(), rate.getEndDate(), rate.getPricePerNight());
    }
}
