package com.ticket.zhigong.controller;

import com.ticket.zhigong.dto.AiSearchRequest;
import com.ticket.zhigong.dto.AiSearchResult;
import com.ticket.zhigong.security.SecurityUtils;
import com.ticket.zhigong.service.AiService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/search")
    public ResponseEntity<List<AiSearchResult>> search(@Valid @RequestBody AiSearchRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<AiSearchResult> results = aiService.search(request.getQuery(), request.getTopK(), userId);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/refine")
    public ResponseEntity<Map<String, String>> refine(@RequestBody Map<String, String> request) {
        Long userId = SecurityUtils.getCurrentUserId();
        String title = request.getOrDefault("title", "");
        String description = request.getOrDefault("description", "");
        String result = aiService.refine(title, description, userId);
        return ResponseEntity.ok(Map.of("questions", result != null ? result : "AI暂时不可用"));
    }

    @PostMapping("/suggest")
    public ResponseEntity<Map<String, String>> suggest(@RequestBody Map<String, Object> request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long ticketId = request.get("ticketId") != null ? Long.valueOf(request.get("ticketId").toString()) : null;
        String title = (String) request.getOrDefault("title", "");
        String description = (String) request.getOrDefault("description", "");
        String result = aiService.suggest(ticketId, title, description, userId);
        return ResponseEntity.ok(Map.of("suggestion", result != null ? result : "AI暂时不可用"));
    }

    @PostMapping("/similar")
    public ResponseEntity<List<AiSearchResult>> findSimilar(@Valid @RequestBody AiSearchRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<AiSearchResult> results = aiService.findSimilarTickets(request.getQuery(), userId);
        return ResponseEntity.ok(results);
    }
}
