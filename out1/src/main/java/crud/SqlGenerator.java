package crud;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import annotation.Id;
import annotation.IgnoredField;

/**
 * Génère dynamiquement des requêtes SQL à partir d'une classe Java.
 * Respecte @Id et @IgnoredField.
 *
 * Exemple pour Personne { @Id String id; String nom; }
 *   INSERT INTO personne(id,nom) VALUES(?,?)
 *   UPDATE personne SET nom=? WHERE id=?
 *   DELETE FROM personne WHERE id=?
 *   SELECT * FROM personne
 */
public class SqlGenerator {

    // -------------------------------------------------------
    // INSERT INTO table(col1,col2) VALUES(?,?)
    // -------------------------------------------------------
    public static String generateInsert(Class<?> clazz) {
        StringBuilder columns = new StringBuilder();
        StringBuilder values  = new StringBuilder();

        List<Field> fields = getActiveFields(clazz);

        for (int i = 0; i < fields.size(); i++) {
            columns.append(fields.get(i).getName());
            values.append("?");
            if (i < fields.size() - 1) {
                columns.append(",");
                values.append(",");
            }
        }

        return "INSERT INTO " + tableName(clazz)
                + "(" + columns + ") VALUES(" + values + ")";
    }

    // -------------------------------------------------------
    // UPDATE table SET col1=?,col2=? WHERE idCol=?
    // -------------------------------------------------------
    public static String generateUpdate(Class<?> clazz) {
        String idCol   = findIdFieldName(clazz);
        StringBuilder set = new StringBuilder();

        for (Field f : getActiveFields(clazz)) {
            if (f.getName().equals(idCol)) continue; // l'ID va dans WHERE
            set.append(f.getName()).append("=?,");
        }
        // Supprimer la dernière virgule
        if (set.length() > 0) set.setLength(set.length() - 1);

        return "UPDATE " + tableName(clazz)
                + " SET " + set
                + " WHERE " + idCol + "=?";
    }

    // -------------------------------------------------------
    // DELETE FROM table WHERE idCol=?
    // -------------------------------------------------------
    public static String generateDelete(Class<?> clazz) {
        return "DELETE FROM " + tableName(clazz)
                + " WHERE " + findIdFieldName(clazz) + "=?";
    }

    // -------------------------------------------------------
    // SELECT * FROM table
    // -------------------------------------------------------
    public static String generateSelectAll(Class<?> clazz) {
        return "SELECT * FROM " + tableName(clazz);
    }

    // -------------------------------------------------------
    // SELECT * FROM table WHERE idCol=?
    // -------------------------------------------------------
    public static String generateSelectById(Class<?> clazz) {
        return "SELECT * FROM " + tableName(clazz)
                + " WHERE " + findIdFieldName(clazz) + "=?";
    }

    // -------------------------------------------------------
    // Liste des noms de colonnes (pour JTable header, etc.)
    // -------------------------------------------------------
    public static List<String> getColumnNames(Class<?> clazz) {
        List<String> names = new ArrayList<>();
        for (Field f : getActiveFields(clazz)) {
            names.add(f.getName());
        }
        return names;
    }

    // -------------------------------------------------------
    // Helpers privés
    // -------------------------------------------------------

    /**
     * Retourne le nom de la table :
     * @Table(name="…") ou nom simple de la classe en minuscules.
     */
    public static String tableName(Class<?> clazz) {
        if (clazz.isAnnotationPresent(annotation.Table.class)) {
            String n = clazz.getAnnotation(annotation.Table.class).name();
            return n.isEmpty() ? clazz.getSimpleName().toLowerCase() : n;
        }
        return clazz.getSimpleName().toLowerCase();
    }

    /**
     * Retourne les champs actifs : ni @IgnoredField.
     */
    public static List<Field> getActiveFields(Class<?> clazz) {
        List<Field> result = new ArrayList<>();
        for (Field f : clazz.getDeclaredFields()) {
            if (!f.isAnnotationPresent(IgnoredField.class)) {
                result.add(f);
            }
        }
        return result;
    }

    /**
     * Trouve le nom du champ @Id.
     * Si aucun @Id n'est présent, utilise "id" par convention.
     */
    public static String findIdFieldName(Class<?> clazz) {
        for (Field f : clazz.getDeclaredFields()) {
            if (f.isAnnotationPresent(Id.class)) return f.getName();
        }
        return "id"; // convention par défaut
    }
}