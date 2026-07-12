package com.bhgroup.pms.service.mapper;

import com.bhgroup.pms.domain.CleaningTask;
import com.bhgroup.pms.domain.CleaningTaskPhoto;
import com.bhgroup.pms.dto.cleaning.CleaningTaskPhotoResponse;
import com.bhgroup.pms.dto.cleaning.CleaningTaskResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CleaningTaskMapper {

    public CleaningTaskResponse toResponse(CleaningTask task, List<CleaningTaskPhoto> photos) {
        return new CleaningTaskResponse(
                task.getId(),
                task.getProperty().getId(),
                task.getProperty().getName(),
                task.getReservation() != null ? task.getReservation().getId() : null,
                task.getStatus(),
                task.getAssignedCleaner() != null ? task.getAssignedCleaner().getId() : null,
                task.getAssignedCleaner() != null
                        ? task.getAssignedCleaner().getFirstName() + " " + task.getAssignedCleaner().getLastName()
                        : null,
                task.getScheduledDate(),
                task.getNotes(),
                task.getCost(),
                task.getEstimatedMinutes(),
                task.getActualMinutes(),
                task.getStartedAt(),
                task.getCompletedAt(),
                task.getChecklistResults(),
                photos.stream().map(this::toPhotoResponse).toList(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }

    public CleaningTaskPhotoResponse toPhotoResponse(CleaningTaskPhoto photo) {
        return new CleaningTaskPhotoResponse(photo.getId(), photo.getUrl(), photo.getCaption(), photo.getCreatedAt());
    }
}
