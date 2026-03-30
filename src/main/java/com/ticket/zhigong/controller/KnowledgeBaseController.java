package com.ticket.zhigong.controller;

import com.ticket.zhigong.dto.KbArticleResponse;
import com.ticket.zhigong.enums.KbArticleStatus;
import com.ticket.zhigong.service.KnowledgeBaseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/kb")
public class KnowledgeBaseController {

    private final KnowledgeBaseService kbService;

    public KnowledgeBaseController(KnowledgeBaseService kbService) {
        this.kbService = kbService;
    }

    @GetMapping("/drafts")
    @PreAuthorize("hasRole('ENGINEER') or hasRole('ADMIN')")
    public ResponseEntity<Page<KbArticleResponse>> listDrafts(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(kbService.listArticles(KbArticleStatus.DRAFT, pageable));
    }

    @GetMapping("/published")
    public ResponseEntity<Page<KbArticleResponse>> listPublished(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(kbService.listArticles(KbArticleStatus.PUBLISHED, pageable));
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("hasRole('ENGINEER') or hasRole('ADMIN')")
    public ResponseEntity<KbArticleResponse> publish(@PathVariable Long id) {
        return ResponseEntity.ok(kbService.publishArticle(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ENGINEER') or hasRole('ADMIN')")
    public ResponseEntity<KbArticleResponse> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String title = body.get("title");
        String content = body.get("content");
        return ResponseEntity.ok(kbService.updateArticle(id, title, content));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ENGINEER') or hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        kbService.deleteArticle(id);
        return ResponseEntity.noContent().build();
    }
}
