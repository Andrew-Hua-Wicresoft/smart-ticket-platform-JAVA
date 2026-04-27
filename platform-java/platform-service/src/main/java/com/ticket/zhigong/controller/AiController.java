package com.ticket.zhigong.controller;

import com.ticket.zhigong.dto.AiRefineRequest;
import com.ticket.zhigong.dto.AiSearchRequest;
import com.ticket.zhigong.dto.AiSearchResult;
import com.ticket.zhigong.dto.AiSuggestionSnapshot;
import com.ticket.zhigong.dto.AiSuggestRequest;
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
    public ResponseEntity<Map<String, String>> refine(@Valid @RequestBody AiRefineRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        String result = aiService.refine(request.getTitle(), request.getDescription(), userId);
        return ResponseEntity.ok(Map.of("questions", result != null ? result : "AI暂时不可用"));
    }

    @PostMapping("/suggest")
    public ResponseEntity<Map<String, String>> suggest(@Valid @RequestBody AiSuggestRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        String result = aiService.suggest(request.getTicketId(), request.getTitle(), request.getDescription(), userId);
        return ResponseEntity.ok(Map.of("suggestion", result != null ? result : "AI暂时不可用"));
    }

    @GetMapping("/suggest/latest")
    public ResponseEntity<AiSuggestionSnapshot> latestSuggestion(@RequestParam Long ticketId) {
        return ResponseEntity.ok(aiService.latestSuggestion(ticketId));
    }

    @PostMapping("/similar")
    public ResponseEntity<List<AiSearchResult>> findSimilar(@Valid @RequestBody AiSearchRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<AiSearchResult> results = aiService.findSimilarTickets(request.getQuery(), userId);
        return ResponseEntity.ok(results);
    }
}
