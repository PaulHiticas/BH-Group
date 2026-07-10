package com.bhgroup.pms.service;

import com.bhgroup.pms.common.exception.ResourceNotFoundException;
import com.bhgroup.pms.common.response.PageResponse;
import com.bhgroup.pms.dto.lead.LeadCreateRequest;
import com.bhgroup.pms.dto.lead.LeadResponse;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bhgroup.pms.domain.PropertyLead;
import com.bhgroup.pms.dto.lead.LeadCreateRequest;
import com.bhgroup.pms.dto.lead.LeadResponse;
import com.bhgroup.pms.repository.PropertyLeadRepository;
import com.bhgroup.pms.service.mapper.LeadMapper;
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

    @Transactional(readOnly = true)
    public List<List<String>> exportRows() {
        return leadRepository.findAll(Sort.by("createdAt").descending())
                .stream()
                .map(lead -> List.of(
                        lead.getFullName(),
                        lead.getEmail(),
                        lead.getPhone() != null ? lead.getPhone() : "",
                        lead.getCity() != null ? lead.getCity() : "",
                        lead.getMessage() != null ? lead.getMessage() : "",
                        lead.isContacted() ? "Da" : "Nu",
                        lead.getCreatedAt().toString()
                ))
                .toList();
    }
}
