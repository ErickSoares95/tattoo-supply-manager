package com.ericksoares.tattoo.shared.outbox;

public enum OutboxStatus {

    /** Written inside the business transaction, not yet published to the broker. */
    PENDING,

    /** Successfully published to Kafka by the poller. */
    PUBLISHED,

    /** Gave up after {@code outbox.max-attempts} failed publish attempts - needs a human. */
    FAILED
}
