package com.ticket.zhigong.repository;

import com.ticket.zhigong.entity.KnowledgeBase;
import com.ticket.zhigong.enums.KbArticleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {

    Page<KnowledgeBase> findByStatus(KbArticleStatus status, Pageable pageable);

    boolean existsBySourceTicketId(Long sourceTicketId);

    long countByStatus(KbArticleStatus status);

    @Query("SELECT COUNT(k) FROM KnowledgeBase k")
    long countTotal();
}
