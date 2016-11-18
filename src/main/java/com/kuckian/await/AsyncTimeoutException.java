package com.kuckian.await;

@SuppressWarnings("serial")
public class AsyncTimeoutException extends AsyncException {
	public AsyncTimeoutException(String message) {
		super(message);
	}
}