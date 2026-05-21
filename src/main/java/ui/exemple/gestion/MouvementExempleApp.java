package ui.exemple.gestion;

import config.DatabaseConfig;
import gestion.dao.LigneStockDao;
import gestion.dao.MouvementStockDao;
import gestion.dao.ProduitDao;
import gestion.service.StockService;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import modele.MouvementStock;
import modele.Produit;
import ui.DynamicTablePanel;
import utils.StringUtils;

/**
 * Écran métier pour saisir plusieurs mouvements avant validation.
 * La logique métier StockService est conservée et appliquée ligne par ligne.
 */
public class MouvementExempleApp extends JFrame {

    private final DynamicTablePanel tablePanel = new DynamicTablePanel(MouvementStock.class);
    private final JLabel lblResultat = new JLabel("Prêt");
    private final JButton btnAjouterMouvement = new JButton("Ajouter un mouvement");
    private final JButton btnValiderMouvements = new JButton("Valider tous les mouvements");
    private final JPanel mouvementsContainer = new JPanel();
    private final List<MouvementRowPanel> mouvements = new ArrayList<>();
    private final List<Produit> produitsDisponibles = new ArrayList<>();

    public MouvementExempleApp() {
        super("Gestion des Mouvements de Stock");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 820);
        setLocationRelativeTo(null);

        setContentPane(buildContent());
        refreshProducts();
        ajouterMouvement();
        refreshMouvementsList();
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildBody(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);
        return root;
    }

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        JLabel title = new JLabel("Entrée / Sortie de stock");
        title.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 20));
        panel.add(title, BorderLayout.NORTH);
        panel.add(new JLabel("Ajoute plusieurs mouvements, puis valide une seule fois. Chaque ligne garde son produit, sa quantité, sa date et sa méthode."), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildBody() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.add(buildFormPanel(), BorderLayout.NORTH);
        panel.add(new JScrollPane(tablePanel), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildFormPanel() {
        JPanel form = new JPanel(new BorderLayout(8, 8));
        form.setBorder(BorderFactory.createTitledBorder("Saisie des mouvements"));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JButton btnRafraichir = new JButton("Rafraîchir la table");
        JButton btnReset = new JButton("Réinitialiser");

        btnAjouterMouvement.addActionListener(e -> ajouterMouvement());
        btnValiderMouvements.addActionListener(e -> validerTousLesMouvements());
        btnRafraichir.addActionListener(e -> refreshMouvementsList());
        btnReset.addActionListener(e -> resetForm());

        buttons.add(btnAjouterMouvement);
        buttons.add(btnValiderMouvements);
        buttons.add(btnRafraichir);
        buttons.add(btnReset);
        form.add(buttons, BorderLayout.NORTH);

        mouvementsContainer.setLayout(new BoxLayout(mouvementsContainer, BoxLayout.Y_AXIS));
        mouvementsContainer.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JScrollPane rowsScroll = new JScrollPane(mouvementsContainer);
        rowsScroll.setPreferredSize(new Dimension(0, 360));
        form.add(rowsScroll, BorderLayout.CENTER);

        return form;
    }

    private JPanel buildFooter() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        panel.add(lblResultat, BorderLayout.WEST);
        return panel;
    }

    private void ajouterMouvement() {
        MouvementRowPanel row = new MouvementRowPanel(this::supprimerMouvement, this::calculerPU);
        row.applyProducts(produitsDisponibles);
        mouvements.add(row);
        mouvementsContainer.add(row);
        refreshRowTitles();
        mouvementsContainer.revalidate();
        mouvementsContainer.repaint();
    }

    private void supprimerMouvement(MouvementRowPanel row) {
        if (mouvements.size() <= 1) {
            row.resetRow();
            return;
        }

        mouvements.remove(row);
        mouvementsContainer.remove(row);
        refreshRowTitles();
        mouvementsContainer.revalidate();
        mouvementsContainer.repaint();
    }

    private void refreshRowTitles() {
        for (int i = 0; i < mouvements.size(); i++) {
            mouvements.get(i).setTitleText("Mouvement " + (i + 1));
        }
    }

    private void refreshProducts() {
        try (Connection connection = DatabaseConfig.getConnection()) {
            ProduitDao produitDao = new ProduitDao(connection);
            produitsDisponibles.clear();
            produitsDisponibles.addAll(produitDao.findAll());
            for (MouvementRowPanel row : mouvements) {
                row.applyProducts(produitsDisponibles);
            }
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this,
                    "Impossible de charger les produits : " + exception.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            exception.printStackTrace();
        }
    }

    private void refreshMouvementsList() {
        try (Connection connection = DatabaseConfig.getConnection()) {
            MouvementStockDao mouvementDao = new MouvementStockDao(connection);
            tablePanel.refreshData(mouvementDao.findAll());
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this,
                    "Impossible de charger les mouvements : " + exception.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            exception.printStackTrace();
        }
    }

    private void validerTousLesMouvements() {
        if (mouvements.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ajoute au moins un mouvement.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try (Connection connection = DatabaseConfig.getConnection()) {
            connection.setAutoCommit(false);

            StockService service = new StockService(
                    new ProduitDao(connection),
                    new MouvementStockDao(connection),
                    new LigneStockDao(connection));
            MouvementStockDao mouvementDao = new MouvementStockDao(connection);

            int processed = 0;
            for (MouvementRowPanel row : new ArrayList<>(mouvements)) {
                MouvementEntry input = row.readInput(this);
                if (input == null) {
                    connection.rollback();
                    return;
                }

                if ("ENTREE".equals(input.getType())) {
                    service.entreeStock(input.getProduit().getId(), input.getQuantite(), input.getPrixUnitaire(), input.getReference(), input.getDate());
                } else {
                    service.sortieStock(input.getProduit().getId(), input.getQuantite(), input.getReference(), input.getDate(), input.getMethode());
                }

                if (!StringUtils.isEmpty(input.getNotes())) {
                    List<MouvementStock> savedMovements = mouvementDao.findByProduitId(input.getProduit().getId());
                    for (MouvementStock mouvement : savedMovements) {
                        boolean sameType = input.getType().equalsIgnoreCase(mouvement.getTypeMouvement());
                        boolean sameQuantity = input.getQuantite().compareTo(mouvement.getQuantite() != null ? mouvement.getQuantite() : BigDecimal.ZERO) == 0;
                        boolean sameDate = input.getDate().equals(mouvement.getDateMouvement());
                        boolean sameReference = input.getReference().equals(MouvementFormUtils.firstNonBlank(mouvement.getReferenceAchat(), mouvement.getReferenceVente()));
                        if (sameType && sameQuantity && sameDate && sameReference) {
                            mouvement.setNotes(input.getNotes());
                            mouvementDao.update(mouvement);
                            break;
                        }
                    }
                }

                processed++;
            }

            connection.commit();
            lblResultat.setText(processed + " mouvement(s) enregistré(s).");
            refreshProducts();
            refreshMouvementsList();
            resetForm();
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this,
                    "Impossible de valider les mouvements :\n" + exception.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            lblResultat.setText("Erreur lors de la validation");
            exception.printStackTrace();
        }
    }

    private void resetForm() {
        mouvements.clear();
        mouvementsContainer.removeAll();
        ajouterMouvement();
        mouvementsContainer.revalidate();
        mouvementsContainer.repaint();
    }

    private void calculerPU(MouvementRowPanel row) {
        if (!"SORTIE".equals(row.getTypeSelection())) {
            JOptionPane.showMessageDialog(this, "Calcul PU réservé aux sorties.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        Produit produit = row.getProduitSelectionne();
        if (produit == null) {
            JOptionPane.showMessageDialog(this, "Sélectionner un produit.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        BigDecimal quantite = MouvementFormUtils.parseBigDecimal(this, row.getQuantiteText(), "quantité");
        if (quantite == null) {
            return;
        }

        String methode = row.getMethodeSelectionnee();
        if ("PRODUIT".equals(methode)) {
            methode = produit.getMethodeValorisation();
        }

        try (Connection connection = DatabaseConfig.getConnection()) {
            StockService service = new StockService(new ProduitDao(connection), new MouvementStockDao(connection), new LigneStockDao(connection));
            BigDecimal total = service.estimateSortieCost(produit.getId(), quantite, methode);
            BigDecimal pu = total.divide(quantite, 6, RoundingMode.HALF_UP);
            row.setPrixText(pu.stripTrailingZeros().toPlainString());
            row.setPrixEditable(false);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Impossible d'estimer PU: " + ex.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new MouvementExempleApp().setVisible(true);
            } catch (Exception exception) {
                exception.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Erreur lors du démarrage de l'application :\n" + exception.getMessage(),
                        "Erreur fatale",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
