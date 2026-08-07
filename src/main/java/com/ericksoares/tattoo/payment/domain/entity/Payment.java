package com.ericksoares.tattoo.payment.domain.entity;

import com.ericksoares.tattoo.shared.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "payments")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class Payment extends BaseEntity {

    // No FK to "orders" on purpose - same cross-module decoupling already used
    // by the other modules (see the ER diagram: only order_items->orders has a real FK).
    private Long orderId;

    private BigDecimal amount;

    private String method;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    /**
     * Simulates gateway approval without an actual payment gateway integration
     * (deliberately out of scope for this increment - see project roadmap).
     * Approves only when the paid amount matches the order total exactly.
     */
    public void decide(BigDecimal orderTotal) {
        this.status = amount != null && amount.compareTo(orderTotal) == 0
                ? PaymentStatus.APPROVED
                : PaymentStatus.REJECTED;
    }

    public boolean isApproved() {
        return status == PaymentStatus.APPROVED;
    }
}
