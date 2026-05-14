package ui;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.*;

public class FormBuilder {
    private JPanel panel;
    private Map<String, JComponent> components = new LinkedHashMap<>();

    public FormBuilder() {
        panel = new JPanel(new java.awt.GridBagLayout());
    }

    public FormBuilder addField(String label, JComponent component) {
        // add to panel
        components.put(label, component);
        return this;
    }

    public JPanel build() {
        // layout the components
        return panel;
    }

    public Map<String, JComponent> getComponents() {
        return components;
    }
}