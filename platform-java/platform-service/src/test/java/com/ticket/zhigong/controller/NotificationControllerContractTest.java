package com.ticket.zhigong.controller;

import com.ticket.zhigong.dto.NotificationResponse;
import com.ticket.zhigong.enums.NotificationType;
import com.ticket.zhigong.service.NotificationService;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationControllerContractTest extends ControllerContractTestSupport {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(notificationController);
    }

    @Test
    void listNotificationsReturnsUnreadFirst() throws Exception {
        authenticate(2001L, "customer1", "ROLE_CUSTOMER");

        when(notificationService.listNotifications(eq(2001L), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageImpl<>(List.of(sampleNotification()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].type").value("TICKET_ASSIGNED"))
                .andExpect(jsonPath("$.content[0].ticketId").value(3001))
                .andExpect(jsonPath("$.content[0].read").value(false));
    }

    @Test
    void countUnreadReturnsCurrentCount() throws Exception {
        authenticate(2001L, "customer1", "ROLE_CUSTOMER");

        when(notificationService.countUnread(eq(2001L))).thenReturn(3L);

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(3));
    }

    @Test
    void markAsReadReturnsUpdatedNotification() throws Exception {
        authenticate(2001L, "customer1", "ROLE_CUSTOMER");

        NotificationResponse response = sampleNotification();
        set(response, "read", true);
        set(response, "readAt", LocalDateTime.of(2026, 4, 20, 13, 45));

        when(notificationService.markAsRead(eq(9001L), eq(2001L))).thenReturn(response);

        mockMvc.perform(put("/api/notifications/9001/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
    }

    private NotificationResponse sampleNotification() {
        NotificationResponse response = new NotificationResponse();
        set(response, "id", 9001L);
        set(response, "type", NotificationType.TICKET_ASSIGNED);
        set(response, "title", "工单已被接取");
        set(response, "content", "工单 #3001 已由工程师一接取处理");
        set(response, "ticketId", 3001L);
        set(response, "read", false);
        set(response, "createdAt", LocalDateTime.of(2026, 4, 20, 13, 40));
        return response;
    }

    private void set(NotificationResponse response, String field, Object value) {
        try {
            var declaredField = NotificationResponse.class.getDeclaredField(field);
            declaredField.setAccessible(true);
            declaredField.set(response, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
