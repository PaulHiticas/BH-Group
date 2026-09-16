package com.bhstays.pms.repository;

import com.bhstays.pms.domain.CleaningTaskPhoto;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CleaningTaskPhotoRepository extends JpaRepository<CleaningTaskPhoto, UUID> {

    List<CleaningTaskPhoto> findByCleaningTaskIdOrderByCreatedAtAsc(UUID cleaningTaskId);
}
