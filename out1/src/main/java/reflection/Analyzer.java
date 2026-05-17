package reflection;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import annotation.Id;
import annotation.IgnoredField;
import annotation.Table;
import metadata.ClassInfo;
import metadata.FieldInfo;
import metadata.TypeField;

/**
 * Analyse une classe Java par Reflection et produit :
 *  - List<FieldInfo> (champs)
 *  - ClassInfo       (classe entière)
 */
public class Analyzer {

    // -------------------------------------------------------
    // Analyse complète → ClassInfo
    // -------------------------------------------------------

    /**
     * Analyse une classe et retourne ses métadonnées complètes.
     */
    public static ClassInfo analyzeClass(Class<?> clazz) {
        ClassInfo info = new ClassInfo();
        info.setClassName(clazz.getSimpleName());

        // Nom de table : @Table(name=…) ou nom simple de la classe
        if (clazz.isAnnotationPresent(Table.class)) {
            String tableName = clazz.getAnnotation(Table.class).name();
            info.setTableName(tableName.isEmpty() ? clazz.getSimpleName().toLowerCase() : tableName);
        } else {
            info.setTableName(clazz.getSimpleName().toLowerCase());
        }

        info.setFields(analyze(clazz));
        return info;
    }

    // -------------------------------------------------------
    // Analyse des champs → List<FieldInfo>
    // -------------------------------------------------------

    /**
     * Analyse tous les champs déclarés d'une classe.
     * Respecte @Id et @IgnoredField.
     * Détecte : PRIMITIVE, ENUM, LIST, MAP, OBJECT.
     *
     * CORRECTION : utilise List.class.isAssignableFrom(type)
     *              au lieu de type == List.class
     *              (ArrayList, LinkedList… sont aussi des List)
     */
    public static List<FieldInfo> analyze(Class<?> clazz) {

        List<FieldInfo> fields = new ArrayList<>();

        for (Field field : clazz.getDeclaredFields()) {

            FieldInfo info = new FieldInfo();
            info.setNom(field.getName());
            info.setType(field.getType());

            // --- @IgnoredField ---
            if (field.isAnnotationPresent(IgnoredField.class)) {
                info.setIgnored(true);
                info.setTypeField(TypeField.PRIMITIVE); // valeur par défaut neutre
                fields.add(info);
                continue;
            }

            // --- @Id ---
            if (field.isAnnotationPresent(Id.class)) {
                info.setIdField(true);
            }

            Class<?> type = field.getType();

            // ---- ENUM ----
            if (type.isEnum()) {
                info.setEnumType(true);
                info.setTypeField(TypeField.ENUM);
            }

            // ---- LIST<T> ----
            // CORRECTION : isAssignableFrom couvre ArrayList, LinkedList, etc.
            else if (List.class.isAssignableFrom(type)) {
                info.setListType(true);
                info.setTypeField(TypeField.LIST);

                // Extraire le type générique T de List<T>
                if (field.getGenericType() instanceof ParameterizedType) {
                    ParameterizedType pType = (ParameterizedType) field.getGenericType();
                    java.lang.reflect.Type arg = pType.getActualTypeArguments()[0];
                    if (arg instanceof Class) {
                        info.setGenericType((Class<?>) arg);
                    }
                }
            }

            // ---- MAP<K,V> ----
            else if (Map.class.isAssignableFrom(type)) {
                info.setTypeField(TypeField.MAP);
            }

            // ---- TYPE PRIMITIF / WRAPPER / String ----
            else if (isPrimitive(type)) {
                info.setPrimitiveType(true);
                info.setTypeField(TypeField.PRIMITIVE);
            }

            // ---- OBJET (classe créée) ----
            else {
                info.setObjectType(true);
                info.setTypeField(TypeField.OBJECT);
            }

            fields.add(info);
        }

        return fields;
    }

    // -------------------------------------------------------
    // Utilitaire
    // -------------------------------------------------------

    /**
     * Retourne true pour tous les types simples Java
     * (primitifs, wrappers, String, char, Date).
     */
    private static boolean isPrimitive(Class<?> type) {
        return type == String.class
                || type == Integer.class   || type == int.class
                || type == Double.class    || type == double.class
                || type == Float.class     || type == float.class
                || type == Long.class      || type == long.class
                || type == Boolean.class   || type == boolean.class
                || type == Short.class     || type == short.class
                || type == Byte.class      || type == byte.class
                || type == Character.class || type == char.class
                || type == java.util.Date.class
                || type == java.time.LocalDate.class
                || type == java.time.LocalDateTime.class;
    }
}