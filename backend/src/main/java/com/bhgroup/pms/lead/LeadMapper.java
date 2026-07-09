package com.bhgroup.pms.lead;

import com.bhgroup.pms.lead.dto.LeadResponse;
import org.springframework.stereotype.Component;

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
