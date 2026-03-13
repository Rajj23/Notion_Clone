package com.blockverse.app.repo;

import com.blockverse.app.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AuditLogRepo extends JpaRepository<AuditLog, Integer> {
    @Query("""
    SELECT a
    FROM AuditLog a
    WHERE a.workSpaceId = :workspaceId
    ORDER BY a.createdAt DESC
    """)
    Page<AuditLog> findByWorkSpace_IdOrderByCreatedAtDesc(int workspaceId, Pageable pageable);
}
