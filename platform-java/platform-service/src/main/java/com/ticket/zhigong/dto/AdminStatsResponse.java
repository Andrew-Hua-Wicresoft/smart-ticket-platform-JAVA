package com.ticket.zhigong.dto;

import java.util.Map;

public class AdminStatsResponse {

    private long totalTickets;
    private Map<String, Long> ticketsByStatus;
    private double deflectionRate;
    private Double avgResolutionTimeHours;
    private long kbArticleCount;
    private long kbPublishedCount;

    public long getTotalTickets() { return totalTickets; }
    public void setTotalTickets(long totalTickets) { this.totalTickets = totalTickets; }
    public Map<String, Long> getTicketsByStatus() { return ticketsByStatus; }
    public void setTicketsByStatus(Map<String, Long> ticketsByStatus) { this.ticketsByStatus = ticketsByStatus; }
    public double getDeflectionRate() { return deflectionRate; }
    public void setDeflectionRate(double deflectionRate) { this.deflectionRate = deflectionRate; }
    public Double getAvgResolutionTimeHours() { return avgResolutionTimeHours; }
    public void setAvgResolutionTimeHours(Double avgResolutionTimeHours) { this.avgResolutionTimeHours = avgResolutionTimeHours; }
    public long getKbArticleCount() { return kbArticleCount; }
    public void setKbArticleCount(long kbArticleCount) { this.kbArticleCount = kbArticleCount; }
    public long getKbPublishedCount() { return kbPublishedCount; }
    public void setKbPublishedCount(long kbPublishedCount) { this.kbPublishedCount = kbPublishedCount; }
}
