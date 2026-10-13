package it.gov.pagopa.pdf.engine.client.impl;

import java.net.SocketTimeoutException;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

/**
 * Tracks one HTTP exchange's time budget and requests cancellation when it
 * expires. Also wakes retry waits at expiry, without interrupting the caller
 * thread.
 */
final class HttpRequestDeadline implements AutoCloseable {

	private static final int ACTIVE = 0;
	private static final int COMPLETED = 1;
	private static final int EXPIRED = 2;

	private final AtomicInteger state = new AtomicInteger(ACTIVE);
	private final Object waitMonitor = new Object();
	private final ScheduledFuture<?> task;
	private final Runnable abort;
	private final int timeoutMs;
	private final LongSupplier nanoTime;
	private final long startedNanos;
	private final long timeoutNanos;

	HttpRequestDeadline(ScheduledExecutorService executor, Runnable abort, int timeoutMs) {
		this(executor, abort, timeoutMs, System::nanoTime);
	}

	HttpRequestDeadline(ScheduledExecutorService executor, Runnable abort, int timeoutMs, LongSupplier nanoTime) {
		if (timeoutMs <= 0) {
			throw new IllegalArgumentException("timeoutMs must be greater than zero");
		}
		this.abort = Objects.requireNonNull(abort);
		this.timeoutMs = timeoutMs;
		this.nanoTime = Objects.requireNonNull(nanoTime);
		this.startedNanos = nanoTime.getAsLong();
		this.timeoutNanos = TimeUnit.MILLISECONDS.toNanos(timeoutMs);
		this.task = executor.schedule(this::expire, timeoutMs, TimeUnit.MILLISECONDS);
	}

	boolean isExpired() {
		// Check elapsed time even if the scheduled cancellation task is delayed.
		if (state.get() == ACTIVE && nanoTime.getAsLong() - startedNanos >= timeoutNanos) {
			expire();
		}
		return state.get() == EXPIRED;
	}

	private void expire() {
		if (state.compareAndSet(ACTIVE, EXPIRED)) {
			wakeWaiters();
			abort.run();
		}
	}

	// Returns false when the deadline expires or is closed while waiting.
	boolean awaitRetryDelay(long delayMs) throws InterruptedException {
		if (delayMs < 0) {
			throw new IllegalArgumentException("delayMs must not be negative");
		}
		long waitStartedNanos = nanoTime.getAsLong();
		long delayNanos = TimeUnit.MILLISECONDS.toNanos(delayMs);

		synchronized (waitMonitor) {
			while (state.get() == ACTIVE) {
				if (isExpired()) {
					return false;
				}
				long now = nanoTime.getAsLong();
				long delayRemaining = delayNanos - (now - waitStartedNanos);
				if (delayRemaining <= 0) {
					return !isExpired() && state.get() == ACTIVE;
				}
				long budgetRemaining = timeoutNanos - (now - startedNanos);
				if (budgetRemaining <= 0) {
					expire();
					return false;
				}
				TimeUnit.NANOSECONDS.timedWait(waitMonitor, Math.min(delayRemaining, budgetRemaining));
			}
			return false;
		}
	}

	// Marks success only if expiry has not already won.
	void complete() throws SocketTimeoutException {
		isExpired();
		state.compareAndSet(ACTIVE, COMPLETED);
		task.cancel(false);
		wakeWaiters();
		if (state.get() == EXPIRED) {
			throw new SocketTimeoutException("PDF Engine HTTP request exceeded " + timeoutMs + " ms");
		}
	}

	private void wakeWaiters() {
		synchronized (waitMonitor) {
			waitMonitor.notifyAll();
		}
	}

	// Cancels the timer on every exit path, including exceptions.
	@Override
	public void close() {
		state.compareAndSet(ACTIVE, COMPLETED);
		task.cancel(false);
		wakeWaiters();
	}
}