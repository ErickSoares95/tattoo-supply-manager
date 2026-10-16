package com.ericksoares.tattoo.notification;

import com.ericksoares.tattoo.notification.sender.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

@Slf4j
@Service
class NotificationService {

    private final List<NotificationSender> senders;
    private final NotificationSenderExecutor executor;

    public NotificationService(List<NotificationSender> senders, NotificationSenderExecutor executor) {
        this.senders = senders;
        this.executor = executor;
    }

    /** Names of all registered channels, one pending failure per name when a task is rejected. */
    public List<String> senderNames() {
        return senders.stream().map(NotificationSender::name).toList();
    }

    /**
     * Sends the "order registered" notification through every sender and reports which ones
     * failed (empty list = all delivered), so the caller can persist one failure per sender.
     */
    public List<SenderFailure> notifyOrderRegistered(NotificationContext context) {

        log.info(
                "Sending notification for order {}",
                context.orderId()
        );

        List<SenderFailure> failures = new ArrayList<>();

        for (NotificationSender sender : senders) {
            sendTo(sender, context).ifPresent(failures::add);
        }

        return failures;
    }

    /**
     * Targeted resend: only the given sender (used to replay a failure on the channel that failed).
     */
    public Optional<SenderFailure> notifyOrderRegistered(NotificationContext context, String senderName) {

        log.info(
                "Sending notification for order {} through sender {}",
                context.orderId(),
                senderName
        );

        return senders.stream()
                .filter(sender -> sender.name().equals(senderName))
                .findFirst()
                .map(sender -> sendTo(sender, context))
                .orElseGet(() -> Optional.of(new SenderFailure(
                        senderName,
                        new PermanentNotificationException("No sender registered with name " + senderName)
                )));
    }

    private Optional<SenderFailure> sendTo(NotificationSender sender, NotificationContext context) {
        try {
            executor.send(sender, context.orderId());
            return Optional.empty();
        } catch (Exception e) {
            log.error(
                    "Delivery failed (persisted for retry) - sender: {}, orderId: {}",
                    sender.name(),
                    context.orderId(),
                    e
            );
            return Optional.of(new SenderFailure(sender.name(), e));
        }
    }

    public List<SenderFailure> notifyPaymentConfirmed(Long orderId) {

        log.info("Sending payment confirmed notification for order {}", orderId);

        return sendToAll(orderId, "payment confirmed", executor::sendPaymentConfirmed);
    }

    /** Targeted resend of "payment confirmed": only the given sender. */
    public Optional<SenderFailure> notifyPaymentConfirmed(Long orderId, String senderName) {

        log.info("Sending payment confirmed notification for order {} through sender {}", orderId, senderName);

        return sendTo(senderName, orderId, "payment confirmed", executor::sendPaymentConfirmed);
    }

    public List<SenderFailure> notifyPaymentRejected(Long orderId) {

        log.info("Sending payment rejected notification for order {}", orderId);

        return sendToAll(orderId, "payment rejected", executor::sendPaymentRejected);
    }

    /** Targeted resend of "payment rejected": only the given sender. */
    public Optional<SenderFailure> notifyPaymentRejected(Long orderId, String senderName) {

        log.info("Sending payment rejected notification for order {} through sender {}", orderId, senderName);

        return sendTo(senderName, orderId, "payment rejected", executor::sendPaymentRejected);
    }

    private List<SenderFailure> sendToAll(Long orderId, String event, BiConsumer<NotificationSender, Long> delivery) {

        List<SenderFailure> failures = new ArrayList<>();

        for (NotificationSender sender : senders) {
            attempt(sender, orderId, event, delivery).ifPresent(failures::add);
        }

        return failures;
    }

    private Optional<SenderFailure> sendTo(
            String senderName, Long orderId, String event, BiConsumer<NotificationSender, Long> delivery
    ) {
        return senders.stream()
                .filter(sender -> sender.name().equals(senderName))
                .findFirst()
                .map(sender -> attempt(sender, orderId, event, delivery))
                .orElseGet(() -> Optional.of(new SenderFailure(
                        senderName,
                        new PermanentNotificationException("No sender registered with name " + senderName)
                )));
    }

    private Optional<SenderFailure> attempt(
            NotificationSender sender, Long orderId, String event, BiConsumer<NotificationSender, Long> delivery
    ) {
        try {
            delivery.accept(sender, orderId);
            return Optional.empty();
        } catch (Exception e) {
            log.error(
                    "Delivery failed (persisted for retry) - sender: {}, orderId: {}, event: {}",
                    sender.name(),
                    orderId,
                    event,
                    e
            );
            return Optional.of(new SenderFailure(sender.name(), e));
        }
    }
}
