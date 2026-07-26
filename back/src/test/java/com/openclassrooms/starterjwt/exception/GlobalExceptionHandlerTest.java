package com.openclassrooms.starterjwt.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void should_returnBadRequestWithoutBody_when_handleBadRequestExceptionIsCalled_and_messageIsNull() {
        ResponseEntity<?> response = handler.handleBadRequestException(new BadRequestException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void should_returnBadRequestWithoutBody_when_handleBadRequestExceptionIsCalled_and_messageIsBlank() {
        ResponseEntity<?> response = handler.handleBadRequestException(new BadRequestException("   "));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNull();
    }
}
