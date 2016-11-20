package com.kuckian.await.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.kuckian.await.Async;
import com.kuckian.await.AsyncTimeoutException;

/* @author Mark Cowan, Open Cosmos, mark@battlesnake.co.uk */
public class AsyncAwaitTest {

	private class AsyncReject extends Thread {
		private final int key;
		private final Exception err;

		public AsyncReject(int key, Exception err) {
			this.key = key;
			this.err = err;
			start();
		}

		@Override
		public void run() {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}
			async.reject(key, err);
		}
	}

	private class AsyncResolve extends Thread {
		private final int key;
		private final int value;

		public AsyncResolve(int key, int value) {
			this.key = key;
			this.value = value;
			start();
		}

		@Override
		public void run() {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
				return;
			}
			async.resolve(key, value);
		}
	}

	@SuppressWarnings("serial")
	private static class TestException extends Exception {
		public TestException() {
		}
	}

	private Async<Integer, Integer> async;
	private Thread thread = null;

	@Test
	public void awaitTimeout() throws AsyncTimeoutException, Exception {
		boolean threw = false;
		try {
			async.await(1, 100, TimeUnit.MILLISECONDS);
		} catch (AsyncTimeoutException e) {
			threw = true;
		}
		assertTrue("Await throws AsyncTimeoutException on timeout", threw);
		assertFalse("Key is cleared after timeout", async.resolve(1, 0));
	}

	@Test
	public void lateCompletion() throws AsyncTimeoutException, Exception {
		boolean threw = false;
		new AsyncResolve(1, 42);
		try {
			async.await(1, 10, TimeUnit.MILLISECONDS);
		} catch (AsyncTimeoutException e) {
			threw = true;
		}
		assertTrue("Operation timed out", threw);
		try {
			Thread.sleep(150);
		} catch (InterruptedException e) {
			/* Om nom nom */
		}
		threw = false;
		try {
			async.await(1, 0, TimeUnit.MILLISECONDS);
		} catch (AsyncTimeoutException e) {
			threw = true;
		}
		assertTrue("Late completion does not leak resources in the callback table", threw);
	}

	@Test
	public void rejectMissing() throws AsyncTimeoutException, Exception {
		assertFalse("Reject returns false if key isn't found", async.reject(1, new Exception("")));
	}

	@Test
	public void rejects() throws AsyncTimeoutException, Exception {
		boolean threw = false;
		thread = new AsyncReject(1, new TestException());
		try {
			async.await(1, 1, TimeUnit.SECONDS);
		} catch (TestException e) {
			threw = true;
		}
		assertTrue("Rejected request throws exception from await", threw);
	}

	@Test
	public void resolveMissing() throws AsyncTimeoutException, Exception {
		assertFalse("Resolve returns false if key isn't found", async.resolve(1, 0));
	}

	@Test
	public void resolves() throws AsyncTimeoutException, Exception {
		thread = new AsyncResolve(1, 42);
		assertTrue("Resolved request returns right value", async.await(1, 1, TimeUnit.SECONDS) == 42);
	}

	@Before
	public void setUp() throws Exception {
		thread = null;
		async = new Async<>();
	}

	@After
	public void tearDown() throws Exception {
		if (thread != null) {
			thread.interrupt();
			thread.join();
			thread = null;
		}
		async = null;
	}

}
