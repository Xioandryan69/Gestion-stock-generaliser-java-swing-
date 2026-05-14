package ui.example;

import crud.CrudRepository;
import crud.GenericDao;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import model.Personne;
import ui.DynamicCrudPanel;
import ui.listener.CrudFormListener;

/**
 * Application de démonstration pour la gestion des entités Personne.
 * Utilise les composants dynamiques (DynamicCrudPanel) et le DAO générique.
 */
public class PersonneExempleApp extends JFrame {

    private final DynamicCrudPanel<Personne> crudPanel;
    private final CrudRepository<Personne> PersonneDao;

    public PersonneExempleApp() throws Exception {
        super("Gestion des Personnes - Exemple CRUD dynamique");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);

        // Initialisation du DAO pour Personne
        PersonneDao = new GenericDao<>(Personne.class);

        // Panneau CRUD dynamique (formulaire + table)
        crudPanel = new DynamicCrudPanel<>(Personne.class);

        // Configuration du listener CRUD qui relie le formulaire à la base de données
        CrudFormListener crudListener = new CrudFormListener(PersonneDao);
        crudListener.setSuccessCallback((event, entity) -> {
            String action = event.getAction().getLabel();
            JOptionPane.showMessageDialog(this,
                    action + " effectué avec succès !",
                    "Succès", JOptionPane.INFORMATION_MESSAGE);
            // Rafraîchir l'affichage après modification
            refreshPersonnesList();
        });
        crudListener.setErrorCallback((event, exception) -> {
            JOptionPane.showMessageDialog(this,
                    "Erreur lors de " + event.getAction().getLabel() + " :\n" + exception.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
            exception.printStackTrace();
        });

        // Enregistrement du listener auprès du formulaire
        crudPanel.addFormListener(crudListener);

        // Ajout du panneau CRUD à la fenêtre
        setContentPane(crudPanel);

        // Chargement initial des données depuis la base
        refreshPersonnesList();
    }

    /**
     * Recharge la liste des adresses depuis la base de données
     * et met à jour la table.
     */
    private void refreshPersonnesList() {
        try {
            List<Personne> Personnes = PersonneDao.findAll(Personne.class);
            crudPanel.loadEntities(Personnes);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Impossible de charger les données : " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Optionnel : vérifier/initialiser la connexion à la base
                // (le DAO utilise la configuration par défaut de DatabaseConfig)
                new PersonneExempleApp().setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Erreur lors du démarrage de l'application :\n" + e.getMessage(),
                        "Erreur fatale", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}