package ui.example;

import crud.CrudRepository;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import ui.DynamicCrudPanel;
import ui.listener.CrudFormListener;
import ui.listener.FormAction;
import ui.listener.FormEvent;
import ui.listener.FormListener;

/**
 * Exemple d'utilisation du système de listeners généralisés
 * et de la classe DynamicCrudPanel
 */
public class CrudFrameExample extends JFrame {
    
    private final DynamicCrudPanel<?> crudPanel;

    public <T> CrudFrameExample(Class<T> entityClass, CrudRepository<T> repository) {
        super("Gestion de " + entityClass.getSimpleName());
        
        // Créer le panneau CRUD complet
        crudPanel = new DynamicCrudPanel<>(entityClass);
        
        // Ajouter le listener CRUD automatique
        CrudFormListener crudListener = new CrudFormListener(repository);
        
        // Configurer les callbacks de succès/erreur
        crudListener.setSuccessCallback((event, entity) -> {
            String action = event.getAction().getLabel();
            JOptionPane.showMessageDialog(this, 
                action + " réussi(e) ! ",
                "Succès", 
                JOptionPane.INFORMATION_MESSAGE);
            
            // Rafraîchir la table si nécessaire
            if (event.getAction() == FormAction.SAVE || 
                event.getAction() == FormAction.UPDATE) {
                crudPanel.getFormPanel().resetForm();
            }
        });
        
        crudListener.setErrorCallback((event, exception) -> {
            JOptionPane.showMessageDialog(this,
                "Erreur : " + exception.getMessage(),
                "Erreur lors de " + event.getAction().getLabel(),
                JOptionPane.ERROR_MESSAGE);
        });
        
        // Ajouter le listener au formulaire
        crudPanel.getFormPanel().addFormListener(crudListener);
        
        // Ajouter un listener supplémentaire pour les logs
        crudPanel.getFormPanel().addFormListener(new FormListener() {
            @Override
            public void onFormAction(FormEvent event) {
                System.out.println("[LOG] Action: " + event.getAction());
                System.out.println("[LOG] Données: " + event.getFormData());
            }

            @Override
            public void onFormSuccess(FormEvent event) {
                System.out.println("[LOG] ✓ Action réussie: " + event.getAction());
            }

            @Override
            public void onFormError(FormEvent event, Exception exception) {
                System.out.println("[LOG] ✗ Erreur: " + exception.getMessage());
            }
        });
        
        // Ajouter le panneau à la fenêtre
        setContentPane(crudPanel);
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    /**
     * Charge les données initiales
     */
    public void loadData(java.util.List<?> entities) throws Exception {
        crudPanel.loadEntities(entities);
    }

    public DynamicCrudPanel<?> getCrudPanel() {
        return crudPanel;
    }
}