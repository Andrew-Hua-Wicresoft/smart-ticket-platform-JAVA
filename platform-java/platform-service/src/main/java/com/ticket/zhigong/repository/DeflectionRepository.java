package com.ticket.zhigong.repository;

import com.ticket.zhigong.entity.Deflection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeflectionRepository extends JpaRepository<Deflection, Long> {
    long countByUserIdIsNotNull();
}
