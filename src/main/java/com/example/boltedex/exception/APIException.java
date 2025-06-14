package com.example.boltedex.exception;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class APIException extends RuntimeException {
	private final String errorCode;
	private final int statusCode;
	private final String timestamp;

	public APIException(String message, String errorCode, int statusCode, String timestamp) {
		super(message);
		this.errorCode = errorCode;
		this.statusCode = statusCode;
		this.timestamp = timestamp;
	}

	public APIException(String message, String errorCode, int statusCode, String timestamp, Throwable cause) {
		super(message, cause);
		this.errorCode = errorCode;
		this.statusCode = statusCode;
		this.timestamp = timestamp;
	}
}
