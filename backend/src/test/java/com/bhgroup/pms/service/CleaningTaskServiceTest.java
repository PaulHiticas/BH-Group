package com.bhgroup.pms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bhgroup.pms.common.exception.ResourceNotFoundException;
import com.bhgroup.pms.domain.CleaningTask;
import com.bhgroup.pms.domain.CleaningTaskPhoto;
import com.bhgroup.pms.domain.User;
import com.bhgroup.pms.repository.CleaningTaskPhotoRepository;
import com.bhgroup.pms.repository.CleaningTaskRepository;
import com.bhgroup.pms.repository.PropertyRepository;
import com.bhgroup.pms.repository.UserRepository;
import com.bhgroup.pms.service.mapper.CleaningTaskMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;

/**
 * Covers the authenticated-download path for cleaning task photos: photos
 * are no longer served as public static files (see SecurityConfig), so
 * loadPhotoResource/loadMyPhotoResource are the only way to read one, and
 * they must enforce the same ownership isolation as the rest of the
 * cleaner-facing API - a cleaner who isn't assigned to the task gets a 404,
 * never the photo.
 */
@ExtendWith(MockitoExtension.class)
class CleaningTaskServiceTest {

    @Mock
    private CleaningTaskRepository cleaningTaskRepository;
    @Mock
    private CleaningTaskPhotoRepository cleaningTaskPhotoRepository;
    @Mock
    private PropertyRepository propertyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private CleaningTaskMapper cleaningTaskMapper;
    @Mock
    private Resource resource;

    private CleaningTaskService cleaningTaskService;

    private UUID taskId;
    private UUID photoId;
    private UUID cleanerId;
    private CleaningTask task;
    private CleaningTaskPhoto photo;

    @BeforeEach
    void setUp() {
        cleaningTaskService = new CleaningTaskService(
                cleaningTaskRepository, cleaningTaskPhotoRepository, propertyRepository,
                userRepository, fileStorageService, cleaningTaskMapper);

        taskId = UUID.randomUUID();
        photoId = UUID.randomUUID();
        cleanerId = UUID.randomUUID();

        User cleaner = User.builder().build();
        cleaner.setId(cleanerId);
        task = CleaningTask.builder().build();
        task.setId(taskId);
        task.setAssignedCleaner(cleaner);

        photo = CleaningTaskPhoto.builder()
                .id(photoId)
                .cleaningTask(task)
                .fileKey("cleaning-tasks/" + taskId + "/photo.jpg")
                .url("http://example.com/photo.jpg")
                .build();
    }

    @Test
    void loadPhotoResource_returnsResource_whenPhotoBelongsToTask() {
        when(cleaningTaskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(cleaningTaskPhotoRepository.findById(photoId)).thenReturn(Optional.of(photo));
        when(fileStorageService.loadAsResource(photo.getFileKey())).thenReturn(resource);

        Resource result = cleaningTaskService.loadPhotoResource(taskId, photoId);

        assertThat(result).isSameAs(resource);
    }

    @Test
    void loadPhotoResource_throwsNotFound_whenPhotoBelongsToADifferentTask() {
        CleaningTask otherTask = CleaningTask.builder().build();
        otherTask.setId(UUID.randomUUID());
        photo.setCleaningTask(otherTask);

        when(cleaningTaskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(cleaningTaskPhotoRepository.findById(photoId)).thenReturn(Optional.of(photo));

        assertThatThrownBy(() -> cleaningTaskService.loadPhotoResource(taskId, photoId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void loadMyPhotoResource_returnsResource_whenCallerIsTheAssignedCleaner() {
        when(cleaningTaskRepository.findById(taskId)).thenReturn(Optional.of(task));
        when(cleaningTaskPhotoRepository.findById(photoId)).thenReturn(Optional.of(photo));
        when(fileStorageService.loadAsResource(photo.getFileKey())).thenReturn(resource);

        Resource result = cleaningTaskService.loadMyPhotoResource(cleanerId, taskId, photoId);

        assertThat(result).isSameAs(resource);
    }

    @Test
    void loadMyPhotoResource_throwsNotFound_whenCallerIsNotTheAssignedCleaner() {
        UUID someoneElseId = UUID.randomUUID();
        when(cleaningTaskRepository.findById(taskId)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> cleaningTaskService.loadMyPhotoResource(someoneElseId, taskId, photoId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void loadMyPhotoResource_throwsNotFound_whenTaskHasNoAssignedCleaner() {
        task.setAssignedCleaner(null);
        when(cleaningTaskRepository.findById(taskId)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> cleaningTaskService.loadMyPhotoResource(cleanerId, taskId, photoId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
