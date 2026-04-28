package com.ticket.zhigong.dto;

import java.util.Map;

public class AdminStatsResponse {

    private long totalTickets;
    private Map<String, Long> ticketsByStatus;
    private double deflectionRate;
    private long deflectionCount;
    private long deflectionOpportunityCount;
    private Double avgResolutionTimeHours;
    private long kbArticleCount;
    private long kbPublishedCount;
    private long kbDraftCount;
    private double kbPublicationRate;

    public long getTotalTickets() { return totalTickets; }
    public void setTotalTickets(long totalTickets) { this.totalTickets = totalTickets; }
    public Map<String, Long> getTicketsByStatus() { return ticketsByStatus; }
    public void setTicketsByStatus(Map<String, Long> ticketsByStatus) { this.ticketsByStatus = ticketsByStatus; }
    public double getDeflectionRate() { return deflectionRate; }
    public void setDeflectionRate(double deflectionRate) { this.deflectionRate = deflectionRate; }
    public long getDeflectionCount() { return deflectionCount; }
    public void setDeflectionCount(long deflectionCount) { this.deflectionCount = deflectionCount; }
    public long getDeflectionOpportunityCount() { return deflectionOpportunityCount; }
    public void setDeflectionOpportunityCount(long deflectionOpportunityCount) { this.deflectionOpportunityCount = deflectionOpportunityCount; }
    public Double getAvgResolutionTimeHours() { return avgResolutionTimeHours; }
    public void setAvgResolutionTimeHours(Double avgResolutionTimeHours) { this.avgResolutionTimeHours = avgResolutionTimeHours; }
    public long getKbArticleCount() { return kbArticleCount; }
    public void setKbArticleCount(long kbArticleCount) { this.kbArticleCount = kbArticleCount; }
    public long getKbPublishedCount() { return kbPublishedCount; }
    public void setKbPublishedCount(long kbPublishedCount) { this.kbPublishedCount = kbPublishedCount; }
    public long getKbDraftCount() { return kbDraftCount; }
    public void setKbDraftCount(long kbDraftCount) { this.kbDraftCount = kbDraftCount; }
    public double getKbPublicationRate() { return kbPublicationRate; }
    public void setKbPublicationRate(double kbPublicationRate) { this.kbPublicationRate = kbPublicationRate; }
}
