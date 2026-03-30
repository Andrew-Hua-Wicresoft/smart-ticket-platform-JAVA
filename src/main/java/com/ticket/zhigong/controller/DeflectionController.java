package com.ticket.zhigong.controller;

import com.ticket.zhigong.dto.DeflectionRequest;
import com.ticket.zhigong.entity.Deflection;
import com.ticket.zhigong.repository.DeflectionRepository;
import com.ticket.zhigong.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/deflections")
public class DeflectionController {

    private final DeflectionRepository deflectionRepository;

    public DeflectionController(DeflectionRepository deflectionRepository) {
        this.deflectionRepository = deflectionRepository;
    }

    @PostMapping
    public ResponseEntity<Void> logDeflection(@RequestBody DeflectionRequest request) {
        Deflection deflection = new Deflection();
        deflection.setUserId(SecurityUtils.getCurrentUserId());
        deflection.setMatchedKbId(request.getMatchedKbId());
        deflection.setSearchQuery(request.getSearchQuery());
        deflectionRepository.save(deflection);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
