package com.ericksoares.tattoo.payment.application.service;

import com.ericksoares.tattoo.order.application.dto.OrderSummary;
import com.ericksoares.tattoo.order.application.service.OrderLookup;
import com.ericksoares.tattoo.payment.application.dto.PaymentRequest;
import com.ericksoares.tattoo.payment.application.dto.PaymentResponse;
import com.ericksoares.tattoo.payment.application.mapper.PaymentMapper;
import com.ericksoares.tattoo.payment.domain.entity.Payment;
import com.ericksoares.tattoo.payment.domain.entity.PaymentStatus;
import com.ericksoares.tattoo.payment.domain.event.PaymentProcessedEvent;
import com.ericksoares.tattoo.payment.domain.exception.OrderAlreadyPaidException;
import com.ericksoares.tattoo.payment.infrastructure.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class ProcessPaymentService {

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

        Payment saved = paymentRepository.save(payment);

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
}
