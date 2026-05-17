package ui.example;

import crud.CrudRepository;
import crud.GenericDao;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import model.Addresse;
import ui.DynamicCrudPanel;
import ui.listener.CrudFormListener;

/**
 * Application de démonstration pour la gestion des entités Addresse.
 * Utilise les composants dynamiques (DynamicCrudPanel) et le DAO générique.
 */
public class AddresseExempleApp extends JFrame {

    private final DynamicCrudPanel<Addresse> crudPanel;
    private final CrudRepository<Addresse> AddresseDao;

    public AddresseExempleApp() throws Exception {
        super("Gestion des Addresses - Exemple CRUD dynamique");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);

        // Initialisation du DAO pour Addresse
        AddresseDao = new GenericDao<>(Addresse.class);

        // Panneau CRUD dynamique (formulaire + table)
        crudPanel = new DynamicCrudPanel<>(Addresse.class);

        // Configuration du listener CRUD qui relie le formulaire à la base de données
        CrudFormListener crudListener = new CrudFormListener(AddresseDao);
        crudListener.setSuccessCallback((event, entity) -> {
            String action = event.getAction().getLabel();
            JOptionPane.showMessageDialog(this,
                    action + " effectué avec succès !",
                    "Succès", JOptionPane.INFORMATION_MESSAGE);
            // Rafraîchir l'affichage après modification
            refreshAddressesList();
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
        refreshAddressesList();
    }

    /**
     * Recharge la liste des adresses depuis la base de données
     * et met à jour la table.
     */
    private void refreshAddressesList() {
        try {
            List<Addresse> Addresses = AddresseDao.findAll(Addresse.class);
            crudPanel.loadEntities(Addresses);
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
                new AddresseExempleApp().setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Erreur lors du démarrage de l'application :\n" + e.getMessage(),
                        "Erreur fatale", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}