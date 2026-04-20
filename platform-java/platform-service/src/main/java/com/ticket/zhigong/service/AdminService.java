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

        // Deflection rate = deflections / (deflections + total tickets)
        long deflections = deflectionRepository.countByUserIdIsNotNull();
        double deflectionRate = (totalTickets + deflections) > 0
                ? (double) deflections / (deflections + totalTickets) * 100
                : 0;
        stats.setDeflectionRate(Math.round(deflectionRate * 10.0) / 10.0);

        stats.setAvgResolutionTimeHours(ticketRepository.averageResolutionTimeHours());

        stats.setKbArticleCount(kbRepository.countTotal());
        stats.setKbPublishedCount(kbRepository.countByStatus(KbArticleStatus.PUBLISHED));

        return stats;
    }
}
