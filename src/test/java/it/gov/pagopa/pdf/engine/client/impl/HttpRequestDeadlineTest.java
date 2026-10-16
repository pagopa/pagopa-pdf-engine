package it.gov.pagopa.pdf.engine.client.impl;

import org.junit.jupiter.api.Test;

import java.net.SocketTimeoutException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class HttpRequestDeadlineTest {

	@Test
	void completionBeforeDeadlinePreventsLateAbort() throws Exception {
		Fixture fixture = new Fixture();
		try (HttpRequestDeadline deadline = fixture.deadline) {
			fixture.clock.set(TimeUnit.MILLISECONDS.toNanos(50));
			deadline.complete();

			fixture.clock.set(TimeUnit.MILLISECONDS.toNanos(200));
			fixture.timer.get().run();

			assertFalse(deadline.isExpired());
			assertEquals(0, fixture.aborts.get());
			verify(fixture.task).cancel(false);
		}
	}

	@Test
	void expirationAbortsOnceAndRejectsCompletion() {
		Fixture fixture = new Fixture();
		try (HttpRequestDeadline deadline = fixture.deadline) {
			fixture.clock.set(TimeUnit.MILLISECONDS.toNanos(101));
			fixture.timer.get().run();
			fixture.timer.get().run();

			assertTrue(deadline.isExpired());
			assertEquals(1, fixture.aborts.get());
			assertThrows(SocketTimeoutException.class, deadline::complete);
		}
	}

	@Test
	void elapsedBudgetIsDetectedEvenWhenTimerHasNotRun() {
		Fixture fixture = new Fixture();
		try (HttpRequestDeadline deadline = fixture.deadline) {
			fixture.clock.set(TimeUnit.MILLISECONDS.toNanos(101));

			// Deliberately do not execute the scheduled timer.
			assertThrows(SocketTimeoutException.class, deadline::complete);
			assertEquals(1, fixture.aborts.get());

			fixture.timer.get().run();
			assertEquals(1, fixture.aborts.get());
		}
	}

	@Test
	void closingPreventsScheduledAbort() {
		Fixture fixture = new Fixture();
		fixture.deadline.close();

		fixture.clock.set(TimeUnit.MILLISECONDS.toNanos(200));
		fixture.timer.get().run();

		assertEquals(0, fixture.aborts.get());
		assertFalse(fixture.deadline.isExpired());
		verify(fixture.task).cancel(false);
	}

	@Test
	void expirationWakesBackoffWaitWithoutInterruptingCaller() throws Exception {
		Fixture fixture = new Fixture();
		AtomicReference<Boolean> result = new AtomicReference<>();
		AtomicReference<Throwable> failure = new AtomicReference<>();
		AtomicBoolean interrupted = new AtomicBoolean();

		Thread worker = new Thread(() -> {
			try {
				result.set(fixture.deadline.awaitRetryDelay(5000));
			} catch (Throwable e) {
				failure.set(e);
			} finally {
				interrupted.set(Thread.currentThread().isInterrupted());
			}
		}, "deadline-backoff-test");

		try (HttpRequestDeadline deadline = fixture.deadline) {
			worker.start();

			long waitLimit = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
			while (worker.isAlive() && worker.getState() != Thread.State.TIMED_WAITING
					&& System.nanoTime() < waitLimit) {
				Thread.sleep(1);
			}
			assertEquals(Thread.State.TIMED_WAITING, worker.getState());

			fixture.clock.set(TimeUnit.MILLISECONDS.toNanos(101));
			fixture.timer.get().run();
			worker.join(1000);

			assertFalse(worker.isAlive());
			assertNull(failure.get());
			assertEquals(Boolean.FALSE, result.get());
			assertFalse(interrupted.get());
			assertEquals(1, fixture.aborts.get());
		} finally {
			worker.interrupt();
			worker.join(1000);
		}
	}

	private static final class Fixture {
		final AtomicLong clock = new AtomicLong();
		final AtomicInteger aborts = new AtomicInteger();
		final AtomicReference<Runnable> timer = new AtomicReference<>();
		final ScheduledFuture<?> task = mock(ScheduledFuture.class);
		final HttpRequestDeadline deadline;

		Fixture() {
			ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
			when(executor.schedule(any(Runnable.class), eq(100L), eq(TimeUnit.MILLISECONDS))).thenAnswer(invocation -> {
				timer.set(invocation.getArgument(0));
				return task;
			});

			deadline = new HttpRequestDeadline(executor, aborts::incrementAndGet, 100, clock::get);
		}
	}
}