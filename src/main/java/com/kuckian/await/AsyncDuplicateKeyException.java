package com.kuckian.await;

@SuppressWarnings("serial")
public class AsyncDuplicateKeyException extends AsyncException {
	public AsyncDuplicateKeyException(String message) {
		super(message);
	}
}
