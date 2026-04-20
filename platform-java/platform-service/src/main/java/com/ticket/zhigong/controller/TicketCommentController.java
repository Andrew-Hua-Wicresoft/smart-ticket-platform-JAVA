package com.ticket.zhigong.controller;

import com.ticket.zhigong.dto.TicketCommentCreateRequest;
import com.ticket.zhigong.dto.TicketCommentResponse;
import com.ticket.zhigong.enums.UserRole;
import com.ticket.zhigong.security.SecurityUtils;
import com.ticket.zhigong.service.TicketCommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets/{ticketId}/comments")
public class TicketCommentController {

    private final TicketCommentService ticketCommentService;

    public TicketCommentController(TicketCommentService ticketCommentService) {
        this.ticketCommentService = ticketCommentService;
    }

    @GetMapping
    public ResponseEntity<List<TicketCommentResponse>> listComments(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ticketCommentService.listComments(
                ticketId,
                SecurityUtils.getCurrentUserId(),
                getCurrentRole()
        ));
    }

    @PostMapping
    public ResponseEntity<TicketCommentResponse> addComment(@PathVariable Long ticketId,
                                                             @Valid @RequestBody TicketCommentCreateRequest request) {
        TicketCommentResponse response = ticketCommentService.addComment(
                ticketId,
                request.getContent(),
                SecurityUtils.getCurrentUserId(),
                getCurrentRole()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private UserRole getCurrentRole() {
        if (SecurityUtils.hasRole("ADMIN")) return UserRole.ADMIN;
        if (SecurityUtils.hasRole("ENGINEER")) return UserRole.ENGINEER;
        return UserRole.CUSTOMER;
    }
}
