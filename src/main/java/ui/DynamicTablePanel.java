package ui;

import crud.GenericDao;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

public class DynamicTablePanel extends JScrollPane {
    private final JTable table;
    private final DefaultTableModel model;
    private final Class<?> entityClass;
    private final GenericDao<?> dao;  // ← AJOUT
    private List<Object> entities = new ArrayList<>();

    public DynamicTablePanel(Class<?> entityClass) {
        this.entityClass = entityClass;
        this.dao = new GenericDao<>(entityClass);  // ← AJOUT : instancier le DAO
        
        List<String> columns = getDisplayColumns();
        model = new DefaultTableModel(columns.toArray(), 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setViewportView(table);
        
        // ← AJOUT CRITIQUE : charger les données au démarrage
        loadAllData();
    }

    // ← AJOUT : Nouvelle méthode pour charger les données
    private void loadAllData() {
        try {
            List<?> allEntities = dao.findAll(1);
            for (Object entity : allEntities) {
                addRow(entity);
            }
            System.out.println("✓ Données chargées : " + entities.size() + " entités");
        } catch (Exception e) {
            System.err.println("✗ Erreur lors du chargement : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<String> getDisplayColumns() {
        List<String> columns = new ArrayList<>();
        for (Field f : entityClass.getDeclaredFields()) {
            // Exclure les champs ignorés et les collections
            if (f.isAnnotationPresent(annotation.IgnoredField.class)) continue;
            //if (f.isAnnotationPresent(annotation.OneToMany.class)) continue;
            
            columns.add(f.getName());
        }
        return columns;
    }

    public void addRow(Object entity) throws Exception {
        entities.add(entity);
        
        List<String> columns = getDisplayColumns();
        Object[] rowData = new Object[columns.size()];
        
        int idx = 0;
        // Dans DynamicTablePanel.addRow()
        for (String colName : columns) {
            try {
                Field f = entityClass.getDeclaredField(colName);
                f.setAccessible(true);
                Object value = f.get(entity);
                if (value instanceof List<?> list) {
                    // Concaténer les numéros de téléphone
                    rowData[idx] = list.stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(" / "));
                } else if (value != null && value.getClass().isAnnotationPresent(annotation.Table.class)) {
                    rowData[idx] = value.toString();
                } else {
                    rowData[idx] = value != null ? value.toString() : "";
                }
            } catch (Exception e) {
                rowData[idx] = "N/A";
            }
            idx++;
        }
        model.addRow(rowData);
    }

    public void clearTable() {
        model.setRowCount(0);
        entities.clear();
    }

    public void refreshData(List<?> dataList) throws Exception {
        clearTable();
        for (Object entity : dataList) {
            addRow(entity);
        }
    }

    public Object getSelectedEntity() {
        int selectedRow = table.getSelectedRow();
        return (selectedRow >= 0 && selectedRow < entities.size()) 
            ? entities.get(selectedRow) 
            : null;
    }

    public int getSelectedRow() {
        return table.getSelectedRow();
    }

    public JTable getTable() {
        return table;
    }

    public DefaultTableModel getModel() {
        return model;
    }

    public int getRowCount() {
        return model.getRowCount();
    }
}