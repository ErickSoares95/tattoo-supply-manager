package com.ericksoares.tattoo.shared.config;

/**
 * A task that knows how to save its own work when the executor refuses it (queue full or
 * executor shut down), so the event it carries is not lost. See {@link AsyncConfig}.
 */
public interface RejectionAwareTask extends Runnable {

    /**
     * Called on the submitting thread instead of {@link #run()} when the executor rejects the task.
     * Must not throw.
     */
    void onRejected();
}
