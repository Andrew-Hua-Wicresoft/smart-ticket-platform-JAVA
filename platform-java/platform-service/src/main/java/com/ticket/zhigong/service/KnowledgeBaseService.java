package com.ticket.zhigong.service;

import com.ticket.zhigong.dto.AiSearchResult;
import com.ticket.zhigong.dto.KbArticleResponse;
import com.ticket.zhigong.entity.KnowledgeBase;
import com.ticket.zhigong.entity.User;
import com.ticket.zhigong.enums.AuditAction;
import com.ticket.zhigong.enums.KbArticleStatus;
import com.ticket.zhigong.enums.NotificationType;
import com.ticket.zhigong.enums.UserRole;
import com.ticket.zhigong.exception.BusinessException;
import com.ticket.zhigong.repository.KnowledgeBaseRepository;
import com.ticket.zhigong.repository.UserRepository;
import com.ticket.zhigong.security.SecurityUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
    private final SanitizationService sanitizationService;
    private final EntityManager entityManager;
    private final NotificationService notificationService;
    private final AuditService auditService;

    public KnowledgeBaseService(KnowledgeBaseRepository kbRepository,
                                 UserRepository userRepository,
                                 EmbeddingService embeddingService,
                                 SanitizationService sanitizationService,
                                 EntityManager entityManager,
                                 NotificationService notificationService,
                                 AuditService auditService) {
        this.kbRepository = kbRepository;
        this.userRepository = userRepository;
        this.embeddingService = embeddingService;
        this.sanitizationService = sanitizationService;
        this.entityManager = entityManager;
        this.notificationService = notificationService;
        this.auditService = auditService;
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

    @Transactional
    public KnowledgeBase createDraftArticle(Long ticketId, String title, String articleContent, Long engineerId) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setTitle(sanitizationService.sanitize(title));
        kb.setContent(sanitizationService.sanitize(articleContent));
        kb.setStatus(KbArticleStatus.DRAFT);
        kb.setSourceTicketId(ticketId);

        User engineer = userRepository.findById(engineerId).orElse(null);
        kb.setCreatedBy(engineer);

        kb = kbRepository.save(kb);
        embedKbArticle(kb);
        log.info("KB draft created [ticketId={}, articleId={}]", ticketId, kb.getId());
        return kb;
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
        notificationService.notifyUsers(
                userRepository.findByRoleIn(List.of(UserRole.ADMIN)).stream()
                        .map(User::getId)
                        .toList(),
                NotificationType.KB_ARTICLE_PUBLISHED,
                "知识库文章已发布",
                "知识库文章 #" + articleId + " 已发布：" + kb.getTitle(),
                kb.getSourceTicketId()
        );
        auditService.log(SecurityUtils.getCurrentUserId(),
                AuditAction.KB_ARTICLE_PUBLISHED, "KNOWLEDGE_BASE", articleId,
                "发布知识库文章 #" + articleId);

        return KbArticleResponse.fromEntity(kb);
    }

    @Transactional
    public KbArticleResponse updateArticle(Long articleId, String title, String content) {
        KnowledgeBase kb = kbRepository.findById(articleId)
                .orElseThrow(() -> new EntityNotFoundException("知识库文章 #" + articleId + " 不存在"));

        kb.setTitle(sanitizationService.sanitize(title));
        kb.setContent(sanitizationService.sanitize(content));
        kb = kbRepository.save(kb);

        // Re-embed with updated content
        embedKbArticle(kb);
        auditService.log(SecurityUtils.getCurrentUserId(),
                AuditAction.KB_ARTICLE_UPDATED, "KNOWLEDGE_BASE", articleId,
                "更新知识库文章 #" + articleId);

        return KbArticleResponse.fromEntity(kb);
    }

    @Transactional
    public void deleteArticle(Long articleId) {
        if (!kbRepository.existsById(articleId)) {
            throw new EntityNotFoundException("知识库文章 #" + articleId + " 不存在");
        }
        kbRepository.deleteById(articleId);
        auditService.log(SecurityUtils.getCurrentUserId(),
                AuditAction.KB_ARTICLE_DELETED, "KNOWLEDGE_BASE", articleId,
                "删除知识库文章 #" + articleId);
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
