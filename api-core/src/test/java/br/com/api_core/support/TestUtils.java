package br.com.api_core.support;

public class TestUtils {

    private TestUtils() {}

    public static void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("setField falhou: " + fieldName, e);
        }
    }

    public static Object getField(Object target, String fieldName) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception e) {
            throw new RuntimeException("getField falhou: " + fieldName, e);
        }
    }
}
