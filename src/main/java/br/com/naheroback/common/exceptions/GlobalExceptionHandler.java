package br.com.naheroback.common.exceptions;

import br.com.naheroback.common.exceptions.custom.DuplicateException;
import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.common.exceptions.custom.ValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;
import java.time.Instant;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j(topic = "GLOBAL_EXCEPTION_HANDLER")
public class GlobalExceptionHandler {
    private final ObjectMapper objectMapper;

    @ExceptionHandler(NotFoundException.class)
    protected ResponseEntity<CustomException> resourceNotFound(RuntimeException e, HttpServletRequest request) {
        var exception = CustomException.builder()
                .status(HttpStatus.NOT_FOUND)
                .timestamp(Instant.now())
                .error(e.getMessage())
                .path(request.getRequestURI())
                .build();

        log.error("NotFoundException: {} - Path: {}", exception.getError(), exception.getPath());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception);
    }

    @ExceptionHandler(DuplicateException.class)
    protected ResponseEntity<CustomException> duplicatedResource(RuntimeException e, HttpServletRequest request) {
        var exception = CustomException.builder()
                .status(HttpStatus.CONFLICT)
                .timestamp(Instant.now())
                .error(e.getMessage())
                .path(request.getRequestURI())
                .build();

        log.error("DuplicateException: {} - Path: {}", exception.getError(), exception.getPath());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(exception);
    }

    @ExceptionHandler(ValidationException.class)
    protected ResponseEntity<CustomException> validationImpediment(RuntimeException e, HttpServletRequest request) {
        var exception = CustomException.builder()
                .status(HttpStatus.PRECONDITION_FAILED)
                .timestamp(Instant.now())
                .error(e.getMessage())
                .path(request.getRequestURI())
                .build();

        log.error("ValidationException: {} - Path: {}", exception.getError(), exception.getPath());

        return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).body(exception);
    }

    public void sendErrorResponse(HttpServletResponse response, HttpStatus status, String error, String path)
            throws IOException {
        response.setStatus(status.value());
        CustomException customException = CustomException.builder()
                .timestamp(Instant.now())
                .status(status)
                .error(error)
                .path(path)
                .build();
        response.setContentType("application/json");
        objectMapper.writeValue(response.getWriter(), customException);
    }
}
