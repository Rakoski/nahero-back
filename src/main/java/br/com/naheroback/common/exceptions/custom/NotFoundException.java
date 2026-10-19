package br.com.naheroback.common.exceptions.custom;

public class NotFoundException extends RuntimeException {
    public NotFoundException(final String message) {
        super(message);
    }

    public static NotFoundException with(
            final Class<?> entity,
            final String propertyName,
            final Object propertyValue) {
        final var errorMessage = "%s with %s '%s' was not found".formatted(
                entity.getSimpleName(),
                propertyName,
                propertyValue);
        return new NotFoundException(errorMessage);
    }
}

