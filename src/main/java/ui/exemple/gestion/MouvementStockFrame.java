package ui.exemple.gestion;

import config.DatabaseConfig;
import gestion.dao.LigneStockDao;
import gestion.dao.MouvementStockDao;
import gestion.dao.ProduitDao;
import gestion.service.StockService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import modele.MouvementStock;
import modele.Produit;

/**
 * Frame for creating and listing stock movements (ENTREE / SORTIE)
 */
public class MouvementStockFrame extends JFrame {

    private final Connection connection;
    private final ProduitDao produitDao;
    private final MouvementStockDao mouvementDao;
    private final LigneStockDao ligneStockDao;
    private final StockService stockService;

    private final JComboBox<Produit> comboProduit = new JComboBox<>();
    private final JComboBox<String> comboType = new JComboBox<>(new String[]{"ENTREE", "SORTIE"});
    private final JComboBox<String> comboMethode = new JComboBox<>(new String[]{"PRODUIT", "FIFO", "LIFO", "CUMP"});
    private final JTextField txtQuantite = new JTextField();
    private final JTextField txtPrix = new JTextField();
    private final JTextField txtReference = new JTextField();
    private final JSpinner spDate = new JSpinner(new SpinnerDateModel());

    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Produit", "Type", "Quantité", "PU", "Date"}, 0);
    private final JTable table = new JTable(model);

    public MouvementStockFrame() throws Exception {
        super("Mouvements stock");
        this.connection = DatabaseConfig.getConnection();
        this.produitDao = new ProduitDao(connection);
        this.mouvementDao = new MouvementStockDao(connection);
        this.ligneStockDao = new LigneStockDao(connection);
        this.stockService = new StockService(produitDao, mouvementDao, ligneStockDao);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        initComponents();
        loadProduits();
        refreshMouvements();

        // Close connection when window disposed
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                try { connection.close(); } catch (Exception ex) { /* ignore */ }
            }
        });
    }

    private void initComponents() {
        setLayout(new BorderLayout(8, 8));

        JPanel form = new JPanel(new GridLayout(6, 2, 8, 8));

        form.add(new JLabel("Produit"));
        form.add(comboProduit);

        form.add(new JLabel("Type"));
        form.add(comboType);

        form.add(new JLabel("Méthode"));
        form.add(comboMethode);

        form.add(new JLabel("Quantité"));
        form.add(txtQuantite);

        form.add(new JLabel("Prix unitaire"));
        form.add(txtPrix);

        JButton btnCalcPU = new JButton("Calculer PU");
        btnCalcPU.addActionListener(e -> computePrixUnitaire());
        form.add(btnCalcPU);
        // placeholder to keep grid alignment
        form.add(new JLabel());

        form.add(new JLabel("Date"));
        spDate.setEditor(new javax.swing.JSpinner.DateEditor(spDate, "dd/MM/yyyy"));
        spDate.setValue(new Date());
        form.add(spDate);

        form.add(new JLabel("Référence"));
        form.add(txtReference);

        JButton btnSave = new JButton("Enregistrer");
        btnSave.addActionListener(e -> onSave());
        form.add(btnSave);

        JButton btnRefresh = new JButton("Actualiser");
        btnRefresh.addActionListener(e -> refreshMouvements());
        form.add(btnRefresh);

        add(form, BorderLayout.NORTH);

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(new JPanel(new FlowLayout(FlowLayout.RIGHT)), BorderLayout.SOUTH);

        comboType.addActionListener(e -> togglePrixVisibility());
        comboMethode.addActionListener(e -> {
            // when user changes method, if it's a sortie, recompute preview
            if ("SORTIE".equals(comboType.getSelectedItem())) {
                computePrixUnitaire();
            }
        });
        comboProduit.addActionListener(e -> onProduitChange());
        togglePrixVisibility();
    }

    private void onProduitChange() {
        Produit p = (Produit) comboProduit.getSelectedItem();
        if (p != null && p.getMethodeValorisation() != null) {
            comboMethode.setSelectedItem("PRODUIT");
        }
    }

    private void togglePrixVisibility() {
        boolean entree = "ENTREE".equals(comboType.getSelectedItem());
        txtPrix.setEditable(entree);
        txtPrix.setVisible(true); // always visible so user can see computed PU for SORTIE
    }

    private void computePrixUnitaire() {
        try {
            if (!"SORTIE".equals(comboType.getSelectedItem())) {
                JOptionPane.showMessageDialog(this, "La calculateur de PU est pour les sorties uniquement.", "Info", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            Produit produit = (Produit) comboProduit.getSelectedItem();
            if (produit == null) {
                JOptionPane.showMessageDialog(this, "Sélectionnez un produit d'abord.", "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }
            BigDecimal quantite = new BigDecimal(txtQuantite.getText().trim());
            if (quantite.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException("Quantité <= 0");

            String methode = (String) comboMethode.getSelectedItem();
            if ("PRODUIT".equals(methode)) methode = produit.getMethodeValorisation();

            BigDecimal total = stockService.estimateSortieCost(produit.getId(), quantite, methode);
            java.math.BigDecimal pu = total.divide(quantite, 6, java.math.RoundingMode.HALF_UP);
            txtPrix.setText(pu.stripTrailingZeros().toPlainString());
            txtPrix.setEditable(false);
        } catch (NumberFormatException nf) {
            JOptionPane.showMessageDialog(this, "Quantité invalide pour calcul.", "Erreur", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Impossible d'estimer le PU: " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadProduits() {
        try {
            comboProduit.removeAllItems();
            List<Produit> produits = produitDao.findAll();
            for (Produit p : produits) comboProduit.addItem(p);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Impossible de charger les produits: " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onSave() {
        try {
            Produit produit = (Produit) comboProduit.getSelectedItem();
            if (produit == null) throw new Exception("Produit requis");

            BigDecimal quantite = new BigDecimal(txtQuantite.getText().trim());
            if (quantite.compareTo(BigDecimal.ZERO) <= 0) throw new Exception("Quantité invalide");

            String type = (String) comboType.getSelectedItem();
            String reference = txtReference.getText().trim();
            Date d = (Date) spDate.getValue();
            LocalDate date = d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

            if ("ENTREE".equals(type)) {
                BigDecimal prix = new BigDecimal(txtPrix.getText().trim());
                stockService.entreeStock(produit.getId(), quantite, prix, reference, date);
            } else {
                String methode = (String) comboMethode.getSelectedItem();
                if ("PRODUIT".equals(methode)) methode = null;
                stockService.sortieStock(produit.getId(), quantite, reference, date, methode);
            }

            JOptionPane.showMessageDialog(this, "Mouvement enregistré", "Succès", JOptionPane.INFORMATION_MESSAGE);
            clearForm();
            refreshMouvements();
        } catch (NumberFormatException nf) {
            JOptionPane.showMessageDialog(this, "Format numérique invalide", "Erreur", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erreur: " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void clearForm() {
        txtQuantite.setText("");
        txtPrix.setText("");
        txtReference.setText("");
        spDate.setValue(new Date());
    }

    private void refreshMouvements() {
        model.setRowCount(0);
        try {
            List<MouvementStock> mouvements = mouvementDao.findAll();
            for (MouvementStock m : mouvements) {
                Produit p = produitDao.findById(m.getIdProduit());
                model.addRow(new Object[]{p != null ? p.getNom() : ("#" + m.getIdProduit()), m.getTypeMouvement(), m.getQuantite(), m.getPrixUnitaire(), m.getDateMouvement()});
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Impossible de charger mouvements: " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new MouvementStockFrame().setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Erreur démarrage: " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
