package reflection;

import java.lang.reflect.Field;

public class ReflectionUtils {

    public static Object getFieldValue(Object obj, Field field) throws Exception {
        field.setAccessible(true);
        return field.get(obj);
    }

    public static void setFieldValue(Object obj, Field field, Object value) throws Exception {
        field.setAccessible(true);
        field.set(obj, value);
    }

    public static Field getFieldByName(Class<?> clazz, String name) throws NoSuchFieldException {
        return clazz.getDeclaredField(name);
    }
    public static Object getFieldValueByName(Object obj, String fieldName) throws Exception {
    Field field = getFieldByName(obj.getClass(), fieldName);
    return getFieldValue(obj, field);
}
}