package com.ericksoares.tattoo.payment;

import com.ericksoares.tattoo.order.OrderSummary;
import com.ericksoares.tattoo.order.OrderLookup;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
class ProcessPaymentService {

    static final String SINGLE_APPROVED_CONSTRAINT = "uk_payments_order_approved";

    private final OrderLookup orderLookup;
    private final PaymentRepository paymentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ProcessPaymentService(
            OrderLookup orderLookup,
            PaymentRepository paymentRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.orderLookup = orderLookup;
        this.paymentRepository = paymentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public PaymentResponse execute(Long orderId, PaymentRequest request, Long userId, boolean isAdmin) {

        OrderSummary order = orderLookup.findSummary(orderId);

        if (!isAdmin && !order.userId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to pay for this order");
        }

        if (paymentRepository.existsByOrderIdAndStatus(orderId, PaymentStatus.APPROVED)) {
            throw new OrderAlreadyPaidException(orderId);
        }

        Payment payment = Payment.builder()
                .orderId(orderId)
                .amount(request.amount())
                .method(request.method())
                .status(PaymentStatus.PENDING)
                .build();

        payment.decide(order.total());

        Payment saved = saveGuardingSingleApproval(payment);

        log.info("Payment {} for order {} processed with status {}", saved.getId(), orderId, saved.getStatus());

        PaymentProcessedEvent event = new PaymentProcessedEvent(
                UUID.randomUUID(),
                orderId,
                saved.getStatus(),
                saved.getAmount(),
                LocalDateTime.now()
        );

        // In-process event: the notification listener runs AFTER_COMMIT, so a payment whose
        // transaction rolls back never notifies anyone. eventId is the correlation id stored on
        // failed_payment_notifications rows.
        eventPublisher.publishEvent(event);

        return PaymentMapper.toResponse(saved);
    }

    /**
     * The existsBy check above is only the fast path: two concurrent requests can both pass it.
     * The partial unique index uk_payments_order_approved (migration V2) rejects the second
     * APPROVED insert; flushing here surfaces that inside this method (instead of at commit) so
     * it becomes the same 409 as the check, and the event below is never published.
     */
    private Payment saveGuardingSingleApproval(Payment payment) {
        try {
            return paymentRepository.saveAndFlush(payment);
        } catch (DataIntegrityViolationException ex) {
            if (ex.getCause() instanceof ConstraintViolationException violation
                    && SINGLE_APPROVED_CONSTRAINT.equals(violation.getConstraintName())) {
                throw new OrderAlreadyPaidException(payment.getOrderId());
            }
            throw ex;
        }
    }
}
