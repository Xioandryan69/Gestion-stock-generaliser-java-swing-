package ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

import metadata.FieldInfo;
import metadata.TypeField;

/**
 * Crée le composant Swing adapté au TypeField d'un FieldInfo.
 *
 * Mapping :
 *   PRIMITIVE  → JTextField / JSpinner / JCheckBox
 *   ENUM       → JComboBox (peuplé avec les constantes)
 *   OBJECT     → JButton "Choisir <nom>"
 *   LIST       → JPanel avec JTable + boutons Ajouter/Supprimer
 *   MAP        → JButton "Modifier map…"
 */
public class ComponentFactory {

    /**
     * Point d'entrée principal.
     */
    public static JComponent createComponent(FieldInfo info) {
        if (info.getTypeField() == null) return new JTextField(15);

        switch (info.getTypeField()) {

            case PRIMITIVE:
                return createPrimitive(info);

            case ENUM:
                return createEnum(info);

            case OBJECT:
                return createObjectButton(info);

            case LIST:
                return createListPanel(info);

            case MAP:
                return new JButton("Modifier map : " + info.getNom() + "…");

            default:
                return new JTextField(15);
        }
    }

    // -------------------------------------------------------
    // PRIMITIVE
    // -------------------------------------------------------
    private static JComponent createPrimitive(FieldInfo info) {
        Class<?> t = info.getType();

        if (t == Boolean.class || t == boolean.class) {
            return new JCheckBox();
        }

        if (Number.class.isAssignableFrom(t)
                || t == int.class || t == double.class
                || t == float.class || t == long.class) {
            return new JSpinner(new SpinnerNumberModel(0, 0, Integer.MAX_VALUE, 1));
        }

        // String, char, Date, LocalDate, etc.
        return new JTextField(15);
    }

    // -------------------------------------------------------
    // ENUM
    // -------------------------------------------------------
    private static JComponent createEnum(FieldInfo info) {
        JComboBox<Object> combo = new JComboBox<>();
        if (info.getType() != null && info.getType().isEnum()) {
            for (Object constant : info.getType().getEnumConstants()) {
                combo.addItem(constant);
            }
        }
        return combo;
    }

    // -------------------------------------------------------
    // OBJECT (classe créée)
    // -------------------------------------------------------
    private static JComponent createObjectButton(FieldInfo info) {
        String label = "Choisir " + info.getNom()
                + (info.getType() != null ? " (" + info.getType().getSimpleName() + ")" : "");
        return new JButton(label);
    }

    // -------------------------------------------------------
    // LIST<T> → mini JTable dans un JPanel
    // -------------------------------------------------------
    private static JComponent createListPanel(FieldInfo info) {
        JPanel panel = new JPanel(new BorderLayout());

        String genericName = (info.getGenericType() != null)
                ? info.getGenericType().getSimpleName()
                : "Element";

        DefaultTableModel model = new DefaultTableModel(
                new Object[]{ genericName }, 0);
        JTable table = new JTable(model);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(200, 80));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton add = new JButton("+ Ajouter");
        JButton del = new JButton("- Supprimer");

        add.addActionListener(e -> model.addRow(new Object[]{ "" }));
        del.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) model.removeRow(row);
        });

        buttons.add(add);
        buttons.add(del);

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        panel.setBorder(BorderFactory.createTitledBorder(
                "Liste de " + genericName));

        return panel;
    }
}