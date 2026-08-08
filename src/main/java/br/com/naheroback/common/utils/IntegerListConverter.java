package br.com.naheroback.common.utils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Converter
public class IntegerListConverter implements AttributeConverter<List<Integer>, String> {
    
    @Override
    public String convertToDatabaseColumn(List<Integer> list) {
        if (list == null || list.isEmpty()) return "";
        return list.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
    
    @Override
    public List<Integer> convertToEntityAttribute(String joined) {
        if (joined == null || joined.isEmpty()) return new ArrayList<>();
        return Arrays.stream(joined.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Integer::parseInt)
                .toList();
    }
}

