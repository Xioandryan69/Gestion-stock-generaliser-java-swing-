package ui;

import javax.swing.*;

public class DynamicFrame extends JFrame {
    private DynamicFormPanel formPanel;
    private DynamicTablePanel tablePanel;

    public DynamicFrame(Class<?> entityClass) {
        super("Gestion de " + entityClass.getSimpleName());
        formPanel = new DynamicFormPanel(entityClass);
        tablePanel = new DynamicTablePanel(entityClass);
        // Example: use a tabbed pane
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Formulaire", formPanel);
        tabs.addTab("Liste", tablePanel);
        setContentPane(tabs);
        pack();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    public void addRow(Object entity) throws Exception {
        tablePanel.addRow(entity);
    }
}