package com.ticket.zhigong.service;

import com.ticket.zhigong.dto.AuditLogResponse;
import com.ticket.zhigong.entity.AuditLog;
import com.ticket.zhigong.entity.User;
import com.ticket.zhigong.enums.AuditAction;
import com.ticket.zhigong.repository.AuditLogRepository;
import com.ticket.zhigong.repository.UserRepository;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    public void log(Long actorId, AuditAction action, String resourceType, Long resourceId, String summary) {
        AuditLog auditLog = new AuditLog();
        if (actorId != null) {
            User actor = userRepository.findById(actorId).orElse(null);
            auditLog.setActor(actor);
        }
        auditLog.setAction(action);
        auditLog.setResourceType(resourceType);
        auditLog.setResourceId(resourceId);
        auditLog.setSummary(summary);
        auditLog.setRequestId(MDC.get("requestId"));
        auditLogRepository.save(auditLog);
    }

    public Page<AuditLogResponse> listLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable).map(AuditLogResponse::fromEntity);
    }
}
