package com.bhgroup.pms.service.mapper;

import com.bhgroup.pms.dto.lead.LeadResponse;
import org.springframework.stereotype.Component;

import com.bhgroup.pms.domain.PropertyLead;
import com.bhgroup.pms.dto.lead.LeadResponse;
@Component
public class LeadMapper {

    public LeadResponse toResponse(PropertyLead lead) {
        return new LeadResponse(
                lead.getId(),
                lead.getFullName(),
                lead.getEmail(),
                lead.getPhone(),
                lead.getCity(),
                lead.getMessage(),
                lead.isContacted(),
                lead.getCreatedAt()
        );
    }
}
