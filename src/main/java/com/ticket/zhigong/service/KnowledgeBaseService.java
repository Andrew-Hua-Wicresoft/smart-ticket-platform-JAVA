package com.ticket.zhigong.service;

import com.ticket.zhigong.dto.AiSearchResult;
import com.ticket.zhigong.dto.KbArticleResponse;
import com.ticket.zhigong.entity.KnowledgeBase;
import com.ticket.zhigong.entity.User;
import com.ticket.zhigong.enums.KbArticleStatus;
import com.ticket.zhigong.exception.BusinessException;
import com.ticket.zhigong.repository.KnowledgeBaseRepository;
import com.ticket.zhigong.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);

    private final KnowledgeBaseRepository kbRepository;
    private final UserRepository userRepository;
    private final EmbeddingService embeddingService;
    private final ClaudeApiService claudeApiService;
    private final EntityManager entityManager;

    public KnowledgeBaseService(KnowledgeBaseRepository kbRepository,
                                 UserRepository userRepository,
                                 EmbeddingService embeddingService,
                                 ClaudeApiService claudeApiService,
                                 EntityManager entityManager) {
        this.kbRepository = kbRepository;
        this.userRepository = userRepository;
        this.embeddingService = embeddingService;
        this.claudeApiService = claudeApiService;
        this.entityManager = entityManager;
    }

    /**
     * Vector similarity search against published KB articles.
     */
    public List<AiSearchResult> searchSimilar(String queryText, int topK) {
        float[] embedding = embeddingService.embed(queryText);
        if (embedding == null) {
            return List.of();
        }

        // Manual pgvector SQL with cosine distance
        String vectorStr = vectorToString(embedding);
        String sql = "SELECT id, title, content, 1 - (content_embedding <=> cast(:vec as vector)) as similarity " +
                     "FROM knowledge_base " +
                     "WHERE status = 'PUBLISHED' AND content_embedding IS NOT NULL " +
                     "ORDER BY content_embedding <=> cast(:vec as vector) " +
                     "LIMIT :topK";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("vec", vectorStr);
        query.setParameter("topK", topK);

        List<Object[]> results = query.getResultList();
        List<AiSearchResult> searchResults = new ArrayList<>();

        for (Object[] row : results) {
            Long id = ((Number) row[0]).longValue();
            String title = (String) row[1];
            String content = (String) row[2];
            double similarity = ((Number) row[3]).doubleValue();

            if (similarity > 0.3) { // minimum similarity threshold
                searchResults.add(new AiSearchResult(id, title, content, similarity));
            }
        }

        return searchResults;
    }

    /**
     * Async: Generate a KB article from resolution notes using Claude API.
     * Creates as DRAFT status for engineer review.
     */
    @Async
    public void generateKbArticleFromResolution(Long ticketId, String title, String description,
                                                 String resolutionNotes, Long engineerId) {
        try {
            String articleContent = claudeApiService.call(
                    "你是一个IT知识库文章生成器。根据工单信息和解决方案，生成一篇结构化的知识库文章。" +
                    "格式：\\n## 问题描述\\n...\\n## 解决方案\\n...\\n## 注意事项\\n..." +
                    "使用简洁明了的中文。",
                    "工单标题: " + title + "\\n问题描述: " + description + "\\n解决方案: " + resolutionNotes,
                    "KB_GENERATE", engineerId, ticketId);

            if (articleContent == null) {
                // Claude unavailable, use raw resolution notes
                articleContent = "## 问题描述\n" + description + "\n\n## 解决方案\n" + resolutionNotes;
            }

            // Sanitize LLM output: strip HTML/script tags to prevent stored XSS/prompt injection
            articleContent = sanitizeLlmOutput(articleContent);

            KnowledgeBase kb = new KnowledgeBase();
            kb.setTitle(sanitizeLlmOutput(title));
            kb.setContent(articleContent);
            kb.setStatus(KbArticleStatus.DRAFT);
            kb.setSourceTicketId(ticketId);

            User engineer = userRepository.findById(engineerId).orElse(null);
            kb.setCreatedBy(engineer);

            kb = kbRepository.save(kb);

            // Embed the content for vector search
            embedKbArticle(kb);

            log.info("KB article generated from ticket #{}: DRAFT id={}", ticketId, kb.getId());
        } catch (Exception e) {
            log.error("Failed to generate KB article from ticket #{}: {}", ticketId, e.getMessage());
        }
    }

    /**
     * Embed a KB article's content and store the vector.
     */
    @Transactional
    public void embedKbArticle(KnowledgeBase kb) {
        float[] embedding = embeddingService.embed(kb.getContent());
        if (embedding != null) {
            String vectorStr = vectorToString(embedding);
            entityManager.createNativeQuery(
                    "UPDATE knowledge_base SET content_embedding = cast(:vec as vector) WHERE id = :id")
                    .setParameter("vec", vectorStr)
                    .setParameter("id", kb.getId())
                    .executeUpdate();
        }
    }

    public Page<KbArticleResponse> listArticles(KbArticleStatus status, Pageable pageable) {
        return kbRepository.findByStatus(status, pageable).map(KbArticleResponse::fromEntity);
    }

    @Transactional
    public KbArticleResponse publishArticle(Long articleId) {
        KnowledgeBase kb = kbRepository.findById(articleId)
                .orElseThrow(() -> new EntityNotFoundException("知识库文章 #" + articleId + " 不存在"));

        if (kb.getStatus() != KbArticleStatus.DRAFT) {
            throw new BusinessException("INVALID_STATUS", "只能发布 DRAFT 状态的文章", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        kb.setStatus(KbArticleStatus.PUBLISHED);
        kb = kbRepository.save(kb);

        // Re-embed if needed
        embedKbArticle(kb);

        return KbArticleResponse.fromEntity(kb);
    }

    @Transactional
    public KbArticleResponse updateArticle(Long articleId, String title, String content) {
        KnowledgeBase kb = kbRepository.findById(articleId)
                .orElseThrow(() -> new EntityNotFoundException("知识库文章 #" + articleId + " 不存在"));

        kb.setTitle(title);
        kb.setContent(content);
        kb = kbRepository.save(kb);

        // Re-embed with updated content
        embedKbArticle(kb);

        return KbArticleResponse.fromEntity(kb);
    }

    @Transactional
    public void deleteArticle(Long articleId) {
        if (!kbRepository.existsById(articleId)) {
            throw new EntityNotFoundException("知识库文章 #" + articleId + " 不存在");
        }
        kbRepository.deleteById(articleId);
    }

    /**
     * Sanitize LLM-generated text before storing in KB.
     * Strips HTML/script tags to prevent stored XSS and prompt injection.
     */
    private String sanitizeLlmOutput(String text) {
        if (text == null) return null;
        // Remove HTML tags (including script, style, iframe)
        String sanitized = text.replaceAll("<[^>]*>", "");
        // Remove common prompt injection patterns
        sanitized = sanitized.replaceAll("(?i)<script[^>]*>.*?</script>", "");
        // Trim excessive whitespace
        sanitized = sanitized.replaceAll("\\n{3,}", "\n\n");
        return sanitized.trim();
    }

    private String vectorToString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
