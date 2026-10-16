package com.ericksoares.tattoo.notification.domain.repository;

import com.ericksoares.tattoo.notification.domain.entity.ProcessedPaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface ProcessedPaymentEventRepository extends JpaRepository<ProcessedPaymentEvent, Long> {

    /**
     * Gives the claim of an event back (own transaction) so a redelivery can process it again;
     * used when the failure bookkeeping itself failed and the event would otherwise be lost.
     */
    @Transactional
    @Modifying
    @Query("delete from ProcessedPaymentEvent p where p.eventId = :eventId")
    int releaseClaim(@Param("eventId") UUID eventId);
}
