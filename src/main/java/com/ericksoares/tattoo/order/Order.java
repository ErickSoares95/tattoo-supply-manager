package com.ericksoares.tattoo.order;

import com.ericksoares.tattoo.shared.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
class Order extends BaseEntity {

    // O2: listing a page of orders loads the items of up to 50 orders in one IN query instead
    // of one query per order. (@EntityGraph would paginate in memory with a collection join.)
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<OrderItem> items;

    private BigDecimal total;

    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;

    // Paying and cancelling update this row; the version makes the later of two concurrent
    // updates fail instead of an order ending up both PAID and CANCELLED.
    @Version
    private Long version;

    // O5: client-chosen key (Idempotency-Key header), unique per user (V4).
    @Column(length = 100)
    private String idempotencyKey;

    public void setItems(List<OrderItem> items) {
        // Copied: Hibernate replaces the elements of this collection on merge, which fails on
        // an immutable list (List.of) handed in by the caller.
        this.items = items == null ? null : new ArrayList<>(items);

        if (items != null) {
            for (OrderItem item : items) {
                item.setOrder(this);
            }
        }
    }

    public void calculateTotal() {
        this.total = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void markPaid() {
        if (status == OrderStatus.CANCELLED) {
            throw new OrderNotPayableException(getId(), status);
        }
        status = OrderStatus.PAID;
    }

    public void cancel() {
        if (status != OrderStatus.PENDING_PAYMENT) {
            throw new OrderNotCancellableException(getId(), status);
        }
        status = OrderStatus.CANCELLED;
    }

    public void validate() {
        if (items == null || items.isEmpty()) {
            throw new EmptyOrderException();
        }
    }
}