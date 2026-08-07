package com.openclassrooms.starterjwt.exception;

import com.openclassrooms.starterjwt.payload.response.MessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<Void> handleNumberFormatException(NumberFormatException e) {
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<?> handleBadRequestException(BadRequestException e) {
        if (e.getMessage() != null && !e.getMessage().isBlank()) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
        return ResponseEntity.badRequest().build();
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Void> handleNotFoundException(NotFoundException e) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Void> handleForbiddenException(ForbiddenException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MessageResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + " " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; ", "Error: ", ""));
        return ResponseEntity.badRequest().body(new MessageResponse(message));
    }
}