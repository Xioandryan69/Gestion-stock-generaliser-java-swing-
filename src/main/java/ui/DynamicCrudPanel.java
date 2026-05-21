package ui;
import annotation.Id;
import annotation.IgnoredField;
import annotation.OneToMany;
import annotation.Table;
import crud.GenericDao;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import metadata.ClassInfo;
import metadata.FieldInfo;
import reflection.Analyzer;
import utils.TypeUtils;






public class DynamicCrudPanel<T> extends JPanel {
    private final Class<T> entityClass;
    private final GenericDao<T> dao;
    private final List<Field> editableFields;
    private final Map<String, JComponent> fieldEditors = new LinkedHashMap<>();
    private final JTable table;
    private final DefaultTableModel tableModel;
    private JButton btnSave, btnDelete, btnNew, btnRefresh;
    private T currentEntity;

    public DynamicCrudPanel(Class<T> entityClass) {
        this.entityClass = entityClass;
        this.dao = new GenericDao<>(entityClass);
        this.editableFields = new ArrayList<>();

        // Utiliser l'analyseur de métadonnées pour déterminer les champs éditables
        try {
            ClassInfo classInfo = Analyzer.analyzeClass(entityClass);
            if (classInfo != null && classInfo.getFields() != null) {
                for (FieldInfo finfo : classInfo.getFields()) {
                    if (finfo.isIgnored()) continue;
                    try {
                        Field f = entityClass.getDeclaredField(finfo.getNom());
                        if (f.isAnnotationPresent(IgnoredField.class)) continue;
                        f.setAccessible(true);
                        editableFields.add(f);
                    } catch (NoSuchFieldException nsf) {
                        // Champ absent, ignorer
                    }
                }
            }
        } catch (Exception ex) {
            // Fallback : si l'analyse échoue, utiliser réflexion directe
            for (Field f : entityClass.getDeclaredFields()) {
                if (f.isAnnotationPresent(IgnoredField.class)) continue;
                f.setAccessible(true);
                editableFields.add(f);
            }
        }

        setLayout(new BorderLayout());

        //--- Formulaire (haut) ---
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.anchor = GridBagConstraints.WEST;
        int row = 0;

        for (Field f : editableFields) {
            JLabel label = new JLabel(f.getName() + ":");
            JComponent editor = createEditor(f);
            fieldEditors.put(f.getName(), editor);

            gbc.gridx = 0; gbc.gridy = row;
            formPanel.add(label, gbc);
            gbc.gridx = 1;
            formPanel.add(editor, gbc);
            row++;
        }

        // Boutons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        btnNew = new JButton("Nouveau");
        btnSave = new JButton("Enregistrer");
        btnDelete = new JButton("Supprimer");
        btnRefresh = new JButton("Rafraîchir");

        btnNew.addActionListener(e -> clearForm());
        btnSave.addActionListener(e -> saveEntity());
        btnDelete.addActionListener(e -> deleteEntity());
        btnRefresh.addActionListener(e -> loadTableData());

        buttonPanel.add(btnNew);
        buttonPanel.add(btnSave);
        buttonPanel.add(btnDelete);
        buttonPanel.add(btnRefresh);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(formPanel, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(topPanel, BorderLayout.NORTH);

        // --- Tableau (centre) ---
        tableModel = new DefaultTableModel() {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        table = new JTable(tableModel);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow >= 0) {
                    Object id = tableModel.getValueAt(selectedRow, 0);
                    loadEntityById(id);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        // Chargement initial
        loadTableData();
    }

    public DynamicCrudPanel<T> getFormPanel() {
        return this;
    }

    public void resetForm() {
        clearForm();
    }

    public void addFormListener(ui.listener.FormListener listener) {
        // Compatibilité avec les écrans d'exemple existants.
    }

    public void addFormListener(ui.listener.FormAction action, ui.listener.FormListener listener) {
        // Compatibilité avec les écrans d'exemple existants.
    }

    public void loadEntities(List<?> entities) {
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);
        for (Field field : editableFields) {
            tableModel.addColumn(field.getName());
        }
        for (Object entity : entities) {
            Object[] row = new Object[editableFields.size()];
            for (int i = 0; i < editableFields.size(); i++) {
                try {
                    Object value = editableFields.get(i).get(entity);
                    // Preserve the actual value type; don't convert to string
                    row[i] = value;
                } catch (Exception exception) {
                    row[i] = null;
                }
            }
            tableModel.addRow(row);
        }
    }

    public void saveEntities(List<? extends T> entities) throws Exception {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        dao.saveAll(entities);
        loadTableData();
        clearForm();
    }

    public void openBatchAddDialog() {
        JDialog dialog = new JDialog();
        dialog.setModal(true);
        dialog.setTitle("Ajout multiple - " + entityClass.getSimpleName());
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JSpinner countSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 30, 1));
        JButton btnRebuild = new JButton("Créer les lignes");
        header.add(new JLabel("Nombre de lignes :"));
        header.add(countSpinner);
        header.add(btnRebuild);
        dialog.add(header, BorderLayout.NORTH);

        JPanel rowsContainer = new JPanel();
        rowsContainer.setLayout(new javax.swing.BoxLayout(rowsContainer, javax.swing.BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(rowsContainer);
        dialog.add(scrollPane, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnCancel = new JButton("Annuler");
        JButton btnSaveAll = new JButton("Ajouter plusieurs");
        footer.add(btnCancel);
        footer.add(btnSaveAll);
        dialog.add(footer, BorderLayout.SOUTH);

        List<Map<String, JComponent>> batchEditors = new ArrayList<>();

        Runnable rebuild = () -> {
            rowsContainer.removeAll();
            batchEditors.clear();
            int count = ((Number) countSpinner.getValue()).intValue();
            for (int index = 0; index < count; index++) {
                Map<String, JComponent> editors = createBatchEditors();
                batchEditors.add(editors);
                rowsContainer.add(createBatchFormPanel(editors, "Ligne " + (index + 1)));
            }
            rowsContainer.revalidate();
            rowsContainer.repaint();
            dialog.pack();
        };

        btnRebuild.addActionListener(e -> rebuild.run());
        countSpinner.addChangeListener(e -> rebuild.run());
        btnCancel.addActionListener(e -> dialog.dispose());
        btnSaveAll.addActionListener(e -> {
            try {
                List<T> entities = new ArrayList<>();
                for (Map<String, JComponent> editors : batchEditors) {
                    T entity = buildEntityFromEditors(editors);
                    if (entity != null) {
                        entities.add(entity);
                    }
                }
                saveEntities(entities);
                dialog.dispose();
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(dialog,
                        "Impossible d'ajouter les éléments :\n" + exception.getMessage(),
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
                exception.printStackTrace();
            }
        });

        rebuild.run();
        dialog.setSize(1000, 700);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JComponent createEditor(Field f) {
        // Si le champ est une énumération
        if (f.getType().isEnum()) {
            JComboBox<Object> combo = new JComboBox<>();
            for (Object constant : f.getType().getEnumConstants()) {
                combo.addItem(constant);
            }
            return combo;
        }
        // Si c'est une entité (@Table) = clé étrangère
        if (f.getType().isAnnotationPresent(Table.class)) {
            GenericDao<?> subDao = new GenericDao<>(f.getType());
            List<?> items = subDao.findAll();
            // Ajouter "null" en première position
            List<Object> comboItems = new ArrayList<>();
            comboItems.add(null);   // option "Aucune"
            comboItems.addAll(items);
            JComboBox<Object> combo = new JComboBox<>(comboItems.toArray());
            combo.setRenderer(new DefaultListCellRenderer() {
                @Override
                public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    if (value == null) {
                        return super.getListCellRendererComponent(list, "(Aucune)", index, isSelected, cellHasFocus);
                    }
                    return super.getListCellRendererComponent(list, value.toString(), index, isSelected, cellHasFocus);
                }
            });
            return combo;
        }
                // Dans createEditor(Field f), après les checks existants :
        if (f.isAnnotationPresent(OneToMany.class)) {
            return createListEditor(f);
        }
        // Sinon, champ texte simple
        return new JTextField(20);
    }

    private Map<String, JComponent> createBatchEditors() {
        Map<String, JComponent> editors = new LinkedHashMap<>();
        for (Field field : editableFields) {
            if (field.isAnnotationPresent(OneToMany.class)) {
                continue;
            }
            editors.put(field.getName(), createBatchEditor(field));
        }
        return editors;
    }

    private JPanel createBatchFormPanel(Map<String, JComponent> editors, String title) {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder(title));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;
        for (Field field : editableFields) {
            if (field.isAnnotationPresent(OneToMany.class)) {
                continue;
            }

            JComponent editor = editors.get(field.getName());
            if (editor == null) {
                continue;
            }

            gbc.gridx = 0;
            gbc.gridy = row;
            formPanel.add(new JLabel(field.getName() + ":"), gbc);

            gbc.gridx = 1;
            formPanel.add(editor, gbc);
            row++;
        }

        return formPanel;
    }

    private JComponent createBatchEditor(Field f) {
        if (f.getType().isEnum()) {
            JComboBox<Object> combo = new JComboBox<>();
            for (Object constant : f.getType().getEnumConstants()) {
                combo.addItem(constant);
            }
            return combo;
        }

        if (f.getType().isAnnotationPresent(Table.class)) {
            GenericDao<?> subDao = new GenericDao<>(f.getType());
            List<?> items = subDao.findAll();
            List<Object> comboItems = new ArrayList<>();
            comboItems.add(null);
            comboItems.addAll(items);
            JComboBox<Object> combo = new JComboBox<>(comboItems.toArray());
            combo.setRenderer(new DefaultListCellRenderer() {
                @Override
                public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    if (value == null) {
                        return super.getListCellRendererComponent(list, "(Aucune)", index, isSelected, cellHasFocus);
                    }
                    return super.getListCellRendererComponent(list, value.toString(), index, isSelected, cellHasFocus);
                }
            });
            return combo;
        }

        return new JTextField(20);
    }

    @SuppressWarnings("unchecked")
    private T buildEntityFromEditors(Map<String, JComponent> editors) throws Exception {
        T entity = entityClass.getDeclaredConstructor().newInstance();

        for (Field field : editableFields) {
            if (field.isAnnotationPresent(OneToMany.class)) {
                continue;
            }

            JComponent component = editors.get(field.getName());
            if (component == null) {
                continue;
            }

            Object value = getValueFromEditor(component, field);
            if (value == null && field.isAnnotationPresent(Id.class)) {
                continue;
            }

            field.set(entity, value);
        }

        try {
            Method prepare = entity.getClass().getMethod("prepareForDb");
            prepare.invoke(entity);
        } catch (NoSuchMethodException ignored) {
        }

        return entity;
    }

    private void clearForm() {
        currentEntity = null;
        for (JComponent comp : fieldEditors.values()) {
            if (comp instanceof JTextField textField) {
                textField.setText("");
            } else if (comp instanceof JComboBox<?> comboBox) {
                comboBox.setSelectedIndex(-1);
            } else if (comp instanceof JPanel panel) {
                clearListPanel(panel);
            }
        }
        table.clearSelection();
    }

    private void clearListPanel(JPanel panel) {
        for (java.awt.Component child : panel.getComponents()) {
            if (child instanceof JScrollPane scrollPane) {
                java.awt.Component view = scrollPane.getViewport().getView();
                if (view instanceof JTable listTable) {
                    DefaultTableModel model = (DefaultTableModel) listTable.getModel();
                    model.setRowCount(0);
                }
            }
        }
    }

    private void loadEntityById(Object id) {
        T entity = dao.findById(id, 1); // profondeur 1 pour charger l'adresse
        if (entity != null) {
            currentEntity = entity;
            populateForm(entity);
            // Force le rafraîchissement de l'affichage
            this.revalidate();
            this.repaint();
        }
    }

    private void populateForm(T entity) {
        try {
            for (Field f : editableFields) {
                JComponent comp = fieldEditors.get(f.getName());
                Object value = f.get(entity);

                if (comp instanceof JPanel panel) {
                    // Cas List<T> (téléphones)
                    JTable listTable = (JTable) ((JScrollPane) panel.getComponent(0)).getViewport().getView();
                    DefaultTableModel model = (DefaultTableModel) listTable.getModel();
                    model.setRowCount(0);
                    if (value instanceof List<?> list) {
                        for (Object item : list) {
                            model.addRow(new Object[]{item});
                        }
                    }
                } else if (comp instanceof JTextField textField) {
                    textField.setText(value != null ? value.toString() : "");
                } else if (comp instanceof JComboBox<?> comboBox) {
                    JComboBox<Object> combo = (JComboBox<Object>) comboBox;
                    if (value == null) {
                        combo.setSelectedItem(null);
                    } else {
                        if (f.getType().isEnum()) {
                            // Enum : sélection directe
                            combo.setSelectedItem(value);
                        } else {
                            // Entité : comparer par ID
                            Object valueId = dao.getIdValue(value);
                            for (int i = 0; i < combo.getItemCount(); i++) {
                                Object item = combo.getItemAt(i);
                                if (item != null && dao.getIdValue(item).equals(valueId)) {
                                    combo.setSelectedIndex(i);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            this.revalidate();
            this.repaint();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erreur lors du remplissage : " + e.getMessage(),
                                        "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

private void saveEntity() {
    try {
        T entity;
        boolean isNew = (currentEntity == null);

        if (isNew) {
            entity = entityClass.getDeclaredConstructor().newInstance();
        } else {
            entity = currentEntity;
        }

        // Remplir les champs depuis le formulaire
        for (Field f : editableFields) {
            JComponent comp = fieldEditors.get(f.getName());
            if (comp == null) continue;

            Object value = getValueFromEditor(comp, f);

            // Ne pas écraser l'ID si c'est une mise à jour (sauf si vide)
            if (!isNew && f.isAnnotationPresent(Id.class) && value == null) {
                continue;
            }

            try {
                f.set(entity, value);
            } catch (Exception ex) {
                System.err.println("Impossible de setter " + f.getName() + " = " + value);
            }
        }

        // Préparation spéciale pour Personne (JSON)
        try {
            Method prepare = entity.getClass().getMethod("prepareForDb");
            prepare.invoke(entity);
        } catch (NoSuchMethodException ignored) {}

        // Sauvegarde
        if (isNew) {
            dao.save(entity);
            JOptionPane.showMessageDialog(this, "Nouvelle personne créée avec succès !");
        } else {
            dao.update(entity);
            JOptionPane.showMessageDialog(this, "Personne mise à jour avec succès !");
        }

        currentEntity = entity;
        loadTableData();     // Rafraîchir le tableau
        clearForm();         // Vider le formulaire

    } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, 
            "Erreur lors de l'enregistrement :\n" + ex.getMessage(), 
            "Erreur", JOptionPane.ERROR_MESSAGE);
    }
}

    private void deleteEntity() {
        if (currentEntity == null) {
            JOptionPane.showMessageDialog(this, "Aucune entité sélectionnée.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Supprimer cette entité ?", "Confirmation", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                dao.delete(currentEntity);
                currentEntity = null;
                clearForm();
                loadTableData();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private Object getValueFromEditor(JComponent comp, Field f) {
        if (comp instanceof JPanel panel && f.isAnnotationPresent(OneToMany.class)) {
            JTable listTable = (JTable) ((JScrollPane) panel.getComponent(0)).getViewport().getView();
            DefaultTableModel model = (DefaultTableModel) listTable.getModel();
            List<Object> items = new ArrayList<>();
            for (int i = 0; i < model.getRowCount(); i++) {
                Object val = model.getValueAt(i, 0);
                if (val != null) items.add(val);
            }
            return items;
        } else if (comp instanceof JTextField textField) {
            String text = textField.getText();
            if (f.getType() == int.class || f.getType() == Integer.class) {
                return text.isEmpty() ? 0 : Integer.valueOf(text);
            }
            if (f.getType() == String.class) {
                return text.isEmpty() ? null : text;
            }
            return text;
        } else if (comp instanceof JComboBox<?> combo) {
            Object selected = combo.getSelectedItem();
            if (selected == null) return null;

            // Si c'est une entité liée (Addresse, etc.)
            if (f.getType().isAnnotationPresent(Table.class)) {
                return selected;   // On garde l'objet complet
            }
            return selected;
        }
        return null;
    }
    // Nouvelle méthode pour créer l'éditeur d'une collection
private JComponent createListEditor(Field f) {
    // Récupérer le type générique de la liste (ex: Addresse.class)
    Class<?> itemType = getGenericTypeOfField(f); // à définir (voir plus bas)

    JPanel panel = new JPanel(new BorderLayout());
    DefaultTableModel listModel = new DefaultTableModel(new String[]{"Éléments"}, 0);
    JTable listTable = new JTable(listModel);
    JScrollPane scroll = new JScrollPane(listTable);
    scroll.setPreferredSize(new java.awt.Dimension(250, 100));

    JPanel editorPanel = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(2, 4, 2, 4);
    gbc.fill = GridBagConstraints.HORIZONTAL;

    List<Field> itemFields = new ArrayList<>();
    Map<String, JComponent> itemEditors = new LinkedHashMap<>();
    int editorRow = 0;
    for (Field itemField : itemType.getDeclaredFields()) {
        if (itemField.isAnnotationPresent(annotation.IgnoredField.class)) continue;
        if (itemField.isAnnotationPresent(annotation.Id.class)) continue;
        if (itemField.isAnnotationPresent(OneToMany.class)) continue;
        itemField.setAccessible(true);
        itemFields.add(itemField);

        gbc.gridx = 0;
        gbc.gridy = editorRow;
        editorPanel.add(new JLabel(itemField.getName() + ":"), gbc);

        gbc.gridx = 1;
        JComponent editor = createSimpleEditorForType(itemField.getType());
        itemEditors.put(itemField.getName(), editor);
        editorPanel.add(editor, gbc);
        editorRow++;
    }

    JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    JButton btnAdd = new JButton("+ Ajouter");
    JButton btnRemove = new JButton("- Supprimer");

    btnAdd.addActionListener(e -> {
        try {
            Object newItem = itemType.getDeclaredConstructor().newInstance();
            for (Field itemField : itemFields) {
                JComponent editor = itemEditors.get(itemField.getName());
                if (editor == null) continue;
                Object value = readSimpleEditorValue(editor, itemField.getType());
                if (value == null && itemField.getType().isPrimitive()) {
                    continue;
                }
                itemField.set(newItem, value);
            }
            listModel.addRow(new Object[]{newItem});
            for (JComponent editor : itemEditors.values()) {
                resetSimpleEditor(editor);
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    });
    btnRemove.addActionListener(e -> {
        int sel = listTable.getSelectedRow();
        if (sel >= 0) listModel.removeRow(sel);
    });

    btnPanel.add(btnAdd);
    btnPanel.add(btnRemove);
    panel.add(scroll, BorderLayout.CENTER);
    JPanel southPanel = new JPanel(new BorderLayout());
    southPanel.add(editorPanel, BorderLayout.CENTER);
    southPanel.add(btnPanel, BorderLayout.SOUTH);
    panel.add(southPanel, BorderLayout.SOUTH);
    panel.setBorder(BorderFactory.createTitledBorder(f.getName()));

    // Stocker la table pour pouvoir lire/écrire les valeurs
    listTable.putClientProperty("fieldName", f.getName());
    listTable.putClientProperty("itemType", itemType);
    fieldEditors.put(f.getName(), panel); // on référence le panel
    return panel;
}

private JComponent createSimpleEditorForType(Class<?> type) {
        if (TypeUtils.isBoolean(type)) {
            return new JCheckBox();
        }
        if (TypeUtils.isNumeric(type)) {
            return new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
        }
        if (type.isEnum()) {
        JComboBox<Object> combo = new JComboBox<>();
        for (Object constant : type.getEnumConstants()) {
            combo.addItem(constant);
        }
        return combo;
    }
        return new JTextField(12);
}

private Object readSimpleEditorValue(JComponent editor, Class<?> targetType) {
    if (editor instanceof JCheckBox checkBox) {
        return checkBox.isSelected();
    }
    if (editor instanceof JSpinner spinner) {
        Number number = (Number) spinner.getValue();
        if (targetType == int.class || targetType == Integer.class) return number.intValue();
        if (targetType == long.class || targetType == Long.class) return number.longValue();
        if (targetType == float.class || targetType == Float.class) return number.floatValue();
        if (targetType == double.class || targetType == Double.class) return number.doubleValue();
        if (targetType == short.class || targetType == Short.class) return number.shortValue();
        if (targetType == byte.class || targetType == Byte.class) return number.byteValue();
        return number;
    }
    if (editor instanceof JComboBox<?> combo) {
        return combo.getSelectedItem();
    }
    if (editor instanceof JTextField textField) {
        String text = textField.getText().trim();
        if (text.isEmpty()) return null;
        if (targetType == int.class || targetType == Integer.class) return Integer.parseInt(text);
        if (targetType == long.class || targetType == Long.class) return Long.parseLong(text);
        if (targetType == float.class || targetType == Float.class) return Float.parseFloat(text);
        if (targetType == double.class || targetType == Double.class) return Double.parseDouble(text);
        if (targetType == short.class || targetType == Short.class) return Short.parseShort(text);
        if (targetType == byte.class || targetType == Byte.class) return Byte.parseByte(text);
        if (targetType == boolean.class || targetType == Boolean.class) return Boolean.parseBoolean(text);
        return text;
    }
    return null;
}

private void resetSimpleEditor(JComponent editor) {
    if (editor instanceof JTextField textField) {
        textField.setText("");
    } else if (editor instanceof JSpinner spinner) {
        spinner.setValue(0);
    } else if (editor instanceof JCheckBox checkBox) {
        checkBox.setSelected(false);
    } else if (editor instanceof JComboBox<?> comboBox) {
        if (comboBox.getItemCount() > 0) {
            comboBox.setSelectedIndex(0);
        }
    }
}
    private Class<?> getGenericTypeOfField(Field f) {
        if (f.getGenericType() instanceof java.lang.reflect.ParameterizedType pt) {
            java.lang.reflect.Type arg = pt.getActualTypeArguments()[0];
            if (arg instanceof Class<?> c) return c;
        }
        return Object.class;
    }
private void loadTableData() {
    List<T> list = dao.findAll(10);   // profondeur 1 pour charger les relations
    tableModel.setRowCount(0);
    tableModel.setColumnCount(0);
    for (Field f : editableFields) {
        tableModel.addColumn(f.getName());
    }
    for (T entity : list) {
        Object[] row = new Object[editableFields.size()];
        for (int i = 0; i < editableFields.size(); i++) {
            try {
                Object val = editableFields.get(i).get(entity);
                if (val instanceof List<?> listVal) {
                    // Concaténer les éléments de la liste (ex: téléphones)
                    row[i] = listVal.stream()
                                .map(Object::toString)
                                .collect(Collectors.joining(" / "));
                } else if (val != null && val.getClass().isAnnotationPresent(Table.class)) {
                    row[i] = val.toString();
                } else {
                    row[i] = val;
                }
            } catch (Exception e) { row[i] = null; }
        }
        tableModel.addRow(row);
    }
}

    // ---- Utilitaire : méthode pour exposer getIdValue (utilisée dans populateForm) ----
    // On rend cette méthode package-private ou on l'ajoute au DAO de façon accessible
    // Ici on triche en instanciant un DAO temporaire, mais ça pourrait être optimisé.
    // C'est pour cela que dans populateForm on utilise dao.getIdValue (qui n'est pas public)
    // On va ajouter une méthode publique dans GenericDao:
    // public Object getIdValue(Object obj) { ... }
}


