package ui.exemple.gestion;

import crud.CrudRepository;
import crud.GenericDao;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import modele.LigneStock;
import ui.DynamicCrudPanel;
import ui.listener.CrudFormListener;

/**
 * Application de démonstration pour l'état du stock.
 * Utilise le même modèle que PersonneExempleApp avec DynamicCrudPanel.
 * Ici, on affiche les lots de stock (LigneStock) comme base de l'état du stock.
 */
public class EtatStockExempleApp extends JFrame {

    private final DynamicCrudPanel<LigneStock> crudPanel;
    private final CrudRepository<LigneStock> ligneStockDao;

    public EtatStockExempleApp() throws Exception {
        super("État du Stock - Exemple CRUD dynamique");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);

        ligneStockDao = new GenericDao<>(LigneStock.class);
        crudPanel = new DynamicCrudPanel<>(LigneStock.class);

        CrudFormListener crudListener = new CrudFormListener(ligneStockDao);
        crudListener.setSuccessCallback((event, entity) -> {
            String action = event.getAction().getLabel();
            JOptionPane.showMessageDialog(this,
                    action + " effectué avec succès !",
                    "Succès",
                    JOptionPane.INFORMATION_MESSAGE);
            refreshLignesList();
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

        refreshLignesList();
    }

    private void refreshLignesList() {
        try {
            List<LigneStock> lignes = ligneStockDao.findAll(LigneStock.class);
            crudPanel.loadEntities(lignes);
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
                new EtatStockExempleApp().setVisible(true);
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
