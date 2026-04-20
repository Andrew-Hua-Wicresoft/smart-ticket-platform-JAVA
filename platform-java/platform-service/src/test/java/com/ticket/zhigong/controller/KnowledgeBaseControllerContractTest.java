package com.ticket.zhigong.controller;

import com.ticket.zhigong.dto.KbArticleResponse;
import com.ticket.zhigong.entity.KnowledgeBase;
import com.ticket.zhigong.entity.User;
import com.ticket.zhigong.enums.KbArticleStatus;
import com.ticket.zhigong.enums.UserRole;
import com.ticket.zhigong.service.KnowledgeBaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseControllerContractTest extends ControllerContractTestSupport {

    @Mock
    private KnowledgeBaseService knowledgeBaseService;

    @InjectMocks
    private KnowledgeBaseController knowledgeBaseController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = buildMockMvc(knowledgeBaseController);
    }

    @Test
    void listPublishedReturnsPagedArticles() throws Exception {
        when(knowledgeBaseService.listArticles(eq(KbArticleStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleArticleResponse()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/kb/published"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(501))
                .andExpect(jsonPath("$.content[0].title").value("VPN 常见故障处理"))
                .andExpect(jsonPath("$.content[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$.content[0].createdByName").value("工程师一"));
    }

    private KbArticleResponse sampleArticleResponse() {
        User engineer = new User("engineer1", "secret", "工程师一", UserRole.ENGINEER, "IT");
        engineer.setId(88L);

        KnowledgeBase article = new KnowledgeBase("VPN 常见故障处理", "先检查网络，再重置客户端。", KbArticleStatus.PUBLISHED, 3001L);
        article.setId(501L);
        article.setCreatedBy(engineer);

        return KbArticleResponse.fromEntity(article);
    }
}
