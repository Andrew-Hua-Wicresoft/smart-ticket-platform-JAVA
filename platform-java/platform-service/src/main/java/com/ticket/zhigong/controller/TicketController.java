package com.ticket.zhigong.controller;

import com.ticket.zhigong.dto.TicketCreateRequest;
import com.ticket.zhigong.dto.TicketListFilter;
import com.ticket.zhigong.dto.TicketResolveRequest;
import com.ticket.zhigong.dto.TicketResponse;
import com.ticket.zhigong.enums.TicketAssigneeScope;
import com.ticket.zhigong.enums.TicketPriority;
import com.ticket.zhigong.enums.TicketStatus;
import com.ticket.zhigong.enums.UserRole;
import com.ticket.zhigong.exception.BusinessException;
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

import java.util.Arrays;
import java.util.List;

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
    public ResponseEntity<Page<TicketResponse>> listTickets(@PageableDefault(size = 20) Pageable pageable,
                                                            @RequestParam(required = false) String status,
                                                            @RequestParam(required = false) String priority,
                                                            @RequestParam(required = false) String keyword,
                                                            @RequestParam(defaultValue = "ALL") String assignee) {
        Long userId = SecurityUtils.getCurrentUserId();
        UserRole role = getCurrentRole();
        TicketListFilter filter = new TicketListFilter(
                parseEnumList(status, TicketStatus.class, "status"),
                parseEnumList(priority, TicketPriority.class, "priority"),
                keyword,
                parseEnum(assignee, TicketAssigneeScope.class, "assignee")
        );
        return ResponseEntity.ok(ticketService.listTickets(userId, role, pageable, filter));
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

    private <E extends Enum<E>> List<E> parseEnumList(String raw, Class<E> enumClass, String parameterName) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> parseEnum(value, enumClass, parameterName))
                .distinct()
                .toList();
    }

    private <E extends Enum<E>> E parseEnum(String raw, Class<E> enumClass, String parameterName) {
        try {
            return Enum.valueOf(enumClass, raw.trim().toUpperCase());
        } catch (Exception ex) {
            throw new BusinessException(
                    "INVALID_FILTER",
                    parameterName + " 参数无效: " + raw,
                    HttpStatus.BAD_REQUEST
            );
        }
    }
}
