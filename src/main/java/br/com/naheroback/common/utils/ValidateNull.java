package br.com.naheroback.common.utils;

import br.com.naheroback.common.entities.BaseEntity;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.function.Function;

public class ValidateNull {
    public static <T extends BaseEntity> T validate(
            Class<T> entityClass,
            Integer id) {

        if (Objects.isNull(id))
            return null;

        try {
            T entity = entityClass.getDeclaredConstructor().newInstance();
            entity.setId(id);
            return entity;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException("Failed to create instance of " + entityClass.getSimpleName(), e);
        }
    }

    public static <T, R> R validate(T value, Function<T, R> mapper) {
        return value != null ? mapper.apply(value) : null;
    }

}
