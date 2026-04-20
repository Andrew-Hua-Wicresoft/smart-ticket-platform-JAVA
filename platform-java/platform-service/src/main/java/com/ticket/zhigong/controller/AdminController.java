package com.ticket.zhigong.controller;

import com.ticket.zhigong.dto.AdminStatsResponse;
import com.ticket.zhigong.dto.AuditLogResponse;
import com.ticket.zhigong.service.AdminService;
import com.ticket.zhigong.service.AuditService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final AuditService auditService;

    public AdminController(AdminService adminService, AuditService auditService) {
        this.adminService = adminService;
        this.auditService = auditService;
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<Page<AuditLogResponse>> listAuditLogs(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(auditService.listLogs(pageable));
    }
}
