package br.com.naheroback.common.utils;

import br.com.naheroback.common.entities.BaseEntity;
import br.com.naheroback.common.exceptions.custom.NotFoundException;
import br.com.naheroback.common.repositories.BaseRepository;

import java.util.Objects;

public class ValidateDependencies {
    public static <E extends BaseEntity, R extends BaseRepository<E, Integer>> E validate(
            Class<E> entityClass,
            R repository,
            E entityInstance) {
        if (Objects.nonNull(entityInstance) && Objects.nonNull(entityInstance.getId())) {
            repository.findById(entityInstance.getId())
                    .orElseThrow(() -> NotFoundException.with(entityClass, "id", entityInstance.getId()));
        }
        return null;
    }

    public static <T> T nullIfEmpty(T value) {
        if (value instanceof String string) {
            return string.isEmpty() ? null : value;
        }
        return value;
    }
}