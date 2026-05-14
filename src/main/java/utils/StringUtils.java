package utils;

public class StringUtils {
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static String capitalize(String str) {
        if (isEmpty(str)) return str;
        return str.substring(0,1).toUpperCase() + str.substring(1);
    }

    public static String toCamelCase(String input) {
        // simple implementation
        String[] parts = input.split("[_\\-]");
        StringBuilder sb = new StringBuilder(parts[0].toLowerCase());
        for (int i=1; i<parts.length; i++) {
            sb.append(capitalize(parts[i].toLowerCase()));
        }
        return sb.toString();
    }
}