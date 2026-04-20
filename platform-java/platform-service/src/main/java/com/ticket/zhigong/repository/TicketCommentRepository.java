package com.ticket.zhigong.repository;

import com.ticket.zhigong.entity.TicketComment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {

    @EntityGraph(attributePaths = {"author"})
    List<TicketComment> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}
