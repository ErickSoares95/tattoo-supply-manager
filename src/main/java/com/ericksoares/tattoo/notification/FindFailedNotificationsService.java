package com.ericksoares.tattoo.notification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * N11: before this, a failure parked as terminal (permanent error, attempts exhausted) was only
 * visible in the database. GET /notifications/failed lists unprocessed order and payment
 * failures together, newest first, optionally only the parked ones.
 */
@Service
@Transactional(readOnly = true)
class FindFailedNotificationsService {

    static final int LIMIT = 200;

    private final FailedNotificationRepository orderFailures;
    private final FailedPaymentNotificationRepository paymentFailures;

    FindFailedNotificationsService(FailedNotificationRepository orderFailures,
                                   FailedPaymentNotificationRepository paymentFailures) {
        this.orderFailures = orderFailures;
        this.paymentFailures = paymentFailures;
    }

    public List<FailedNotificationView> execute(boolean parkedOnly) {
        Stream<FailedNotificationView> orders = orderFailures.findByProcessedFalse().stream()
                .map(f -> view(f.getId(), "ORDER", f.getOrderId(), f.getSenderName(), f.getAttempts(),
                        f.getNextAttemptAt(), f.getErrorMessage(), f.getCreatedAt()));
        Stream<FailedNotificationView> payments = paymentFailures.findByProcessedFalse().stream()
                .map(f -> view(f.getId(), "PAYMENT", f.getOrderId(), f.getSenderName(), f.getAttempts(),
                        f.getNextAttemptAt(), f.getErrorMessage(), f.getCreatedAt()));

        return Stream.concat(orders, payments)
                .filter(view -> !parkedOnly || view.parked())
                .sorted(Comparator.comparing(FailedNotificationView::createdAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(LIMIT)
                .toList();
    }

    private static FailedNotificationView view(Long id, String type, Long orderId, String sender, Integer attempts,
                                               java.time.Instant nextAttemptAt, String error,
                                               java.time.LocalDateTime createdAt) {
        int tries = attempts == null ? 0 : attempts;
        boolean parked = NotificationRetryPolicy.isExhausted(tries) && nextAttemptAt == null;
        return new FailedNotificationView(id, type, orderId, sender, tries, NotificationRetryPolicy.MAX_ATTEMPTS,
                nextAttemptAt, parked, error, createdAt);
    }
}
