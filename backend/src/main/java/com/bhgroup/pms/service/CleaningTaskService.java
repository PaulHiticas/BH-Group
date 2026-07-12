package com.bhgroup.pms.service;

import com.bhgroup.pms.common.exception.BadRequestException;
import com.bhgroup.pms.common.exception.ResourceNotFoundException;
import com.bhgroup.pms.common.response.PageResponse;
import com.bhgroup.pms.domain.CleaningTask;
import com.bhgroup.pms.domain.CleaningTaskPhoto;
import com.bhgroup.pms.domain.CleaningTaskStatus;
import com.bhgroup.pms.domain.Property;
import com.bhgroup.pms.domain.Reservation;
import com.bhgroup.pms.domain.Role;
import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.dto.cleaning.ChecklistItemUpdateRequest;
import com.bhgroup.pms.dto.cleaning.CleaningTaskAssignRequest;
import com.bhgroup.pms.dto.cleaning.CleaningTaskCreateRequest;
import com.bhgroup.pms.dto.cleaning.CleaningTaskResponse;
import com.bhgroup.pms.dto.cleaning.CleaningTaskStatusUpdateRequest;
import com.bhgroup.pms.dto.cleaning.CleaningTaskUpdateRequest;
import com.bhgroup.pms.repository.CleaningTaskPhotoRepository;
import com.bhgroup.pms.repository.CleaningTaskRepository;
import com.bhgroup.pms.repository.CleaningTaskSpecifications;
import com.bhgroup.pms.repository.PropertyRepository;
import com.bhgroup.pms.repository.UserRepository;
import com.bhgroup.pms.service.mapper.CleaningTaskMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CleaningTaskService {

    private static final Map<CleaningTaskStatus, Set<CleaningTaskStatus>> ALLOWED_TRANSITIONS = Map.of(
            CleaningTaskStatus.NEW, Set.of(CleaningTaskStatus.ACCEPTED, CleaningTaskStatus.REJECTED),
            CleaningTaskStatus.ACCEPTED, Set.of(CleaningTaskStatus.IN_PROGRESS, CleaningTaskStatus.REJECTED),
            CleaningTaskStatus.IN_PROGRESS, Set.of(CleaningTaskStatus.DONE),
            CleaningTaskStatus.DONE, Set.of(),
            CleaningTaskStatus.REJECTED, Set.of()
    );

    private final CleaningTaskRepository cleaningTaskRepository;
    private final CleaningTaskPhotoRepository cleaningTaskPhotoRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final CleaningTaskMapper cleaningTaskMapper;

    @Transactional
    public CleaningTask autoCreateFromCheckout(Reservation reservation) {
        Property property = reservation.getProperty();
        CleaningTask task = CleaningTask.builder()
                .property(property)
                .reservation(reservation)
                .status(CleaningTaskStatus.NEW)
                .scheduledDate(reservation.getCheckOutDate())
                .checklistResults(snapshotChecklist(property))
                .build();
        return cleaningTaskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public PageResponse<CleaningTaskResponse> list(UUID propertyId, CleaningTaskStatus status, UUID cleanerId,
                                                     Pageable pageable) {
        Specification<CleaningTask> spec = CleaningTaskSpecifications.combine(
                CleaningTaskSpecifications.hasProperty(propertyId),
                CleaningTaskSpecifications.hasStatus(status),
                CleaningTaskSpecifications.hasCleaner(cleanerId)
        );
        Page<CleaningTask> page = cleaningTaskRepository.findAll(spec, pageable);
        return PageResponse.of(page, this::toResponse);
    }

    @Transactional(readOnly = true)
    public PageResponse<CleaningTaskResponse> listMine(UUID cleanerId, CleaningTaskStatus status, Pageable pageable) {
        return list(null, status, cleanerId, pageable);
    }

    @Transactional(readOnly = true)
    public CleaningTaskResponse get(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public CleaningTaskResponse getMine(UUID cleanerId, UUID id) {
        CleaningTask task = findOrThrow(id);
        assertCleanerOwns(task, cleanerId);
        return toResponse(task);
    }

    @Transactional
    public CleaningTaskResponse create(CleaningTaskCreateRequest request) {
        Property property = propertyRepository.findById(request.propertyId())
                .orElseThrow(() -> new ResourceNotFoundException("Property not found"));

        CleaningTask task = CleaningTask.builder()
                .property(property)
                .status(CleaningTaskStatus.NEW)
                .scheduledDate(request.scheduledDate())
                .notes(request.notes())
                .checklistResults(snapshotChecklist(property))
                .build();

        if (request.assignedCleanerId() != null) {
            task.setAssignedCleaner(resolveCleaner(request.assignedCleanerId()));
        }

        task = cleaningTaskRepository.save(task);
        return toResponse(task);
    }

    @Transactional
    public CleaningTaskResponse update(UUID id, CleaningTaskUpdateRequest request) {
        CleaningTask task = findOrThrow(id);
        if (request.scheduledDate() != null) task.setScheduledDate(request.scheduledDate());
        task.setNotes(request.notes());
        task.setCost(request.cost());
        task.setEstimatedMinutes(request.estimatedMinutes());
        task = cleaningTaskRepository.save(task);
        return toResponse(task);
    }

    @Transactional
    public CleaningTaskResponse assign(UUID id, CleaningTaskAssignRequest request) {
        CleaningTask task = findOrThrow(id);
        task.setAssignedCleaner(resolveCleaner(request.cleanerId()));
        task.setStatus(CleaningTaskStatus.NEW);
        task.setStartedAt(null);
        task.setCompletedAt(null);
        task = cleaningTaskRepository.save(task);
        return toResponse(task);
    }

    @Transactional
    public CleaningTaskResponse updateStatus(UUID id, CleaningTaskStatusUpdateRequest request) {
        return toResponse(applyStatusTransition(findOrThrow(id), request.status()));
    }

    @Transactional
    public CleaningTaskResponse updateMyStatus(UUID cleanerId, UUID id, CleaningTaskStatusUpdateRequest request) {
        CleaningTask task = findOrThrow(id);
        assertCleanerOwns(task, cleanerId);
        return toResponse(applyStatusTransition(task, request.status()));
    }

    @Transactional
    public CleaningTaskResponse updateChecklistItem(UUID id, ChecklistItemUpdateRequest request) {
        CleaningTask task = findOrThrow(id);
        applyChecklistUpdate(task, request);
        return toResponse(cleaningTaskRepository.save(task));
    }

    @Transactional
    public CleaningTaskResponse updateMyChecklistItem(UUID cleanerId, UUID id, ChecklistItemUpdateRequest request) {
        CleaningTask task = findOrThrow(id);
        assertCleanerOwns(task, cleanerId);
        applyChecklistUpdate(task, request);
        return toResponse(cleaningTaskRepository.save(task));
    }

    @Transactional
    public CleaningTaskResponse addPhoto(UUID id, MultipartFile file, String caption) {
        CleaningTask task = findOrThrow(id);
        return addPhotoInternal(task, file, caption);
    }

    @Transactional
    public CleaningTaskResponse addMyPhoto(UUID cleanerId, UUID id, MultipartFile file, String caption) {
        CleaningTask task = findOrThrow(id);
        assertCleanerOwns(task, cleanerId);
        return addPhotoInternal(task, file, caption);
    }

    private CleaningTaskResponse addPhotoInternal(CleaningTask task, MultipartFile file, String caption) {
        FileStorageService.StoredFile stored = fileStorageService.storeImage(
                file, "cleaning-tasks/" + task.getId());
        CleaningTaskPhoto photo = CleaningTaskPhoto.builder()
                .cleaningTask(task)
                .fileKey(stored.fileKey())
                .url(stored.url())
                .caption(caption)
                .build();
        cleaningTaskPhotoRepository.save(photo);
        return toResponse(task);
    }

    private CleaningTask applyStatusTransition(CleaningTask task, CleaningTaskStatus target) {
        CleaningTaskStatus current = task.getStatus();
        if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
            throw new BadRequestException("Cannot transition cleaning task from " + current + " to " + target);
        }
        task.setStatus(target);
        if (target == CleaningTaskStatus.IN_PROGRESS) {
            task.setStartedAt(Instant.now());
        } else if (target == CleaningTaskStatus.DONE) {
            task.setCompletedAt(Instant.now());
            if (task.getStartedAt() != null) {
                task.setActualMinutes((int) java.time.Duration.between(task.getStartedAt(), task.getCompletedAt()).toMinutes());
            }
        }
        return cleaningTaskRepository.save(task);
    }

    private void applyChecklistUpdate(CleaningTask task, ChecklistItemUpdateRequest request) {
        if (!task.getChecklistResults().containsKey(request.label())) {
            throw new BadRequestException("This item is not part of the task's checklist");
        }
        task.getChecklistResults().put(request.label(), request.checked());
    }

    private Map<String, Boolean> snapshotChecklist(Property property) {
        Map<String, Boolean> results = new LinkedHashMap<>();
        for (String item : property.getCleaningChecklist()) {
            results.put(item, false);
        }
        return results;
    }

    private User resolveCleaner(UUID cleanerId) {
        User cleaner = userRepository.findById(cleanerId)
                .orElseThrow(() -> new BadRequestException("Cleaner not found"));
        if (cleaner.getRole() != Role.CLEANER) {
            throw new BadRequestException("Assigned user must have the CLEANER role");
        }
        return cleaner;
    }

    private void assertCleanerOwns(CleaningTask task, UUID cleanerId) {
        // 404 rather than 403: a cleaner should not learn that a task exists
        // when it isn't assigned to them.
        if (task.getAssignedCleaner() == null || !task.getAssignedCleaner().getId().equals(cleanerId)) {
            throw new ResourceNotFoundException("Cleaning task not found");
        }
    }

    private CleaningTaskResponse toResponse(CleaningTask task) {
        var photos = cleaningTaskPhotoRepository.findByCleaningTaskIdOrderByCreatedAtAsc(task.getId());
        return cleaningTaskMapper.toResponse(task, photos);
    }

    private CleaningTask findOrThrow(UUID id) {
        return cleaningTaskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cleaning task not found"));
    }
}
