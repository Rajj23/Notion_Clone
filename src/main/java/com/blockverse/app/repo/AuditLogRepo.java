package com.blockverse.app.repo;

import com.blockverse.app.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepo extends JpaRepository<AuditLog, Integer> {
}
