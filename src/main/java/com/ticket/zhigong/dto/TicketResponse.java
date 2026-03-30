package com.ticket.zhigong.dto;

import com.ticket.zhigong.entity.Ticket;
import com.ticket.zhigong.enums.TicketPriority;
import com.ticket.zhigong.enums.TicketStatus;
import java.time.LocalDateTime;
import java.util.List;

public class TicketResponse {

    private Long id;
    private String title;
    private String description;
    private TicketStatus status;
    private TicketPriority priority;
    private String priorityReason;
    private List<String> imageUrls;
    private Long customerId;
    private String customerName;
    private Long assignedEngineerId;
    private String assignedEngineerName;
    private String resolutionNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;

    public static TicketResponse fromEntity(Ticket ticket) {
        TicketResponse response = new TicketResponse();
        response.id = ticket.getId();
        response.title = ticket.getTitle();
        response.description = ticket.getDescription();
        response.status = ticket.getStatus();
        response.priority = ticket.getPriority();
        response.priorityReason = ticket.getPriorityReason();
        response.imageUrls = ticket.getImageUrls();
        response.customerId = ticket.getCustomer().getId();
        response.customerName = ticket.getCustomer().getName();
        if (ticket.getAssignedEngineer() != null) {
            response.assignedEngineerId = ticket.getAssignedEngineer().getId();
            response.assignedEngineerName = ticket.getAssignedEngineer().getName();
        }
        response.resolutionNotes = ticket.getResolutionNotes();
        response.createdAt = ticket.getCreatedAt();
        response.updatedAt = ticket.getUpdatedAt();
        response.resolvedAt = ticket.getResolvedAt();
        return response;
    }

    // Getters
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public TicketStatus getStatus() { return status; }
    public TicketPriority getPriority() { return priority; }
    public String getPriorityReason() { return priorityReason; }
    public List<String> getImageUrls() { return imageUrls; }
    public Long getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public Long getAssignedEngineerId() { return assignedEngineerId; }
    public String getAssignedEngineerName() { return assignedEngineerName; }
    public String getResolutionNotes() { return resolutionNotes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
}
