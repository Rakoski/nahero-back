package br.com.naheroback.common.utils;

import java.text.Normalizer;

public class StringUtils {
    public static String createSlug(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .toLowerCase()
                .replaceAll("\\p{IsM}+", "")
                .replaceAll("\\p{IsP}+", " ")
                .trim()
                .replaceAll("\\s+", "-");
    }
}
