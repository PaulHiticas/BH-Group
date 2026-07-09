package com.bhgroup.pms.lead;

import com.bhgroup.pms.common.exception.ResourceNotFoundException;
import com.bhgroup.pms.common.response.PageResponse;
import com.bhgroup.pms.lead.dto.LeadCreateRequest;
import com.bhgroup.pms.lead.dto.LeadResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeadService {

    private final PropertyLeadRepository leadRepository;
    private final LeadMapper leadMapper;

    @Transactional
    public LeadResponse create(LeadCreateRequest request) {
        PropertyLead lead = PropertyLead.builder()
                .fullName(request.fullName())
                .email(request.email())
                .phone(request.phone())
                .city(request.city())
                .message(request.message())
                .build();

        lead = leadRepository.save(lead);
        return leadMapper.toResponse(lead);
    }

    @Transactional(readOnly = true)
    public PageResponse<LeadResponse> list(Pageable pageable) {
        return PageResponse.of(leadRepository.findAllByOrderByCreatedAtDesc(pageable), leadMapper::toResponse);
    }

    @Transactional
    public LeadResponse markContacted(UUID id, boolean contacted) {
        PropertyLead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found"));
        lead.setContacted(contacted);
        lead = leadRepository.save(lead);
        return leadMapper.toResponse(lead);
    }

    @Transactional(readOnly = true)
    public long countUncontacted() {
        return leadRepository.countByContactedFalse();
    }
}
