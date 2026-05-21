package utils;

public class TypeUtils {
    public static boolean isNumeric(Class<?> type) {
        return Number.class.isAssignableFrom(type) || type == int.class || type == double.class ||
               type == float.class || type == long.class || type == short.class ||type ==  Integer.class ;
    }

    public static boolean isTextual(Class<?> type) {
        return type == String.class || type == char.class || type == Character.class;
    }

    public static boolean isBoolean(Class<?> type) {
        return type == Boolean.class || type == boolean.class;
    }

    public static boolean isSimpleType(Class<?> type) {
        return isNumeric(type) || isTextual(type) || isBoolean(type);
    }
}