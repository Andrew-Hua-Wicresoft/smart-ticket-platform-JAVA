package com.ticket.zhigong.service;

import com.ticket.zhigong.dto.AdminStatsResponse;
import com.ticket.zhigong.enums.KbArticleStatus;
import com.ticket.zhigong.enums.TicketStatus;
import com.ticket.zhigong.repository.DeflectionRepository;
import com.ticket.zhigong.repository.KnowledgeBaseRepository;
import com.ticket.zhigong.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private KnowledgeBaseRepository kbRepository;

    @Mock
    private DeflectionRepository deflectionRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    void getStatsIncludesReadableKpiBreakdowns() {
        when(ticketRepository.count()).thenReturn(20L);
        when(ticketRepository.countByStatus(TicketStatus.OPEN)).thenReturn(5L);
        when(ticketRepository.countByStatus(TicketStatus.IN_PROGRESS)).thenReturn(4L);
        when(ticketRepository.countByStatus(TicketStatus.RESOLVED)).thenReturn(10L);
        when(ticketRepository.countByStatus(TicketStatus.CLOSED)).thenReturn(1L);
        when(deflectionRepository.countByUserIdIsNotNull()).thenReturn(5L);
        when(kbRepository.countTotal()).thenReturn(20L);
        when(kbRepository.countByStatus(KbArticleStatus.PUBLISHED)).thenReturn(16L);

        AdminStatsResponse stats = adminService.getStats();

        assertThat(stats.getDeflectionCount()).isEqualTo(5L);
        assertThat(stats.getDeflectionOpportunityCount()).isEqualTo(25L);
        assertThat(stats.getDeflectionRate()).isEqualTo(20.0);
        assertThat(stats.getKbArticleCount()).isEqualTo(20L);
        assertThat(stats.getKbPublishedCount()).isEqualTo(16L);
        assertThat(stats.getKbDraftCount()).isEqualTo(4L);
        assertThat(stats.getKbPublicationRate()).isEqualTo(80.0);
    }

    @Test
    void getStatsUsesZeroRatesWhenNoOpportunitiesExist() {
        when(ticketRepository.count()).thenReturn(0L);
        when(deflectionRepository.countByUserIdIsNotNull()).thenReturn(0L);
        when(kbRepository.countTotal()).thenReturn(0L);
        when(kbRepository.countByStatus(KbArticleStatus.PUBLISHED)).thenReturn(0L);

        AdminStatsResponse stats = adminService.getStats();

        assertThat(stats.getDeflectionOpportunityCount()).isZero();
        assertThat(stats.getDeflectionRate()).isZero();
        assertThat(stats.getKbDraftCount()).isZero();
        assertThat(stats.getKbPublicationRate()).isZero();
    }
}
