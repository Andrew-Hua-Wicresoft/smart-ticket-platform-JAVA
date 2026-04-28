package com.ticket.zhigong.service;

import com.ticket.zhigong.dto.TicketListFilter;
import com.ticket.zhigong.dto.TicketResponse;
import com.ticket.zhigong.entity.Ticket;
import com.ticket.zhigong.entity.User;
import com.ticket.zhigong.enums.TicketPriority;
import com.ticket.zhigong.enums.TicketStatus;
import com.ticket.zhigong.enums.UserRole;
import com.ticket.zhigong.messaging.TicketEventPublisher;
import com.ticket.zhigong.repository.TicketRepository;
import com.ticket.zhigong.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.sql.init.mode=never",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.show_sql=false"
})
@Import(TicketService.class)
class TicketServiceSortIntegrationTest {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private TicketEventPublisher ticketEventPublisher;

    @Test
    void prioritySortDescUsesBusinessRank() {
        User customer = saveCustomer("customer-priority");
        saveTicket("低优先级工单", TicketPriority.LOW, TicketStatus.OPEN, customer);
        saveTicket("高优先级工单", TicketPriority.HIGH, TicketStatus.OPEN, customer);
        saveTicket("中优先级工单", TicketPriority.MEDIUM, TicketStatus.OPEN, customer);

        var result = ticketService.listTickets(
                9001L,
                UserRole.ADMIN,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "priority")),
                TicketListFilter.empty()
        );

        assertThat(result.getContent())
                .extracting(TicketResponse::getPriority)
                .containsExactly(TicketPriority.HIGH, TicketPriority.MEDIUM, TicketPriority.LOW);
    }

    @Test
    void statusSortAscUsesWorkflowOrder() {
        User customer = saveCustomer("customer-status");
        saveTicket("已解决工单", TicketPriority.MEDIUM, TicketStatus.RESOLVED, customer);
        saveTicket("处理中工单", TicketPriority.MEDIUM, TicketStatus.IN_PROGRESS, customer);
        saveTicket("待处理工单", TicketPriority.MEDIUM, TicketStatus.OPEN, customer);
        saveTicket("已关闭工单", TicketPriority.MEDIUM, TicketStatus.CLOSED, customer);

        var result = ticketService.listTickets(
                9001L,
                UserRole.ADMIN,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "status")),
                TicketListFilter.empty()
        );

        assertThat(result.getContent())
                .extracting(TicketResponse::getStatus)
                .containsExactly(TicketStatus.OPEN, TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED, TicketStatus.CLOSED);
    }

    private User saveCustomer(String username) {
        return userRepository.save(new User(username, "secret", username, UserRole.CUSTOMER, "IT"));
    }

    private Ticket saveTicket(String title, TicketPriority priority, TicketStatus status, User customer) {
        Ticket ticket = new Ticket();
        ticket.setTitle(title);
        ticket.setDescription("用于验证工单队列排序。");
        ticket.setPriority(priority);
        ticket.setStatus(status);
        ticket.setCustomer(customer);
        return ticketRepository.save(ticket);
    }
}
