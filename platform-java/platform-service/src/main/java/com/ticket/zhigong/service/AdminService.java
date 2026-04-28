package com.ticket.zhigong.service;

import com.ticket.zhigong.dto.AdminStatsResponse;
import com.ticket.zhigong.enums.KbArticleStatus;
import com.ticket.zhigong.enums.TicketStatus;
import com.ticket.zhigong.repository.DeflectionRepository;
import com.ticket.zhigong.repository.KnowledgeBaseRepository;
import com.ticket.zhigong.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AdminService {

    private final TicketRepository ticketRepository;
    private final KnowledgeBaseRepository kbRepository;
    private final DeflectionRepository deflectionRepository;

    public AdminService(TicketRepository ticketRepository,
                         KnowledgeBaseRepository kbRepository,
                         DeflectionRepository deflectionRepository) {
        this.ticketRepository = ticketRepository;
        this.kbRepository = kbRepository;
        this.deflectionRepository = deflectionRepository;
    }

    public AdminStatsResponse getStats() {
        AdminStatsResponse stats = new AdminStatsResponse();

        long totalTickets = ticketRepository.count();
        stats.setTotalTickets(totalTickets);

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (TicketStatus status : TicketStatus.values()) {
            byStatus.put(status.name(), ticketRepository.countByStatus(status));
        }
        stats.setTicketsByStatus(byStatus);

        long deflections = deflectionRepository.countByUserIdIsNotNull();
        long deflectionOpportunities = totalTickets + deflections;
        double deflectionRate = deflectionOpportunities > 0
                ? (double) deflections / deflectionOpportunities * 100
                : 0;
        stats.setDeflectionCount(deflections);
        stats.setDeflectionOpportunityCount(deflectionOpportunities);
        stats.setDeflectionRate(roundOneDecimal(deflectionRate));

        stats.setAvgResolutionTimeHours(ticketRepository.averageResolutionTimeHours());

        long kbArticleCount = kbRepository.countTotal();
        long kbPublishedCount = kbRepository.countByStatus(KbArticleStatus.PUBLISHED);
        long kbDraftCount = Math.max(kbArticleCount - kbPublishedCount, 0);
        double kbPublicationRate = kbArticleCount > 0
                ? (double) kbPublishedCount / kbArticleCount * 100
                : 0;
        stats.setKbArticleCount(kbArticleCount);
        stats.setKbPublishedCount(kbPublishedCount);
        stats.setKbDraftCount(kbDraftCount);
        stats.setKbPublicationRate(roundOneDecimal(kbPublicationRate));

        return stats;
    }

    private double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
