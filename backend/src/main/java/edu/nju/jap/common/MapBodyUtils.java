package edu.nju.jap.common;

import java.util.List;
import java.util.Map;

public final class MapBodyUtils {
    private MapBodyUtils() {
    }

    public static String text(Map<String, Object> body, String key, String fallback) {
        Object value = body.get(key);
        return value == null || value.toString().isBlank() ? fallback : value.toString();
    }

    public static boolean bool(Map<String, Object> body, String key, boolean fallback) {
        Object value = body.get(key);
        return value == null ? fallback : Boolean.parseBoolean(value.toString());
    }

    public static long longValue(Object value, long fallback) {
        if (value == null) {
            return fallback;
        }
        return Long.parseLong(value.toString());
    }

    public static List<Long> longList(Object value) {
        if (!(value instanceof List<?> raw)) {
            return List.of();
        }
        return raw.stream().map(v -> Long.parseLong(v.toString())).toList();
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> mapList(Object value) {
        if (!(value instanceof List<?> raw)) {
            return List.of();
        }
        return raw.stream()
                .filter(Map.class::isInstance)
                .map(v -> (Map<String, Object>) v)
                .toList();
    }
}
