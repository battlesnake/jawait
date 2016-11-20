package com.kuckian.await;

/*
 * Mark K Cowan, mark@battlesnake.co.uk
 */
import java.util.HashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/* Make asynchronous operations look synchronous */
public class Async<KeyType, ResultType> {

	public ResultType await(KeyType key, long timeout, TimeUnit unit) throws AsyncTimeoutException, Exception {
		final MemoStruct memo = new MemoStruct();
		synchronized (list) {
			if (list.putIfAbsent(key, memo) != null) {
				throw new AsyncDuplicateKeyException("Duplicate key: " + key.toString());
			}
		}
		if (!memo.sem.tryAcquire(timeout, unit)) {
			synchronized (list) {
				list.remove(key);
			}
			throw new AsyncTimeoutException("Operation timed out");
		}
		if (memo.error != null) {
			throw memo.error;
		}
		return memo.result;
	}

	private MemoStruct completing(KeyType key) {
		MemoStruct memo;
		synchronized (list) {
			memo = list.remove(key);
		}
		return memo;
	}

	public boolean reject(KeyType key, Exception error) {
		MemoStruct memo = completing(key);
		if (memo == null) {
			return false;
		}
		memo.error = error;
		memo.sem.release();
		return true;
	}

	public boolean resolve(KeyType key, ResultType value) {
		MemoStruct memo = completing(key);
		if (memo == null) {
			return false;
		}
		memo.result = value;
		memo.sem.release();
		return true;
	}

	private class MemoStruct {
		public Semaphore sem = new Semaphore(0);
		public ResultType result = null;
		public Exception error = null;
	}

	private final HashMap<KeyType, MemoStruct> list = new HashMap<>();

}