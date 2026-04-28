package com.ticket.zhigong.service;

import com.ticket.zhigong.dto.TicketListFilter;
import com.ticket.zhigong.dto.TicketResponse;
import com.ticket.zhigong.entity.Ticket;
import com.ticket.zhigong.entity.User;
import com.ticket.zhigong.enums.TicketAssigneeScope;
import com.ticket.zhigong.enums.TicketPriority;
import com.ticket.zhigong.enums.TicketStatus;
import com.ticket.zhigong.enums.UserRole;
import com.ticket.zhigong.messaging.TicketEventPublisher;
import com.ticket.zhigong.repository.TicketRepository;
import com.ticket.zhigong.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditService auditService;

    @Mock
    private TicketEventPublisher ticketEventPublisher;

    @InjectMocks
    private TicketService ticketService;

    @Test
    void engineerDefaultStatusScopeStaysOpenAndInProgress() {
        List<TicketStatus> statuses = ticketService.allowedStatuses(UserRole.ENGINEER, TicketListFilter.empty());

        assertThat(statuses).containsExactly(TicketStatus.OPEN, TicketStatus.IN_PROGRESS);
    }

    @Test
    void engineerCannotUseFilterToReadResolvedTickets() {
        TicketListFilter filter = new TicketListFilter(
                List.of(TicketStatus.RESOLVED),
                List.of(),
                null,
                TicketAssigneeScope.ALL
        );

        var result = ticketService.listTickets(3001L, UserRole.ENGINEER, PageRequest.of(0, 20), filter);

        assertThat(result.getTotalElements()).isZero();
        verify(ticketRepository, never()).findAll(anyTicketSpecification(), any(Pageable.class));
    }

    @Test
    void adminCombinedFilterUsesRepositoryQuery() {
        TicketListFilter filter = new TicketListFilter(
                List.of(TicketStatus.RESOLVED),
                List.of(TicketPriority.HIGH),
                "vpn",
                TicketAssigneeScope.UNASSIGNED
        );
        when(ticketRepository.findAll(anyTicketSpecification(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleTicket())));

        var result = ticketService.listTickets(9001L, UserRole.ADMIN, PageRequest.of(0, 20), filter);

        assertThat(result.getContent()).extracting(TicketResponse::getTitle).containsExactly("VPN 无法连接");
        verify(ticketRepository).findAll(anyTicketSpecification(), any(Pageable.class));
    }

    @SuppressWarnings("unchecked")
    private Specification<Ticket> anyTicketSpecification() {
        return any(Specification.class);
    }

    private Ticket sampleTicket() {
        User customer = new User("customer1", "secret", "客户一", UserRole.CUSTOMER, "IT");
        customer.setId(2001L);

        Ticket ticket = new Ticket();
        ticket.setId(3001L);
        ticket.setTitle("VPN 无法连接");
        ticket.setDescription("VPN 登录后立即断开。");
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setPriority(TicketPriority.HIGH);
        ticket.setCustomer(customer);
        return ticket;
    }
}
