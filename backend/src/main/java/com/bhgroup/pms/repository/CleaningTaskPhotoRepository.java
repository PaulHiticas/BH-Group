package com.bhgroup.pms.repository;

import com.bhgroup.pms.domain.CleaningTaskPhoto;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CleaningTaskPhotoRepository extends JpaRepository<CleaningTaskPhoto, UUID> {

    List<CleaningTaskPhoto> findByCleaningTaskIdOrderByCreatedAtAsc(UUID cleaningTaskId);
}
