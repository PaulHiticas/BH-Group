package com.bhgroup.pms.repository;

import com.bhgroup.pms.domain.OwnerThread;
import com.bhgroup.pms.domain.OwnerThreadStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnerThreadRepository extends JpaRepository<OwnerThread, UUID> {

    Page<OwnerThread> findByOwnerId(UUID ownerId, Pageable pageable);

    Page<OwnerThread> findByStatus(OwnerThreadStatus status, Pageable pageable);
}
