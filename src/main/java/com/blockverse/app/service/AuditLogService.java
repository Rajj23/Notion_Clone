package com.blockverse.app.service;

import com.blockverse.app.entity.AuditLog;
import com.blockverse.app.enums.AuditActionType;
import com.blockverse.app.enums.AuditEntityType;
import com.blockverse.app.repo.AuditLogRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuditLogService {
    private final AuditLogRepo auditLogRepo;

    public void auditLog(int workspaceId, int userId, AuditEntityType auditType, int entityId, AuditActionType actionType, String metadata){
        AuditLog log = AuditLog.builder()
                .workSpaceId(workspaceId)
                .userId(userId)
                .entityType(auditType)
                .entityId(entityId)
                .actionType(actionType)
                .metadata(metadata)
                .build();
        auditLogRepo.save(log);
    }
}
