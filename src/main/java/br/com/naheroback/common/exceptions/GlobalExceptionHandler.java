package br.com.naheroback.common.exceptions;

import br.com.naheroback.common.exceptions.custom.DuplicateException;
import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.common.exceptions.custom.UnprocessableEntityException;
import br.com.naheroback.common.exceptions.custom.ValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;
import java.time.Instant;

@ControllerAdvice
@RequiredArgsConstructor
@Slf4j(topic = "GLOBAL_EXCEPTION_HANDLER")
public class GlobalExceptionHandler {
    private final ObjectMapper objectMapper;
    private final MessageSource messageSource;

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

    @ExceptionHandler(UnprocessableEntityException.class)
    protected ResponseEntity<CustomException> unprocessableEntity(UnprocessableEntityException e, HttpServletRequest request) {
        String resolved = messageSource.getMessage(
                e.getMessageKey(),
                e.getArgs(),
                e.getMessageKey(),
                LocaleContextHolder.getLocale()
        );

        var exception = CustomException.builder()
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .timestamp(Instant.now())
                .error(resolved)
                .path(request.getRequestURI())
                .build();

        log.error("UnprocessableEntityException: {} - Path: {}", exception.getError(), exception.getPath());

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(exception);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<CustomException> methodArgumentNotValid(MethodArgumentNotValidException e, HttpServletRequest request) {
        var exception = CustomException.builder()
                .status(HttpStatus.BAD_REQUEST)
                .timestamp(Instant.now())
                .error(e.getMessage())
                .path(request.getRequestURI())
                .build();

        log.error("MethodArgumentNotValidException: {} - Path: {}", exception.getError(), exception.getPath());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<CustomException> methodNotSupported(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        var exception = CustomException.builder()
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .timestamp(Instant.now())
                .error(e.getMessage())
                .path(request.getRequestURI())
                .build();

        log.error("HttpRequestMethodNotSupportedException: {} - Path: {}", exception.getError(), exception.getPath());

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(exception);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<CustomException> methodArgumentTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String paramName = e.getName();
        String requiredType = e.getRequiredType() != null ? e.getRequiredType().getSimpleName() : "unknown";
        String errorMessage = String.format("Invalid value for parameter '%s'. Expected type: %s", paramName, requiredType);

        var exception = CustomException.builder()
                .status(HttpStatus.BAD_REQUEST)
                .timestamp(Instant.now())
                .error(errorMessage)
                .path(request.getRequestURI())
                .build();

        log.error("MethodArgumentTypeMismatchException: {} - Path: {}", exception.getError(), exception.getPath());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception);
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
