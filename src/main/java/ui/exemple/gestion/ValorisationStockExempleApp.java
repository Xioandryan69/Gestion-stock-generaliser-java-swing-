package ui.exemple.gestion;

import config.DatabaseConfig;
import gestion.dao.LigneStockDao;
import gestion.dao.MouvementStockDao;
import gestion.dao.ProduitDao;
import gestion.service.StockService;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableModel;

/**
 * Fenêtre métier dynamique pour FIFO / LIFO / CUMP.
 * Toutes les données affichées viennent de la base de données.
 */
public class ValorisationStockExempleApp extends JFrame {

    private final JComboBox<ProduitItem> cmbProduit = new JComboBox<>();
    private final JSpinner spDate = new JSpinner(new SpinnerDateModel());

    private final JLabel lblQuantiteStock = new JLabel("0");
    private final JLabel lblValeurStock = new JLabel("0");
    private final JLabel lblCump = new JLabel("0");
    private final JLabel lblMethode = new JLabel("-");

    private final DefaultTableModel mouvementsModel = new DefaultTableModel(
            new String[]{"Date", "Type", "Qté", "PU", "Valeur", "Référence"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final DefaultTableModel lotsModel = new DefaultTableModel(
            new String[]{"Lot", "Date entrée", "Qté initiale", "Qté restante", "PU", "Valeur restante"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final DefaultTableModel syntheseModel = new DefaultTableModel(
            new String[]{"Date", "Quantité", "Valeur", "CUMP", "Méthode"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final DefaultTableModel fifoModel = new DefaultTableModel(
            new String[]{"Ordre", "Date", "Qté utilisée", "PU", "Coût"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final DefaultTableModel lifoModel = new DefaultTableModel(
            new String[]{"Ordre", "Date", "Qté utilisée", "PU", "Coût"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final DefaultTableModel cumpModel = new DefaultTableModel(
            new String[]{"Étape", "Qté", "Valeur", "CUMP"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public ValorisationStockExempleApp() {
        super("Valorisation du stock - FIFO / LIFO / CUMP");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1300, 820);
        setLocationRelativeTo(null);
        setContentPane(buildContent());

        configureInitialDate();
        wireEvents();
        loadProducts();
        refreshData();
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildTabs(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);
        return root;
    }

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 210, 210)),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));

        JLabel title = new JLabel("ETU 004367 Gestion article - FIFO / LIFO / CUMP", SwingConstants.LEFT);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        panel.add(title, BorderLayout.NORTH);

        JLabel subtitle = new JLabel("Les tableaux ci-dessous viennent de la base de données et du calcul métier réel.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(new Color(80, 80, 80));
        panel.add(subtitle, BorderLayout.SOUTH);
        return panel;
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Vue globale", buildGlobalPanel());
        tabs.addTab("Mouvements", buildTableContainer("Historique des mouvements", mouvementsModel));
        tabs.addTab("Lots restants", buildTableContainer("Lots restants", lotsModel));
        tabs.addTab("FIFO", buildTableContainer("Consommation FIFO", fifoModel));
        tabs.addTab("LIFO", buildTableContainer("Consommation LIFO", lifoModel));
        tabs.addTab("CUMP", buildTableContainer("Calcul CUMP", cumpModel));
        return tabs;
    }

    private JPanel buildGlobalPanel() {
        JPanel root = new JPanel(new BorderLayout(10, 10));

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        filterPanel.setBorder(BorderFactory.createTitledBorder("Filtre"));

        filterPanel.add(new JLabel("Produit :"));
        filterPanel.add(cmbProduit);

        filterPanel.add(new JLabel("Date :"));
        spDate.setEditor(new JSpinner.DateEditor(spDate, "dd/MM/yyyy"));
        filterPanel.add(spDate);

        JButton btnActualiser = new JButton("Actualiser");
        btnActualiser.addActionListener(e -> refreshData());
        filterPanel.add(btnActualiser);

        JButton btnGererProduits = new JButton("Gérer les produits");
        btnGererProduits.addActionListener(e -> ouvrirProduit());
        filterPanel.add(btnGererProduits);

        root.add(filterPanel, BorderLayout.NORTH);
        root.add(buildSummaryPanel(), BorderLayout.CENTER);
        return root;
    }

    private JPanel buildSummaryPanel() {
        JPanel root = new JPanel(new BorderLayout(10, 10));

        JPanel metrics = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        metrics.add(buildMetricCard("Quantité stock", lblQuantiteStock));
        metrics.add(buildMetricCard("Valeur stock", lblValeurStock));
        metrics.add(buildMetricCard("CUMP", lblCump));
        metrics.add(buildMetricCard("Méthode", lblMethode));

        root.add(metrics, BorderLayout.NORTH);
        root.add(buildTableContainer("Synthèse calculée depuis la base", syntheseModel), BorderLayout.CENTER);
        return root;
    }

    private JPanel buildMetricCard(String title, JLabel valueLabel) {
        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.setPreferredSize(new Dimension(220, 90));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        valueLabel.setForeground(new Color(30, 90, 120));

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildTableContainer(String title, DefaultTableModel model) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        JTable table = new JTable(model);
        table.setRowHeight(26);
        table.getTableHeader().setReorderingAllowed(false);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildFooter() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel footer = new JLabel("Valeur stock global = somme de la valeur stock de chaque article", SwingConstants.CENTER);
        footer.setFont(new Font("SansSerif", Font.PLAIN, 13));
        footer.setForeground(new Color(70, 70, 70));
        panel.add(footer, BorderLayout.CENTER);
        return panel;
    }

    private void configureInitialDate() {
        spDate.setValue(java.sql.Date.valueOf(LocalDate.now()));
    }

    private void wireEvents() {
        cmbProduit.addActionListener(e -> refreshData());
        spDate.addChangeListener(this::onDateChanged);
    }

    private void onDateChanged(ChangeEvent event) {
        refreshData();
    }

    private void loadProducts() {
        DefaultComboBoxModel<ProduitItem> model = new DefaultComboBoxModel<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "SELECT id, nom, methode_valorisation FROM produit WHERE actif = TRUE ORDER BY nom");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                model.addElement(new ProduitItem(
                        rs.getInt("id"),
                        rs.getString("nom"),
                        rs.getString("methode_valorisation")
                ));
            }
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this,
                    "Impossible de charger les produits : " + exception.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            exception.printStackTrace();
        }
        cmbProduit.setModel(model);
        if (model.getSize() > 0) {
            cmbProduit.setSelectedIndex(0);
        }
    }

    private ProduitItem getSelectedProduct() {
        return (ProduitItem) cmbProduit.getSelectedItem();
    }

    private LocalDate getSelectedDate() {
        Date date = (Date) spDate.getValue();
        return new java.sql.Date(date.getTime()).toLocalDate();
    }

    private void refreshData() {
        try {
            ProduitItem produit = getSelectedProduct();
            if (produit == null) {
                clearTables();
                return;
            }

            LocalDate date = getSelectedDate();
            loadMouvements(produit.id);
            loadLots(produit.id);
            loadSynthese(produit.id, date, produit.methodeValorisation);
            loadFifo(produit.id);
            loadLifo(produit.id);
            loadCump(produit.id);
            updateMetrics(produit.id, date, produit.methodeValorisation);
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this,
                    "Erreur de chargement depuis la base : " + exception.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            exception.printStackTrace();
        }
    }

    private void clearTables() {
        mouvementsModel.setRowCount(0);
        lotsModel.setRowCount(0);
        syntheseModel.setRowCount(0);
        fifoModel.setRowCount(0);
        lifoModel.setRowCount(0);
        cumpModel.setRowCount(0);
        lblQuantiteStock.setText("0");
        lblValeurStock.setText("0");
        lblCump.setText("0");
        lblMethode.setText("-");
    }

    private void loadMouvements(int idProduit) throws Exception {
        mouvementsModel.setRowCount(0);
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "SELECT date_mouvement, type_mouvement, quantite, prix_unitaire, valeur_total, " +
                             "COALESCE(reference_achat, reference_vente) AS reference " +
                             "FROM mouvement_stock WHERE id_produit = ? AND statut = 'VALIDÉ' ORDER BY date_mouvement DESC, id DESC")) {
            stmt.setInt(1, idProduit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    mouvementsModel.addRow(new Object[]{
                            rs.getDate("date_mouvement").toLocalDate(),
                            rs.getString("type_mouvement"),
                            rs.getBigDecimal("quantite"),
                            rs.getBigDecimal("prix_unitaire"),
                            rs.getBigDecimal("valeur_total"),
                            rs.getString("reference")
                    });
                }
            }
        }
    }

    private void loadLots(int idProduit) throws Exception {
        lotsModel.setRowCount(0);
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "SELECT id, date_entree, quantite_initiale, quantite_restante, prix_unitaire, valeur_restante " +
                             "FROM ligne_stock WHERE id_produit = ? AND quantite_restante > 0 ORDER BY date_entree ASC, id ASC")) {
            stmt.setInt(1, idProduit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lotsModel.addRow(new Object[]{
                            rs.getInt("id"),
                            rs.getDate("date_entree").toLocalDate(),
                            rs.getBigDecimal("quantite_initiale"),
                            rs.getBigDecimal("quantite_restante"),
                            rs.getBigDecimal("prix_unitaire"),
                            rs.getBigDecimal("valeur_restante")
                    });
                }
            }
        }
    }

    private void loadSynthese(int idProduit, LocalDate date, String methode) throws Exception {
        syntheseModel.setRowCount(0);
        StockSnapshot snapshot = computeSnapshot(idProduit, date);
        syntheseModel.addRow(new Object[]{
                date,
                snapshot.quantite,
                snapshot.valeur,
                snapshot.cump,
                methode
        });
    }

    private void loadFifo(int idProduit) throws Exception {
        fifoModel.setRowCount(0);
        List<StockLot> lots = loadLotsForConsumption(idProduit, true);
        BigDecimal reste = BigDecimal.valueOf(13);
        int ordre = 1;
        for (StockLot lot : lots) {
            if (reste.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal utilisee = lot.quantiteRestante.min(reste);
            fifoModel.addRow(new Object[]{ordre++, lot.dateEntree, utilisee, lot.prixUnitaire, utilisee.multiply(lot.prixUnitaire)});
            reste = reste.subtract(utilisee);
        }
    }

    private void loadLifo(int idProduit) throws Exception {
        lifoModel.setRowCount(0);
        List<StockLot> lots = loadLotsForConsumption(idProduit, false);
        BigDecimal reste = BigDecimal.valueOf(13);
        int ordre = 1;
        for (StockLot lot : lots) {
            if (reste.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal utilisee = lot.quantiteRestante.min(reste);
            lifoModel.addRow(new Object[]{ordre++, lot.dateEntree, utilisee, lot.prixUnitaire, utilisee.multiply(lot.prixUnitaire)});
            reste = reste.subtract(utilisee);
        }
    }

    private void loadCump(int idProduit) throws Exception {
        cumpModel.setRowCount(0);
        List<StockLot> lots = loadLotsForConsumption(idProduit, true);
        BigDecimal quantite = BigDecimal.ZERO;
        BigDecimal valeur = BigDecimal.ZERO;
        int step = 1;
        for (StockLot lot : lots) {
            quantite = quantite.add(lot.quantiteRestante);
            valeur = valeur.add(lot.quantiteRestante.multiply(lot.prixUnitaire));
            BigDecimal cump = quantite.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : valeur.divide(quantite, 4, RoundingMode.HALF_UP);
            cumpModel.addRow(new Object[]{"Étape " + step++, quantite, valeur, cump});
        }
    }

    private void updateMetrics(int idProduit, LocalDate date, String methode) throws Exception {
        StockSnapshot snapshot = computeSnapshot(idProduit, date);
        lblQuantiteStock.setText(snapshot.quantite.toPlainString());
        lblValeurStock.setText(snapshot.valeur.toPlainString());
        lblCump.setText(snapshot.cump.toPlainString());
        lblMethode.setText(methode);
    }

    private StockSnapshot computeSnapshot(int idProduit, LocalDate date) throws Exception {
        try (Connection connection = DatabaseConfig.getConnection()) {
            StockService service = new StockService(new ProduitDao(connection), new MouvementStockDao(connection), new LigneStockDao(connection));
            gestion.service.StockService.EtatStock etat = service.computeSnapshotAtDate(idProduit, date, null);
            return new StockSnapshot(etat.getQuantite(), etat.getValeurStock(), etat.getCump());
        }
    }

    private List<StockLot> loadLotsForConsumption(int idProduit, boolean ascending) throws Exception {
        List<StockLot> lots = new ArrayList<>();
        String order = ascending ? "ASC" : "DESC";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "SELECT date_entree, quantite_restante, prix_unitaire " +
                             "FROM ligne_stock WHERE id_produit = ? AND quantite_restante > 0 ORDER BY date_entree " + order + ", id " + order)) {
            stmt.setInt(1, idProduit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lots.add(new StockLot(
                            rs.getDate("date_entree").toLocalDate(),
                            rs.getBigDecimal("quantite_restante"),
                            rs.getBigDecimal("prix_unitaire")
                    ));
                }
            }
        }
        return lots;
    }

    private void ouvrirProduit() {
        try {
            new ProduitExempleApp().setVisible(true);
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this,
                    "Impossible d'ouvrir la fenêtre produit : " + exception.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new ValorisationStockExempleApp().setVisible(true);
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(null,
                        "Erreur lors du démarrage : " + exception.getMessage(),
                        "Erreur fatale",
                        JOptionPane.ERROR_MESSAGE);
                exception.printStackTrace();
            }
        });
    }

    private static final class ProduitItem {
        private final int id;
        private final String nom;
        private final String methodeValorisation;

        private ProduitItem(int id, String nom, String methodeValorisation) {
            this.id = id;
            this.nom = nom;
            this.methodeValorisation = methodeValorisation;
        }

        @Override
        public String toString() {
            return nom + " [" + methodeValorisation + "]";
        }
    }

    private static final class StockLot {
        private final LocalDate dateEntree;
        private final BigDecimal quantiteRestante;
        private final BigDecimal prixUnitaire;

        private StockLot(LocalDate dateEntree, BigDecimal quantiteRestante, BigDecimal prixUnitaire) {
            this.dateEntree = dateEntree;
            this.quantiteRestante = quantiteRestante;
            this.prixUnitaire = prixUnitaire;
        }
    }

    private static final class StockSnapshot {
        private final BigDecimal quantite;
        private final BigDecimal valeur;
        private final BigDecimal cump;

        private StockSnapshot(BigDecimal quantite, BigDecimal valeur, BigDecimal cump) {
            this.quantite = quantite;
            this.valeur = valeur;
            this.cump = cump;
        }
    }
}
