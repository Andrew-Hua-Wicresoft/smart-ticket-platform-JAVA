package com.ticket.zhigong.controller;

import com.ticket.zhigong.dto.TicketCreateRequest;
import com.ticket.zhigong.dto.TicketResolveRequest;
import com.ticket.zhigong.dto.TicketResponse;
import com.ticket.zhigong.enums.UserRole;
import com.ticket.zhigong.security.SecurityUtils;
import com.ticket.zhigong.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody TicketCreateRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        TicketResponse response = ticketService.createTicket(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<TicketResponse>> listTickets(@PageableDefault(size = 20) Pageable pageable) {
        Long userId = SecurityUtils.getCurrentUserId();
        UserRole role = getCurrentRole();
        return ResponseEntity.ok(ticketService.listTickets(userId, role, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> getTicket(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        UserRole role = getCurrentRole();
        return ResponseEntity.ok(ticketService.getTicket(id, userId, role));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('ENGINEER') or hasRole('ADMIN')")
    public ResponseEntity<TicketResponse> assignTicket(@PathVariable Long id) {
        Long engineerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ticketService.assignTicket(id, engineerId));
    }

    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasRole('ENGINEER') or hasRole('ADMIN')")
    public ResponseEntity<TicketResponse> resolveTicket(@PathVariable Long id,
                                                         @Valid @RequestBody TicketResolveRequest request) {
        Long engineerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ticketService.resolveTicket(id, request, engineerId));
    }

    private UserRole getCurrentRole() {
        if (SecurityUtils.hasRole("ADMIN")) return UserRole.ADMIN;
        if (SecurityUtils.hasRole("ENGINEER")) return UserRole.ENGINEER;
        return UserRole.CUSTOMER;
    }
}
