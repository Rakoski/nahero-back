package br.com.naheroback.common.repositories;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import br.com.naheroback.common.entities.BaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.NoRepositoryBean;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.DatePath;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;

@NoRepositoryBean
public interface BaseRepository<E extends BaseEntity, I>
        extends JpaRepository<E, I>, QuerydslPredicateExecutor<E>,
        QuerydslBinderCustomizer<EntityPathBase<E>> {

    @Override
    default void customize(QuerydslBindings bindings, EntityPathBase<E> entity) {

        bindings.bind(String.class).all((path, values) -> {
            final BooleanBuilder predicate = new BooleanBuilder();
            values.forEach(value -> predicate.or(((StringPath) path).containsIgnoreCase(value)));
            return Optional.of(predicate);
        });

        bindings.bind(Integer.class).all((path, values) -> {
            final List<? extends Integer> numbers = new ArrayList<>(values);
            if (numbers.size() == 1) {
                return Optional.of(((NumberPath<Integer>) path).eq(numbers.get(0)));
            }
            if (numbers.size() == 2) {
                return Optional.of(((NumberPath<Integer>) path).between(numbers.get(0), numbers.get(1)));
            }
            final BooleanBuilder predicate = new BooleanBuilder();
            values.forEach(value -> predicate.or(((NumberPath<Integer>) path).eq(value)));
            return Optional.of(predicate);
        });

        bindings.bind(BigDecimal.class).all((path, values) -> {
            final List<? extends BigDecimal> numbers = new ArrayList<>(values);
            if (numbers.size() == 1) {
                return Optional.of(((NumberPath<BigDecimal>) path).eq(numbers.get(0)));
            }
            if (numbers.size() == 2) {
                return Optional.of(((NumberPath<BigDecimal>) path).between(numbers.get(0), numbers.get(1)));
            }
            final BooleanBuilder predicate = new BooleanBuilder();
            values.forEach(value -> predicate.or(((NumberPath<BigDecimal>) path).eq(value)));
            return Optional.of(predicate);
        });

        bindings.bind(LocalDate.class).all((path, values) -> {
            final List<? extends LocalDate> dates = new ArrayList<>(values);
            if (dates.size() == 1) {
                return Optional.of(((DatePath<LocalDate>) path).eq(dates.get(0)));
            }
            return Optional.of(((DatePath<LocalDate>) path).between(dates.get(0), dates.get(1)));
        });

        bindings.bind(LocalDateTime.class).all((path, values) -> {
            final List<? extends LocalDateTime> dates = new ArrayList<>(values);
            if (dates.size() == 1) {
                return Optional.of(((DateTimePath<LocalDateTime>) path).eq(dates.get(0)));
            }
            return Optional.of(((DateTimePath<LocalDateTime>) path).between(dates.get(0), dates.get(1)));
        });
    }

}

