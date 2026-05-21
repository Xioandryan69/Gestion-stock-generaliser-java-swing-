package ui.example;

import crud.CrudRepository;
import crud.GenericDao;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import modele.MouvementStock;
import ui.DynamicCrudPanel;
import ui.listener.CrudFormListener;

/**
 * Application de démonstration pour la gestion des entités MouvementStock.
 * Utilise les composants dynamiques (DynamicCrudPanel) et le DAO générique.
 */
public class MouvementStockExempleApp extends JFrame {

    private final DynamicCrudPanel<MouvementStock> crudPanel;
    private final CrudRepository<MouvementStock> MouvementStockDao;
    private final JButton btnAjouterPlusieurs = new JButton("Ajouter plusieurs");

    public MouvementStockExempleApp() throws Exception {
        super("Gestion des MouvementStocks - Exemple CRUD dynamique");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);

        // Initialisation du DAO pour MouvementStock
        MouvementStockDao = new GenericDao<>(MouvementStock.class);

        // Panneau CRUD dynamique (formulaire + table)
        crudPanel = new DynamicCrudPanel<>(MouvementStock.class);

        // Configuration du listener CRUD qui relie le formulaire à la base de données
        CrudFormListener crudListener = new CrudFormListener(MouvementStockDao);
        crudListener.setSuccessCallback((event, entity) -> {
            String action = event.getAction().getLabel();
            JOptionPane.showMessageDialog(this,
                    action + " effectué avec succès !",
                    "Succès", JOptionPane.INFORMATION_MESSAGE);
            // Rafraîchir l'affichage après modification
            refreshMouvementStocksList();
        });
        crudListener.setErrorCallback((event, exception) -> {
            JOptionPane.showMessageDialog(this,
                    "Erreur lors de " + event.getAction().getLabel() + " :\n" + exception.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
            exception.printStackTrace();
        });

        // Enregistrement du listener auprès du formulaire
        crudPanel.addFormListener(crudListener);

        btnAjouterPlusieurs.addActionListener(e -> crudPanel.openBatchAddDialog());

        JPanel root = new JPanel(new BorderLayout(8, 8));
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.add(btnAjouterPlusieurs);
        root.add(toolbar, BorderLayout.NORTH);
        root.add(crudPanel, BorderLayout.CENTER);
        setContentPane(root);

        // Chargement initial des données depuis la base
        refreshMouvementStocksList();
    }

    /**
     * Recharge la liste des adresses depuis la base de données
     * et met à jour la table.
     */
    private void refreshMouvementStocksList() {
        try {
            List<MouvementStock> MouvementStocks = MouvementStockDao.findAll(MouvementStock.class);
            crudPanel.loadEntities(MouvementStocks);
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
                new MouvementStockExempleApp().setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Erreur lors du démarrage de l'application :\n" + e.getMessage(),
                        "Erreur fatale", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}