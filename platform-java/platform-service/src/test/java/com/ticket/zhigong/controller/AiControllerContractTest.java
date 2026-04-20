package com.ticket.zhigong.controller;

import com.ticket.zhigong.dto.AiSearchResult;
import com.ticket.zhigong.repository.TicketRepository;
import com.ticket.zhigong.service.AiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AiControllerContractTest extends ControllerContractTestSupport {

    @Mock
    private AiService aiService;

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private AiController aiController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(aiController);
    }

    @Test
    void searchReturnsAiSearchResults() throws Exception {
        authenticate(3001L, "engineer1", "ROLE_ENGINEER");

        when(aiService.search(eq("vpn 断连"), eq(2), eq(3001L)))
                .thenReturn(List.of(
                        new AiSearchResult(11L, "VPN 连接排障", "重置客户端缓存后重试。", 0.92),
                        new AiSearchResult(12L, "MFA 登录失败", "检查时间同步。", 0.57)
                ));

        mockMvc.perform(post("/api/ai/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"vpn 断连","topK":2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].kbId").value(11))
                .andExpect(jsonPath("$[0].title").value("VPN 连接排障"))
                .andExpect(jsonPath("$[0].similarity").value(0.92))
                .andExpect(jsonPath("$[1].kbId").value(12));
    }

    @Test
    void searchRejectsShortQuery() throws Exception {
        authenticate(3001L, "engineer1", "ROLE_ENGINEER");

        mockMvc.perform(post("/api/ai/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"query":"a","topK":2}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("搜索内容长度必须在2-5000字之间"));
    }
}
