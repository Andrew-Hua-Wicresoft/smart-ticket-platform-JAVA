package com.ticket.zhigong.dto;

import com.ticket.zhigong.entity.AuditLog;
import com.ticket.zhigong.enums.AuditAction;
import com.ticket.zhigong.enums.UserRole;

import java.time.LocalDateTime;

public class AuditLogResponse {

    private Long id;
    private Long actorId;
    private String actorName;
    private UserRole actorRole;
    private AuditAction action;
    private String resourceType;
    private Long resourceId;
    private String summary;
    private String requestId;
    private LocalDateTime createdAt;

    public static AuditLogResponse fromEntity(AuditLog auditLog) {
        AuditLogResponse response = new AuditLogResponse();
        response.id = auditLog.getId();
        if (auditLog.getActor() != null) {
            response.actorId = auditLog.getActor().getId();
            response.actorName = auditLog.getActor().getName();
            response.actorRole = auditLog.getActor().getRole();
        }
        response.action = auditLog.getAction();
        response.resourceType = auditLog.getResourceType();
        response.resourceId = auditLog.getResourceId();
        response.summary = auditLog.getSummary();
        response.requestId = auditLog.getRequestId();
        response.createdAt = auditLog.getCreatedAt();
        return response;
    }

    public Long getId() { return id; }
    public Long getActorId() { return actorId; }
    public String getActorName() { return actorName; }
    public UserRole getActorRole() { return actorRole; }
    public AuditAction getAction() { return action; }
    public String getResourceType() { return resourceType; }
    public Long getResourceId() { return resourceId; }
    public String getSummary() { return summary; }
    public String getRequestId() { return requestId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
