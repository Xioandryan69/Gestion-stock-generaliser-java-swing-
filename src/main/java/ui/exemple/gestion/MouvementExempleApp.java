package ui.exemple.gestion;

import crud.CrudRepository;
import crud.GenericDao;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import modele.MouvementStock;
import ui.DynamicCrudPanel;
import ui.listener.CrudFormListener;

/**
 * Application de démonstration pour la gestion des mouvements de stock.
 * Utilise le même modèle que PersonneExempleApp avec DynamicCrudPanel.
 */
public class MouvementExempleApp extends JFrame {

    private final DynamicCrudPanel<MouvementStock> crudPanel;
    private final CrudRepository<MouvementStock> mouvementDao;

    public MouvementExempleApp() throws Exception {
        super("Gestion des Mouvements de Stock - Exemple CRUD dynamique");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);

        mouvementDao = new GenericDao<>(MouvementStock.class);
        crudPanel = new DynamicCrudPanel<>(MouvementStock.class);

        CrudFormListener crudListener = new CrudFormListener(mouvementDao);
        crudListener.setSuccessCallback((event, entity) -> {
            String action = event.getAction().getLabel();
            JOptionPane.showMessageDialog(this,
                    action + " effectué avec succès !",
                    "Succès",
                    JOptionPane.INFORMATION_MESSAGE);
            refreshMouvementsList();
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

        refreshMouvementsList();
    }

    private void refreshMouvementsList() {
        try {
            List<MouvementStock> mouvements = mouvementDao.findAll(MouvementStock.class);
            crudPanel.loadEntities(mouvements);
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
                new MouvementExempleApp().setVisible(true);
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
