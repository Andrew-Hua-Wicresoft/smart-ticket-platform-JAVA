package com.ticket.zhigong.controller;

import com.ticket.zhigong.dto.AdminStatsResponse;
import com.ticket.zhigong.dto.AuditLogResponse;
import com.ticket.zhigong.enums.AuditAction;
import com.ticket.zhigong.enums.UserRole;
import com.ticket.zhigong.service.AdminService;
import com.ticket.zhigong.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminControllerContractTest extends ControllerContractTestSupport {

    @Mock
    private AdminService adminService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AdminController adminController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(adminController);
    }

    @Test
    void getStatsReturnsAdminDashboardPayload() throws Exception {
        authenticate(1001L, "admin1", "ROLE_ADMIN");

        AdminStatsResponse stats = new AdminStatsResponse();
        stats.setTotalTickets(12);
        stats.setTicketsByStatus(Map.of("OPEN", 4L, "RESOLVED", 8L));
        stats.setDeflectionRate(42.5);
        stats.setDeflectionCount(9);
        stats.setDeflectionOpportunityCount(21);
        stats.setAvgResolutionTimeHours(3.75);
        stats.setKbArticleCount(18);
        stats.setKbPublishedCount(12);
        stats.setKbDraftCount(6);
        stats.setKbPublicationRate(66.7);

        when(adminService.getStats()).thenReturn(stats);

        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTickets").value(12))
                .andExpect(jsonPath("$.deflectionCount").value(9))
                .andExpect(jsonPath("$.deflectionOpportunityCount").value(21))
                .andExpect(jsonPath("$.kbPublishedCount").value(12))
                .andExpect(jsonPath("$.kbDraftCount").value(6))
                .andExpect(jsonPath("$.kbPublicationRate").value(66.7));
    }

    @Test
    void auditLogsReturnsPagedAuditData() throws Exception {
        authenticate(1001L, "admin1", "ROLE_ADMIN");

        when(auditService.listLogs(any()))
                .thenReturn(new PageImpl<>(List.of(sampleAuditLog()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/admin/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("TICKET_RESOLVED"))
                .andExpect(jsonPath("$.content[0].actorRole").value("ENGINEER"));
    }

    private AuditLogResponse sampleAuditLog() {
        AuditLogResponse response = new AuditLogResponse();
        set(response, "id", 7001L);
        set(response, "actorId", 3001L);
        set(response, "actorName", "工程师一");
        set(response, "actorRole", UserRole.ENGINEER);
        set(response, "action", AuditAction.TICKET_RESOLVED);
        set(response, "resourceType", "TICKET");
        set(response, "resourceId", 3001L);
        set(response, "summary", "解决工单 #3001");
        set(response, "requestId", "req-123");
        set(response, "createdAt", LocalDateTime.of(2026, 4, 20, 14, 0));
        return response;
    }

    private void set(AuditLogResponse response, String field, Object value) {
        try {
            var declaredField = AuditLogResponse.class.getDeclaredField(field);
            declaredField.setAccessible(true);
            declaredField.set(response, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
