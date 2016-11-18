package com.kuckian.await;

@SuppressWarnings("serial")
public abstract class AsyncException extends Exception {
	public AsyncException(String message) {
		super(message);
	}
}
