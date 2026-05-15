package ui.exemple.gestion;

import crud.CrudRepository;
import crud.GenericDao;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import modele.Produit;
import ui.DynamicCrudPanel;
import ui.listener.CrudFormListener;

/**
 * Application de démonstration pour la gestion des entités Produit.
 * Utilise le même modèle que PersonneExempleApp avec DynamicCrudPanel.
 */
public class ProduitExempleApp extends JFrame {

    private final DynamicCrudPanel<Produit> crudPanel;
    private final CrudRepository<Produit> produitDao;

    public ProduitExempleApp() throws Exception {
        super("Gestion des Produits - Exemple CRUD dynamique");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);

        produitDao = new GenericDao<>(Produit.class);
        crudPanel = new DynamicCrudPanel<>(Produit.class);

        CrudFormListener crudListener = new CrudFormListener(produitDao);
        crudListener.setSuccessCallback((event, entity) -> {
            String action = event.getAction().getLabel();
            JOptionPane.showMessageDialog(this,
                    action + " effectué avec succès !",
                    "Succès",
                    JOptionPane.INFORMATION_MESSAGE);
            refreshProduitsList();
        });
        crudListener.setErrorCallback((event, exception) -> {
            JOptionPane.showMessageDialog(this,
                    "Erreur lors de " + event.getAction().getLabel() + " :\n" + exception.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            exception.printStackTrace();
        });

        crudPanel.addFormListener(crudListener);
        setContentPane(crudPanel);

        refreshProduitsList();
    }

    private void refreshProduitsList() {
        try {
            List<Produit> produits = produitDao.findAll(Produit.class);
            crudPanel.loadEntities(produits);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Impossible de charger les données : " + e.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new ProduitExempleApp().setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Erreur lors du démarrage de l'application :\n" + e.getMessage(),
                        "Erreur fatale",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
