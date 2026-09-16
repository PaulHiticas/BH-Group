package com.bhstays.pms.repository;

import com.bhstays.pms.domain.CleaningTask;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CleaningTaskRepository extends JpaRepository<CleaningTask, UUID>,
        JpaSpecificationExecutor<CleaningTask> {
}
