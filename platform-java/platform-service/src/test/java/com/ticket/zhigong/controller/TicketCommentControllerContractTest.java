package com.ticket.zhigong.controller;

import com.ticket.zhigong.dto.TicketCommentResponse;
import com.ticket.zhigong.enums.UserRole;
import com.ticket.zhigong.service.TicketCommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TicketCommentControllerContractTest extends ControllerContractTestSupport {

    @Mock
    private TicketCommentService ticketCommentService;

    @InjectMocks
    private TicketCommentController ticketCommentController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(ticketCommentController);
    }

    @Test
    void listCommentsReturnsThread() throws Exception {
        authenticate(2001L, "customer1", "ROLE_CUSTOMER");

        when(ticketCommentService.listComments(eq(3001L), eq(2001L), eq(UserRole.CUSTOMER)))
                .thenReturn(List.of(sampleComment()));

        mockMvc.perform(get("/api/tickets/3001/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticketId").value(3001))
                .andExpect(jsonPath("$[0].authorName").value("工程师一"))
                .andExpect(jsonPath("$[0].authorRole").value("ENGINEER"));
    }

    @Test
    void addCommentRejectsBlankContent() throws Exception {
        authenticate(2001L, "customer1", "ROLE_CUSTOMER");

        mockMvc.perform(post("/api/tickets/3001/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("评论内容不能为空"));
    }

    private TicketCommentResponse sampleComment() {
        return new TicketCommentResponseBuilder()
                .id(8001L)
                .ticketId(3001L)
                .authorId(4001L)
                .authorName("工程师一")
                .authorRole(UserRole.ENGINEER)
                .content("已经定位到 VPN 配置问题，正在处理。")
                .createdAt(LocalDateTime.of(2026, 4, 20, 12, 30))
                .updatedAt(LocalDateTime.of(2026, 4, 20, 12, 30))
                .build();
    }

    private static final class TicketCommentResponseBuilder {
        private final TicketCommentResponse response = new TicketCommentResponse();

        TicketCommentResponseBuilder id(Long value) { set("id", value); return this; }
        TicketCommentResponseBuilder ticketId(Long value) { set("ticketId", value); return this; }
        TicketCommentResponseBuilder authorId(Long value) { set("authorId", value); return this; }
        TicketCommentResponseBuilder authorName(String value) { set("authorName", value); return this; }
        TicketCommentResponseBuilder authorRole(UserRole value) { set("authorRole", value); return this; }
        TicketCommentResponseBuilder content(String value) { set("content", value); return this; }
        TicketCommentResponseBuilder createdAt(LocalDateTime value) { set("createdAt", value); return this; }
        TicketCommentResponseBuilder updatedAt(LocalDateTime value) { set("updatedAt", value); return this; }

        TicketCommentResponse build() { return response; }

        private void set(String field, Object value) {
            try {
                var declaredField = TicketCommentResponse.class.getDeclaredField(field);
                declaredField.setAccessible(true);
                declaredField.set(response, value);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
