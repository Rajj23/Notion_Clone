package com.blockverse.app.repo;

import com.blockverse.app.entity.WorkSpace;
import lombok.Builder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkSpaceRepo extends JpaRepository<WorkSpace, Integer> {

    Optional<WorkSpace> findByIdAndDeletedAtIsNull(int workspaceId);
}
