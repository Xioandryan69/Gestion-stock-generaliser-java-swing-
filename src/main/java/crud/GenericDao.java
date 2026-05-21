package crud;

import annotation.Column;
import annotation.Enumerated;
import annotation.Id;
import annotation.IgnoredField;
import annotation.ManyToOne;
import annotation.OneToMany;
import annotation.Table;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GenericDao<T> implements CrudRepository<T> {
    private final Class<T> entityClass;
    private final String tableName;
    private final List<Field> persistentFields;
    private final Field idField;

    public GenericDao(Class<T> entityClass) {
        this.entityClass = entityClass;
        this.tableName = entityClass.getAnnotation(Table.class).name();
        this.persistentFields = new ArrayList<>();
        Field tempId = null;

        for (Field f : entityClass.getDeclaredFields()) {
            if (f.isAnnotationPresent(IgnoredField.class)) continue;
            f.setAccessible(true);
            persistentFields.add(f);
            if (f.isAnnotationPresent(Id.class)) {
                tempId = f;
            }
        }
        if (tempId == null) throw new RuntimeException("Aucun champ @Id dans " + entityClass.getName());
        this.idField = tempId;
    }

    private Connection getConnection() throws SQLException {
        String url = "jdbc:postgresql://localhost:5432/stock";
        String user = "dev1";
        String password = "dev";
        try {
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(url, user, password);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Driver PostgreSQL introuvable. Assurez-vous que le driver est dans le classpath.", e);
        }
    }

    // --------------------------------------------------------------
    // Sauvegarde (INSERT) avec gestion FK, JSONB et collections
    // --------------------------------------------------------------
    @Override
    public void save(T entity) throws SQLException {
        try {
            prepareEntityForDb(entity);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new SQLException("Impossible de préparer l'entité avant sauvegarde", e);
        }

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            // 1. Insertion dans la table principale
            StringBuilder columns = new StringBuilder();
            StringBuilder placeholders = new StringBuilder();
            List<Object> values = new ArrayList<>();
            List<String> columnNames = new ArrayList<>();

            for (Field f : persistentFields) {
                if (f.isAnnotationPresent(OneToMany.class)) continue;
                Object rawValue = f.get(entity);
                Object dbValue = convertToDbValue(f, rawValue);
                if (dbValue == null) continue;
                String colName = getColumnName(f);
                columns.append(colName).append(",");
                placeholders.append("?,");
                values.add(dbValue);
                columnNames.add(colName);
            }
            columns.deleteCharAt(columns.length() - 1);
            placeholders.deleteCharAt(placeholders.length() - 1);

            String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int i = 0; i < values.size(); i++) {
                    Object val = values.get(i);
                    String colName = columnNames.get(i);

                    // Gestion spéciale pour les colonnes JSONB
                    if (colName.equals("infosSupplementaires") && val instanceof String) {
                        ps.setObject(i + 1, val, Types.OTHER);
                    } else {
                        ps.setObject(i + 1, val);
                    }
                }
                ps.executeUpdate();
            }

            syncJoinTableCollections(conn, entity);

            conn.commit();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void saveAll(Collection<? extends T> entities) throws SQLException {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        for (T entity : entities) {
            save(entity);
        }
    }

    // --------------------------------------------------------------
    // Chargement par ID avec profondeur récursive
    // --------------------------------------------------------------
    public T findById(Object id, int maxDepth) {
        return findById(id, maxDepth, new HashMap<>());
    }

    private T findById(Object id, int maxDepth, Map<Pair, Object> loadedCache) {
        if (maxDepth < 0) return null;

        Pair cacheKey = new Pair(entityClass, id);
        if (loadedCache.containsKey(cacheKey)) {
            return entityClass.cast(loadedCache.get(cacheKey));
        }

        try (Connection conn = getConnection()) {
            String sql = "SELECT * FROM " + tableName + " WHERE " + getColumnName(idField) + " = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setObject(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    T entity = entityClass.getDeclaredConstructor().newInstance();
                    loadedCache.put(cacheKey, entity);

                    // Remplir les champs simples et FK
                    for (Field f : persistentFields) {
                        if (f.isAnnotationPresent(OneToMany.class)) continue;
                        String colName = getColumnName(f);
                        Object dbValue = rs.getObject(colName);
                        if (dbValue != null) {
                            setFieldFromDbValue(entity, f, dbValue, maxDepth - 1, loadedCache);
                        }
                    }

                    // Remplir les collections @OneToMany
                    for (Field f : persistentFields) {
                        if (!f.isAnnotationPresent(OneToMany.class)) continue;
                        OneToMany ann = f.getAnnotation(OneToMany.class);
                        List<Object> items = loadJoinTableCollection(conn, ann, id, maxDepth - 1, loadedCache);
                        f.set(entity, items);
                    }

                    // Appel post‑chargement (désérialisation JSON)
                    try {
                        Method load = entity.getClass().getMethod("loadFromDb");
                        load.invoke(entity);
                    } catch (NoSuchMethodException ignored) {}

                    return entity;
                }
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    // --------------------------------------------------------------
    // Récupération de tous les enregistrements (avec/sans profondeur)
    // --------------------------------------------------------------
    public List<T> findAll(int depth) {
        List<T> list = new ArrayList<>();
        String sql = "SELECT * FROM " + tableName;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Object id = rs.getObject(getColumnName(idField));
                T entity = findById(id, depth);
                if (entity != null) list.add(entity);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return list;
    }

    public List<T> findAll() {
        return findAll(0);
    }

    @Override
    public List<T> findAll(Class<T> clazz) throws Exception {
        return findAll();
    }

    // --------------------------------------------------------------
    // Mise à jour (DELETE + INSERT)
    // --------------------------------------------------------------
    @Override
    public void update(T entity) throws SQLException {
        try {
            prepareEntityForDb(entity);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new SQLException("Impossible de préparer l'entité avant mise à jour", e);
        }

        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);

            List<Field> updateFields = new ArrayList<>();
            for (Field f : persistentFields) {
                if (f.isAnnotationPresent(OneToMany.class) || f.isAnnotationPresent(Id.class)) {
                    continue;
                }
                updateFields.add(f);
            }

            StringBuilder setClause = new StringBuilder();
            List<Object> values = new ArrayList<>();

            for (Field f : updateFields) {
                Object rawValue = f.get(entity);
                Object dbValue = convertToDbValue(f, rawValue);
                setClause.append(getColumnName(f)).append("=?,");
                values.add(dbValue);
            }

            if (setClause.length() > 0) {
                setClause.setLength(setClause.length() - 1);
            }

            String sql = "UPDATE " + tableName + " SET " + setClause +
                         " WHERE " + getColumnName(idField) + " = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int i = 0; i < updateFields.size(); i++) {
                    Field f = updateFields.get(i);
                    Object val = values.get(i);
                    String colName = getColumnName(f);
                    if (colName.equals("infosSupplementaires") && val instanceof String) {
                        ps.setObject(i + 1, val, Types.OTHER);
                    } else {
                        ps.setObject(i + 1, val);
                    }
                }
                ps.setObject(updateFields.size() + 1, idField.get(entity));
                ps.executeUpdate();
            }

            syncJoinTableCollections(conn, entity);

            conn.commit();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // --------------------------------------------------------------
    // Suppression
    // --------------------------------------------------------------
    @Override
    public void delete(T entity) throws SQLException {
        try (Connection conn = getConnection()) {
            delete(entity, conn);
        }
    }

    private void delete(T entity, Connection conn) throws SQLException {
        Object id;
        try {
            id = idField.get(entity);
        } catch (IllegalAccessException e) {
            throw new SQLException("Impossible de lire l'identifiant", e);
        }
        // Supprimer les entrées des tables de jointure
        for (Field f : persistentFields) {
            if (f.isAnnotationPresent(OneToMany.class)) {
                OneToMany ann = f.getAnnotation(OneToMany.class);
                String sql = "DELETE FROM " + ann.joinTable() + " WHERE " + ann.joinColumn() + " = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setObject(1, id);
                    ps.executeUpdate();
                }
            }
        }
        // Supprimer l'entité principale
        String sql = "DELETE FROM " + tableName + " WHERE " + getColumnName(idField) + " = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, id);
            ps.executeUpdate();
        }
    }

    private void prepareEntityForDb(T entity) throws IllegalAccessException, InvocationTargetException {
        try {
            Method prepare = entity.getClass().getMethod("prepareForDb");
            prepare.invoke(entity);
        } catch (NoSuchMethodException ignored) {
        }
    }

    private void syncJoinTableCollections(Connection conn, T entity) throws Exception {
        for (Field f : persistentFields) {
            if (!f.isAnnotationPresent(OneToMany.class)) continue;
            OneToMany ann = f.getAnnotation(OneToMany.class);
            Collection<?> items = (Collection<?>) f.get(entity);
            if (items == null) items = Collections.emptyList();

            Object ownerId = idField.get(entity);

            String deleteSql = "DELETE FROM " + ann.joinTable() +
                               " WHERE " + ann.joinColumn() + " = ?";
            try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                ps.setObject(1, ownerId);
                ps.executeUpdate();
            }

            String insertSql = "INSERT INTO " + ann.joinTable() +
                               " (" + ann.joinColumn() + ", " + ann.inverseJoinColumn() + ") VALUES (?,?)";
            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                for (Object item : items) {
                    Object itemId = persistCollectionItem(item);
                    if (itemId == null) continue;
                    ps.setObject(1, ownerId);
                    ps.setObject(2, itemId);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }
    }

    private Object persistCollectionItem(Object item) throws Exception {
        if (item == null) return null;
        // If the item is not an entity, it can be a simple ID value (Number/String)
        // or an object containing an @Id field. Handle common simple cases first.
        if (!isEntity(item.getClass())) {
            if (item instanceof Number || item instanceof String) {
                return item;
            }
            try {
                return getIdValue(item);
            } catch (RuntimeException ignored) {
                // No @Id field found — assume the object itself is the identifier
                return item;
            }
        }

        GenericDao<?> dao = createDaoForItem(item.getClass());
        Object itemId = dao.getIdValue(item);
        if (isUnsetId(itemId)) {
            invokeDaoOperation(dao, "save", item);
        } else {
            invokeDaoOperation(dao, "update", item);
        }
        return dao.getIdValue(item);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private GenericDao<?> createDaoForItem(Class<?> itemClass) {
        return new GenericDao(itemClass);
    }

    private void invokeDaoOperation(GenericDao<?> dao, String methodName, Object item) throws Exception {
        Method method = GenericDao.class.getMethod(methodName, Object.class);
        method.invoke(dao, item);
    }

    private boolean isUnsetId(Object id) {
        if (id == null) return true;
        if (id instanceof Number number) return number.longValue() == 0L;
        if (id instanceof String text) return text.isBlank() || "0".equals(text.trim());
        return false;
    }

    // --------------------------------------------------------------
    // Méthodes utilitaires privées
    // --------------------------------------------------------------
    private Object convertToDbValue(Field f, Object raw) throws IllegalAccessException {
        if (raw == null) return null;

        // FK: ne prendre que l'ID quand le champ contient une vraie entité.
        if (f.isAnnotationPresent(ManyToOne.class) && isEntity(f.getType())) {
            return getIdValue(raw);
        }
        if (isEntity(raw.getClass())) {
            return getIdValue(raw);
        }
        // Enum
        if (f.getType().isEnum()) {
            Enumerated enumAnn = f.getAnnotation(Enumerated.class);
            if (enumAnn != null && enumAnn.value() == Enumerated.EnumType.STRING) {
                return ((Enum<?>) raw).name();
            } else {
                return ((Enum<?>) raw).ordinal();
            }
        }
        // Champ JSON (chaîne sérialisée, sera traitée plus tard pour le cast PGobject)
        if (raw instanceof String && f.getName().equals("infosSupplementairesJson")) {
            return raw;
        }
        return raw;
    }

    private void setFieldFromDbValue(T entity, Field f, Object dbValue, int depth, Map<Pair, Object> cache) throws Exception {
        if (f.isAnnotationPresent(ManyToOne.class)) {
            if (isEntity(f.getType())) {
                GenericDao<?> targetDao = new GenericDao<>(f.getType());
                Object related = targetDao.findById(dbValue, depth, cache);
                f.set(entity, related);
            } else {
                if (dbValue == null) {
                    return;
                }
                if (f.getType() == int.class || f.getType() == Integer.class) {
                    f.set(entity, ((Number) dbValue).intValue());
                } else if (f.getType() == long.class || f.getType() == Long.class) {
                    f.set(entity, ((Number) dbValue).longValue());
                } else if (f.getType() == String.class) {
                    f.set(entity, dbValue.toString());
                } else {
                    f.set(entity, dbValue);
                }
            }
        } else if (isEntity(f.getType())) {
            if (dbValue != null) {
                GenericDao<?> targetDao = new GenericDao<>(f.getType());
                Object related = targetDao.findById(dbValue, depth, cache);
                f.set(entity, related);
            }
        } else if (f.getType().isEnum()) {
            Enumerated enumAnn = f.getAnnotation(Enumerated.class);
            if (enumAnn != null && enumAnn.value() == Enumerated.EnumType.STRING) {
                for (Object constant : f.getType().getEnumConstants()) {
                    if (((Enum<?>) constant).name().equals(dbValue)) {
                        f.set(entity, constant);
                        break;
                    }
                }
            } else {
                int ordinal = ((Number) dbValue).intValue();
                Object[] constants = f.getType().getEnumConstants();
                f.set(entity, constants[ordinal]);
            }
        } else if (f.getType() == java.time.LocalDate.class) {
            if (dbValue instanceof java.sql.Date sqlDate) {
                f.set(entity, sqlDate.toLocalDate());
            } else if (dbValue instanceof java.sql.Timestamp timestamp) {
                f.set(entity, timestamp.toLocalDateTime().toLocalDate());
            } else if (dbValue != null) {
                f.set(entity, java.time.LocalDate.parse(dbValue.toString()));
            }
        } else if (f.getType() == java.time.LocalDateTime.class) {
            if (dbValue instanceof java.sql.Timestamp timestamp) {
                f.set(entity, timestamp.toLocalDateTime());
            } else if (dbValue instanceof java.sql.Date sqlDate) {
                f.set(entity, sqlDate.toLocalDate().atStartOfDay());
            } else if (dbValue != null) {
                f.set(entity, java.time.LocalDateTime.parse(dbValue.toString()));
            }
        } else {
            // Conversion basique
            if (f.getType() == int.class || f.getType() == Integer.class) {
                f.set(entity, ((Number) dbValue).intValue());
            } else if (f.getType() == String.class) {
                f.set(entity, dbValue.toString());
            } else {
                f.set(entity, dbValue);
            }
        }
    }

    private List<Object> loadJoinTableCollection(Connection conn, OneToMany ann, Object ownerId,
                                                 int depth, Map<Pair, Object> cache) throws Exception {
        List<Object> items = new ArrayList<>();
        String sql = "SELECT t.* FROM " + ann.joinTable() + " jt " +
                     "JOIN " + getTableNameFromJoinTable(ann.inverseJoinColumn()) + " t " +
                     "ON jt." + ann.inverseJoinColumn() + " = t.id " +
                     "WHERE jt." + ann.joinColumn() + " = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, ownerId);
            ResultSet rs = ps.executeQuery();
            Class<?> targetClass = getTargetClassOfCollectionField(ann);
            GenericDao<?> targetDao = new GenericDao<>(targetClass);
            while (rs.next()) {
                Object itemId = rs.getObject("id");
                Object item = targetDao.findById(itemId, depth, cache);
                if (item != null) items.add(item);
            }
        }
        return items;
    }

    private String getTableNameFromJoinTable(String inverseJoinColumn) {
        return inverseJoinColumn.replace("_id", "");
    }

    private Class<?> getTargetClassOfCollectionField(OneToMany ann) throws Exception {
        String table = getTableNameFromJoinTable(ann.inverseJoinColumn());
        String className = "model." + Character.toUpperCase(table.charAt(0)) + table.substring(1);
        return Class.forName(className);
    }

    private boolean isEntity(Class<?> type) {
        return type.isAnnotationPresent(Table.class);
    }

    public Object getIdValue(Object obj) throws IllegalAccessException {
        if (obj == null) return null;
        for (Field f : obj.getClass().getDeclaredFields()) {
            if (f.isAnnotationPresent(Id.class)) {
                f.setAccessible(true);
                return f.get(obj);
            }
        }
        throw new RuntimeException("Aucun champ @Id dans " + obj.getClass());
    }

    private String getColumnName(Field f) {
        Column col = f.getAnnotation(Column.class);
        if (col != null && !col.name().isEmpty()) return col.name();
        ManyToOne many = f.getAnnotation(ManyToOne.class);
        if (many != null && !many.joinColumn().isEmpty()) return many.joinColumn();
        return f.getName();
    }

    public String getLastId() {
        String sql = "SELECT " + getColumnName(idField) + " FROM " + tableName +
                     " ORDER BY CAST(" + getColumnName(idField) + " AS INTEGER) DESC LIMIT 1";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return "0";
    }

    // Clé de cache interne
    private static class Pair {
        Class<?> clazz;
        Object id;
        Pair(Class<?> c, Object i) { clazz = c; id = i; }
        @Override public boolean equals(Object o) {
            if (!(o instanceof Pair p)) return false;
            return clazz.equals(p.clazz) && Objects.equals(id, p.id);
        }
        @Override public int hashCode() { return Objects.hash(clazz, id); }
    }
}