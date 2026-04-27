package com.ticket.zhigong.repository;

import com.ticket.zhigong.entity.AiInteraction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiInteractionRepository extends JpaRepository<AiInteraction, Long> {

    Optional<AiInteraction> findFirstByTicketIdAndTypeAndSuccessTrueOrderByCreatedAtDesc(Long ticketId, String type);
}
