package br.com.naheroback.common.exceptions.custom;

import br.com.naheroback.common.entities.BaseEntity;

public class DuplicateException extends RuntimeException {

    protected DuplicateException(final String message) {
        super(message);
    }

    public static DuplicateException with(
            final Class<? extends BaseEntity> entity,
            final String propertyName,
            final Object propertyValue) {
        final var errorMessage = "%s with %s '%s' already exists".formatted(
                entity.getSimpleName(),
                propertyName,
                propertyValue);
        return new DuplicateException(errorMessage);
    }

}
