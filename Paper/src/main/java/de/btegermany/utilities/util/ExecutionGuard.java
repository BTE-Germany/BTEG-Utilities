package de.btegermany.utilities.util;

import java.time.Duration;
import java.util.Objects;

/** Cooperative time limit for long-running command calculations. */
public final class ExecutionGuard implements AutoCloseable {
    private static final ThreadLocal<ExecutionGuard> CURRENT = new ThreadLocal<>();

    private final ExecutionGuard previous;
    private final long deadlineNanos;
    private boolean closed;

    private ExecutionGuard(Duration limit) {
        if (limit.isNegative() || limit.isZero()) {
            throw new IllegalArgumentException("Execution limit must be positive");
        }
        previous = CURRENT.get();
        deadlineNanos = System.nanoTime() + limit.toNanos();
        CURRENT.set(this);
    }

    /** Starts a time-limited scope. Close it with try-with-resources. */
    public static ExecutionGuard start(Duration limit) {
        return new ExecutionGuard(Objects.requireNonNull(limit, "limit"));
    }

    /** Aborts the current calculation once its time limit has elapsed. */
    public static void check() {
        ExecutionGuard guard = CURRENT.get();
        if (guard != null && System.nanoTime() - guard.deadlineNanos >= 0) {
            throw new TimeoutException();
        }
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        if (previous == null) CURRENT.remove();
        else CURRENT.set(previous);
    }

    /** Signals that a guarded calculation exceeded its time limit. */
    public static final class TimeoutException extends RuntimeException {
        public TimeoutException() {
            super("Execution time limit exceeded");
        }
    }
}
