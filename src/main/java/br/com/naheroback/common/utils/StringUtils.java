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

    public static String plainText(String html, int maxLength) {
        if (html == null) return "";
        String text = html.replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }
}
