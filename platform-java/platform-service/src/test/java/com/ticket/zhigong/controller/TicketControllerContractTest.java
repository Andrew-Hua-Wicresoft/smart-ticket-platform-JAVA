package com.ticket.zhigong.controller;

import com.ticket.zhigong.dto.TicketCreateRequest;
import com.ticket.zhigong.dto.TicketListFilter;
import com.ticket.zhigong.dto.TicketResponse;
import com.ticket.zhigong.entity.Ticket;
import com.ticket.zhigong.entity.User;
import com.ticket.zhigong.enums.TicketAssigneeScope;
import com.ticket.zhigong.enums.TicketPriority;
import com.ticket.zhigong.enums.TicketStatus;
import com.ticket.zhigong.enums.UserRole;
import com.ticket.zhigong.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TicketControllerContractTest extends ControllerContractTestSupport {

    @Mock
    private TicketService ticketService;

    @InjectMocks
    private TicketController ticketController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(ticketController);
    }

    @Test
    void createTicketReturnsCreatedTicketPayload() throws Exception {
        authenticate(2001L, "customer1", "ROLE_CUSTOMER");

        TicketCreateRequest request = new TicketCreateRequest();
        request.setTitle("VPN 无法连接");
        request.setDescription("公司 VPN 客户端登录后立即断开，已经尝试重启电脑。");

        when(ticketService.createTicket(any(TicketCreateRequest.class), eq(2001L)))
                .thenReturn(sampleTicketResponse());

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("VPN 无法连接"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.customerId").value(2001))
                .andExpect(jsonPath("$.imageUrls[0]").value("https://example.com/vpn.png"));
    }

    @Test
    void createTicketAllowsAdminActor() throws Exception {
        authenticate(9001L, "admin1", "ROLE_ADMIN");

        TicketCreateRequest request = new TicketCreateRequest();
        request.setTitle("管理员提交设备故障");
        request.setDescription("管理员巡检时发现会议室投影设备无法开机，需要工程师排查。");

        when(ticketService.createTicket(any(TicketCreateRequest.class), eq(9001L)))
                .thenReturn(sampleTicketResponse());

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void listTicketsPassesFilterParametersToService() throws Exception {
        authenticate(9001L, "admin1", "ROLE_ADMIN");
        when(ticketService.listTickets(eq(9001L), eq(UserRole.ADMIN), any(), any(TicketListFilter.class)))
                .thenReturn(new PageImpl<>(List.of(sampleTicketResponse()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/tickets")
                        .param("status", "OPEN")
                        .param("priority", "HIGH")
                        .param("keyword", "vpn")
                        .param("assignee", "UNASSIGNED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("VPN 无法连接"));

        ArgumentCaptor<TicketListFilter> filterCaptor = ArgumentCaptor.forClass(TicketListFilter.class);
        verify(ticketService).listTickets(eq(9001L), eq(UserRole.ADMIN), any(), filterCaptor.capture());
        TicketListFilter filter = filterCaptor.getValue();
        assertThat(filter.getStatuses()).containsExactly(TicketStatus.OPEN);
        assertThat(filter.getPriorities()).containsExactly(TicketPriority.HIGH);
        assertThat(filter.getKeyword()).isEqualTo("vpn");
        assertThat(filter.getAssignee()).isEqualTo(TicketAssigneeScope.UNASSIGNED);
    }

    @Test
    void listTicketsRejectsInvalidFilterParameters() throws Exception {
        authenticate(9001L, "admin1", "ROLE_ADMIN");

        mockMvc.perform(get("/api/tickets").param("status", "BROKEN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILTER"))
                .andExpect(jsonPath("$.message").value("status 参数无效: BROKEN"));
    }

    @Test
    void createTicketRejectsShortTitle() throws Exception {
        authenticate(2001L, "customer1", "ROLE_CUSTOMER");

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"太短","description":"这是一段超过十个字符的问题描述。"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("标题长度必须在5-200字之间"));
    }

    private TicketResponse sampleTicketResponse() {
        User customer = new User("customer1", "secret", "客户一", UserRole.CUSTOMER, "IT");
        customer.setId(2001L);

        Ticket ticket = new Ticket();
        ticket.setId(3001L);
        ticket.setTitle("VPN 无法连接");
        ticket.setDescription("公司 VPN 客户端登录后立即断开，已经尝试重启电脑。");
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(TicketPriority.HIGH);
        ticket.setPriorityReason("影响远程办公");
        ticket.setImageUrls(List.of("https://example.com/vpn.png"));
        ticket.setCustomer(customer);

        return TicketResponse.fromEntity(ticket);
    }
}
