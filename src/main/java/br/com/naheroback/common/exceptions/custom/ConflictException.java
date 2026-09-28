package br.com.naheroback.common.exceptions.custom;

import lombok.Getter;

@Getter
public class ConflictException extends RuntimeException {
    private final String messageKey;
    private final String errorCode;
    private final Object[] args;

    public ConflictException(String messageKey, String errorCode, Object... args) {
        super(messageKey);
        this.messageKey = messageKey;
        this.errorCode = errorCode;
        this.args = args;
    }
}
