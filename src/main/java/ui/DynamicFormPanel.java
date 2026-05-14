package ui;
import annotation.Table;
import crud.GenericDao;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import metadata.FieldInfo;
import reflection.Analyzer;
import reflection.ReflectionUtils;
import ui.listener.FormAction;
import ui.listener.FormEvent;
import ui.listener.FormListener;
import ui.listener.FormListenerManager;

/**
 * Génère automatiquement un formulaire Swing à partir d'une classe Java.
 *
 * Utilisation :
 *   DynamicFormPanel panel = new DynamicFormPanel(Personne.class);
 *   panel.addFormListener(myListener);
 *
 * Chaque champ non @IgnoredField devient une ligne :
 *   label  →  composant adapté (JTextField, JComboBox, JSpinner, …)
 */
public class DynamicFormPanel extends JPanel {

    private final Class<?> clazz;
    private final List<FieldInfo> fields;

    // Garde une référence à chaque composant pour lire/écrire les valeurs
    private final Map<String, JComponent> componentMap = new LinkedHashMap<>();
    
    // Gestionnaire centralisé des listeners
    private final FormListenerManager listenerManager = new FormListenerManager();
    
    // Entité source (pour UPDATE/DELETE)
    private Object sourceEntity = null;
    
    // Boutons de la interface
    private JButton btnSave;
    private JButton btnUpdate;
    private JButton btnDelete;
    private JButton btnReset;

    public DynamicFormPanel(Class<?> clazz) {
        this.clazz  = clazz;
        this.fields = Analyzer.analyze(clazz);
        buildUI();
    }

    // -------------------------------------------------------
    // Construction de l'interface
    // -------------------------------------------------------
    private void buildUI() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder(
                "Formulaire : " + clazz.getSimpleName()));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        int row = 0;
        for (FieldInfo info : fields) {

            // Sauter les champs ignorés
            if (info.isIgnored()) continue;

            // Label
            gbc.gridx  = 0;
            gbc.gridy  = row;
            gbc.weightx = 0;
            String label = info.getNom()
                    + (info.isIdField() ? " [ID]" : "")
                    + " :";
            add(new JLabel(label), gbc);

            // Composant
            gbc.gridx   = 1;
            gbc.weightx = 1.0;
            JComponent comp = ComponentFactory.createComponent(info);
            add(comp, gbc);
            componentMap.put(info.getNom(), comp);

            row++;
        }

        // Paneau des boutons généralisé
        gbc.gridx  = 0;
        gbc.gridy  = row;
        gbc.gridwidth = 2;
        gbc.fill   = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel btnPanel = createButtonPanel();
        add(btnPanel, gbc);
    }

    // -------------------------------------------------------
    // Création du paneau des boutons
    // -------------------------------------------------------
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        
        btnSave = new JButton("💾 Sauvegarder");
        btnUpdate = new JButton("🔄 Mettre à jour");
        btnDelete = new JButton("🗑️ Supprimer");
        btnReset = new JButton("↻ Réinitialiser");

        btnSave.addActionListener(e -> fireFormAction(FormAction.SAVE));
        btnUpdate.addActionListener(e -> fireFormAction(FormAction.UPDATE));
        btnDelete.addActionListener(e -> fireFormAction(FormAction.DELETE));
        btnReset.addActionListener(e -> resetForm());

        panel.add(btnSave);
        panel.add(btnUpdate);
        panel.add(btnDelete);
        panel.add(btnReset);

        return panel;
    }

    // -------------------------------------------------------
    // Déclencher une action de formulaire
    // -------------------------------------------------------
    private void fireFormAction(FormAction action) {
        try {
            Map<String, Object> formData = getFormDataAsMap();
            FormEvent event = new FormEvent(action, clazz, formData, sourceEntity);
            listenerManager.fireFormEvent(event);
        } catch (Exception e) {
            FormEvent event = new FormEvent(FormAction.CUSTOM, clazz, new HashMap<>());
            listenerManager.fireFormError(event, e);
        }
    }

    // -------------------------------------------------------
    // Action Valider : affiche les valeurs (compatibilité)
    // -------------------------------------------------------
    @Deprecated
    protected void onValider() {
        fireFormAction(FormAction.SAVE);
    }

    // -------------------------------------------------------
    // Réinitialiser
    // -------------------------------------------------------
    public void resetForm() {
        for (Map.Entry<String, JComponent> entry : componentMap.entrySet()) {
            JComponent c = entry.getValue();
            if (c instanceof JTextField)   ((JTextField) c).setText("");
            else if (c instanceof JSpinner) ((JSpinner) c).setValue(0);
            else if (c instanceof JCheckBox) ((JCheckBox) c).setSelected(false);
            else if (c instanceof JComboBox) ((JComboBox<?>) c).setSelectedIndex(0);
        }
    }

    // -------------------------------------------------------
    // Remplir le formulaire depuis un objet existant
    // -------------------------------------------------------
    public void fillFrom(Object obj) {
        for (FieldInfo info : fields) {
            if (info.isIgnored()) continue;
            JComponent comp = componentMap.get(info.getNom());
            if (comp == null) continue;
            try {
                Object value = ReflectionUtils.getFieldValueByName(obj, info.getNom());
                if (value == null) continue;

                if (comp instanceof JTextField)
                    ((JTextField) comp).setText(value.toString());
                else if (comp instanceof JSpinner)
                    ((JSpinner) comp).setValue(value);
                else if (comp instanceof JCheckBox)
                    ((JCheckBox) comp).setSelected((Boolean) value);
                else if (comp instanceof JComboBox)
                    ((JComboBox<?>) comp).setSelectedItem(value);
            } catch (Exception ignored) {}
        }
    }

    // -------------------------------------------------------
    // Lire la valeur d'un composant
    // -------------------------------------------------------
    public Object getComponentValue(JComponent comp) {
        if (comp instanceof JTextField)  return ((JTextField) comp).getText();
        if (comp instanceof JSpinner)    return ((JSpinner) comp).getValue();
        if (comp instanceof JCheckBox)   return ((JCheckBox) comp).isSelected();
        if (comp instanceof JComboBox)   return ((JComboBox<?>) comp).getSelectedItem();
        return null;
    }

    // -------------------------------------------------------
    // Gestion des listeners
    // -------------------------------------------------------
    /**
     * Ajoute un listener qui écoute tous les événements
     */
    public void addFormListener(FormListener listener) {
        listenerManager.addFormListener(listener);
    }

    /**
     * Ajoute un listener pour une action spécifique (SAVE, UPDATE, DELETE)
     */
    public void addFormListener(FormAction action, FormListener listener) {
        listenerManager.addFormListener(action, listener);
    }

    /**
     * Retire un listener global
     */
    public void removeFormListener(FormListener listener) {
        listenerManager.removeFormListener(listener);
    }

    /**
     * Retire un listener d'une action spécifique
     */
    public void removeFormListener(FormAction action, FormListener listener) {
        listenerManager.removeFormListener(action, listener);
    }

    /**
     * Accès au gestionnaire pour un contrôle avancé
     */
    public FormListenerManager getListenerManager() {
        return listenerManager;
    }

    // -------------------------------------------------------
    // Récupération des données du formulaire
    // -------------------------------------------------------
    /**
     * Récupère toutes les valeurs du formulaire sous forme de Map
     */
    public Map<String, Object> getFormDataAsMap() {
        Map<String, Object> data = new LinkedHashMap<>();
        for (Map.Entry<String, JComponent> entry : componentMap.entrySet()) {
            data.put(entry.getKey(), getComponentValue(entry.getValue()));
        }
        return data;
    }

    /**
     * Définit l'entité source (pour UPDATE/DELETE)
     */
    public void setSourceEntity(Object entity) {
        this.sourceEntity = entity;
        fillFrom(entity);
    }

    /**
     * Récupère l'entité source
     */
    public Object getSourceEntity() {
        return sourceEntity;
    }

    /**
     * Active ou désactive les boutons UPDATE et DELETE selon le contexte
     */
    public void setEditMode(boolean enabled) {
        btnUpdate.setEnabled(enabled);
        btnDelete.setEnabled(enabled);
        btnSave.setEnabled(!enabled);
    }

    /**
     * Retourne l'état du mode édition
     */
    public boolean isEditMode() {
        return sourceEntity != null;
    }
    private static JComponent createObjectButton(FieldInfo info) {
    // Détecter si c'est une entité persistante
    if (info.getType().isAnnotationPresent(Table.class)) {
        // Créer un JComboBox similaire à DynamicCrudPanel
        return createEntityComboBox(info.getType());
    } else {
        return new JButton("Choisir " + info.getNom() + " (" + info.getType().getSimpleName() + ")");
    }
}

private static JComponent createEntityComboBox(Class<?> entityClass) {
    try {
        GenericDao<?> dao = new GenericDao<>(entityClass);
        List<?> items = dao.findAll();
        List<Object> comboItems = new ArrayList<>();
        comboItems.add(null);   // Option "Aucune"
        comboItems.addAll(items);
        
        JComboBox<Object> combo = new JComboBox<>(comboItems.toArray());
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                        int index, boolean isSelected, boolean cellHasFocus) {
                if (value == null) {
                    return super.getListCellRendererComponent(list, "(Aucune)", index, isSelected, cellHasFocus);
                }
                return super.getListCellRendererComponent(list, value.toString(), index, isSelected, cellHasFocus);
            }
        });
        return combo;
    } catch (Exception e) {
        e.printStackTrace();
        return new JComboBox<>(); // fallback
    }
}
}