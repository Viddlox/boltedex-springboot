package com.example.boltedex.exception;

import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.ResponseEntity;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {
	private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(APIException.class)
	public ResponseEntity<APIException> handleAPIException(APIException ex) {
		logger.error("API Exception occurred: {}", ex.getMessage(), ex);
		return ResponseEntity.status(ex.getStatusCode()).body(ex);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<APIException> handleGenericException(Exception ex) {
		logger.error("Unexpected error occurred: {}", ex.getMessage(), ex);
		APIException error = new APIException(
				ExceptionConstants.GENERIC_ERROR_MESSAGE,
				ExceptionConstants.INTERNAL_ERROR,
				ExceptionConstants.INTERNAL_SERVER_ERROR,
				Instant.now().toString(),
				ex
		);
		return ResponseEntity.status(ExceptionConstants.INTERNAL_SERVER_ERROR).body(error);
	}
}
