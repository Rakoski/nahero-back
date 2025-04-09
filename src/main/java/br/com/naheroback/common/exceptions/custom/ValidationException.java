package br.com.naheroback.common.exceptions.custom;

public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
