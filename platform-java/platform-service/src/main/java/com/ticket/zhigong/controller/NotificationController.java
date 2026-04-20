package com.ticket.zhigong.controller;

import com.ticket.zhigong.dto.NotificationResponse;
import com.ticket.zhigong.dto.UnreadNotificationCountResponse;
import com.ticket.zhigong.security.SecurityUtils;
import com.ticket.zhigong.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> listNotifications(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(notificationService.listNotifications(
                SecurityUtils.getCurrentUserId(),
                pageable
        ));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadNotificationCountResponse> countUnread() {
        return ResponseEntity.ok(new UnreadNotificationCountResponse(
                notificationService.countUnread(SecurityUtils.getCurrentUserId())
        ));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(id, SecurityUtils.getCurrentUserId()));
    }
}
