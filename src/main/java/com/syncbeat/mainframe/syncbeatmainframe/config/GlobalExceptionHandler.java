package com.syncbeat.mainframe.syncbeatmainframe.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ResponseEntity<?> handleValidationException(MethodArgumentNotValidException ex) {

		Map<String, String> errors = new HashMap<>();

		ex.getBindingResult().getFieldErrors().forEach(error ->
				errors.put(error.getField(), error.getDefaultMessage())
		);

		return ResponseEntity
				.badRequest()
				.body(Map.of(
						"status", 400,
						"message", "Validation failed",
						"errors", errors
				));
	}

	// Handles malformed JSON or invalid request body
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<?> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
		return ResponseEntity
				.badRequest()
				.body(Map.of(
						"status", 400,
						"message", "Invalid request body or wrong data format"
				));
	}

	@ExceptionHandler(NullPointerException.class)
	public ResponseEntity<?> handleNullPointerExceptions(NullPointerException ex) {
		return ResponseEntity
				.badRequest()
				.body(Map.of(
						"status", 400,
						"message", "Null pointer exception"
				));
	}

}
