package com.ticket.zhigong.repository;

import com.ticket.zhigong.entity.Ticket;
import com.ticket.zhigong.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @EntityGraph(attributePaths = {"customer", "assignedEngineer"})
    Page<Ticket> findByCustomerId(Long customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "assignedEngineer"})
    Page<Ticket> findByStatusIn(List<TicketStatus> statuses, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "assignedEngineer"})
    Page<Ticket> findAll(Pageable pageable);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.status = :status")
    long countByStatus(@Param("status") TicketStatus status);

    @Query(value = "SELECT AVG(EXTRACT(EPOCH FROM (t.resolved_at - t.created_at)) / 3600) FROM tickets t WHERE t.resolved_at IS NOT NULL", nativeQuery = true)
    Double averageResolutionTimeHours();

    long countByAssignedEngineerId(Long engineerId);

    @Modifying
    @Query("UPDATE Ticket t SET t.status = 'IN_PROGRESS', t.assignedEngineer = :engineer WHERE t.id = :ticketId AND t.status = 'OPEN'")
    int assignToEngineer(@Param("ticketId") Long ticketId, @Param("engineer") com.ticket.zhigong.entity.User engineer);
}
