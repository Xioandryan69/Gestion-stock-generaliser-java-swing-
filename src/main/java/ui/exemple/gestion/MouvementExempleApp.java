package ui.exemple.gestion;

import config.DatabaseConfig;
import gestion.dao.LigneStockDao;
import gestion.dao.MouvementStockDao;
import gestion.dao.ProduitDao;
import gestion.service.StockService;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.event.ListSelectionEvent;
import modele.MouvementStock;
import modele.Produit;
import ui.DynamicTablePanel;

/**
 * Écran métier pour saisir une entrée ou une sortie de stock.
 * Le type choisi déclenche la logique StockService correspondante.
 */
public class MouvementExempleApp extends JFrame {

    private final JComboBox<Produit> cmbProduit = new JComboBox<>();
    private final JComboBox<String> cmbType = new JComboBox<>(new String[]{"ENTREE", "SORTIE"});
    private final JTextField txtQuantite = new JTextField(12);
    private final JTextField txtPrix = new JTextField(12);
    private final JTextField txtReference = new JTextField(18);
    private final JTextField txtNotes = new JTextField(24);
    private final JSpinner spDate = new JSpinner(new SpinnerDateModel());
    private final JLabel lblResultat = new JLabel("Prêt");
    private final DynamicTablePanel tablePanel = new DynamicTablePanel(MouvementStock.class);
    private final JButton btnAction = new JButton("Enregistrer le mouvement");
    private Integer selectedMovementId = null;

    public MouvementExempleApp() {
        super("Gestion des Mouvements de Stock");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1150, 760);
        setLocationRelativeTo(null);

        setContentPane(buildContent());
        configureInitialValues();
        wireEvents();
        refreshProducts();
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
        panel.add(new JLabel("Choisis un produit, sélectionne le type, puis enregistre. Le service applique FIFO/LIFO/CUMP côté base."), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildBody() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.add(buildFormPanel(), BorderLayout.NORTH);
        panel.add(new JScrollPane(tablePanel), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildFormPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Saisie du mouvement"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;
        addField(form, gbc, row++, "Produit", cmbProduit);
        addField(form, gbc, row++, "Type", cmbType);
        addField(form, gbc, row++, "Quantité", txtQuantite);
        addField(form, gbc, row++, "Prix unitaire", txtPrix);
        addField(form, gbc, row++, "Référence", txtReference);
        addField(form, gbc, row++, "Notes", txtNotes);

        spDate.setEditor(new JSpinner.DateEditor(spDate, "dd/MM/yyyy"));
        addField(form, gbc, row++, "Date", spDate);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JButton btnRafraichir = new JButton("Rafraîchir la table");
        JButton btnReset = new JButton("Réinitialiser");

        btnAction.addActionListener(e -> traiterMouvement());
        btnRafraichir.addActionListener(e -> refreshMouvementsList());
        btnReset.addActionListener(e -> resetForm());

        buttons.add(btnAction);
        buttons.add(btnRafraichir);
        buttons.add(btnReset);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        form.add(buttons, gbc);

        return form;
    }

    private void addField(JPanel form, GridBagConstraints gbc, int row, String label, java.awt.Component field) {
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = row;
        form.add(new JLabel(label + " :"), gbc);

        gbc.gridx = 1;
        form.add(field, gbc);
    }

    private JPanel buildFooter() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        panel.add(lblResultat, BorderLayout.WEST);
        return panel;
    }

    private void configureInitialValues() {
        spDate.setValue(java.sql.Date.valueOf(LocalDate.now()));
        txtPrix.setEnabled(true);
        cmbType.setSelectedItem("ENTREE");
    }

    private void wireEvents() {
        cmbType.addActionListener(e -> updateFieldState());
        tablePanel.getTable().getSelectionModel().addListSelectionListener(this::onMovementSelected);
    }

    private void onMovementSelected(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }

        MouvementStock selected = (MouvementStock) tablePanel.getSelectedEntity();
        if (selected == null) {
            return;
        }

        selectedMovementId = selected.getId();
        selectProduitById(selected.getIdProduit());
        cmbType.setSelectedItem(selected.getTypeMouvement());
        txtQuantite.setText(selected.getQuantite() != null ? selected.getQuantite().toPlainString() : "");
        txtPrix.setText(selected.getPrixUnitaire() != null ? selected.getPrixUnitaire().toPlainString() : "");
        txtReference.setText(firstNonBlank(selected.getReferenceAchat(), selected.getReferenceVente()));
        txtNotes.setText(selected.getNotes() != null ? selected.getNotes() : "");
        if (selected.getDateMouvement() != null) {
            spDate.setValue(java.sql.Date.valueOf(selected.getDateMouvement()));
        }

        setEditionMode(true);
        lblResultat.setText("Sélectionné: mouvement #" + selectedMovementId);
    }

    private void selectProduitById(Integer idProduit) {
        if (idProduit == null) {
            return;
        }
        for (int i = 0; i < cmbProduit.getItemCount(); i++) {
            Produit produit = cmbProduit.getItemAt(i);
            if (produit != null && idProduit.equals(produit.getId())) {
                cmbProduit.setSelectedIndex(i);
                return;
            }
        }
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return "";
    }

    private void setEditionMode(boolean enabled) {
        cmbProduit.setEnabled(!enabled);
        cmbType.setEnabled(!enabled);
        txtQuantite.setEnabled(!enabled);
        txtPrix.setEnabled(!enabled);
        btnAction.setText(enabled ? "Modifier la sélection" : "Enregistrer le mouvement");
    }

    private void updateFieldState() {
        boolean entree = "ENTREE".equals(cmbType.getSelectedItem());
        txtPrix.setEnabled(entree);
        if (!entree) {
            txtPrix.setText("0");
        }
    }

    private void refreshProducts() {
        try (Connection connection = DatabaseConfig.getConnection()) {
            ProduitDao produitDao = new ProduitDao(connection);
            List<Produit> produits = produitDao.findAll();
            cmbProduit.removeAllItems();
            for (Produit produit : produits) {
                cmbProduit.addItem(produit);
            }
            if (cmbProduit.getItemCount() > 0) {
                cmbProduit.setSelectedIndex(0);
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

    private void traiterMouvement() {
        if (selectedMovementId != null) {
            modifierMouvementSelectionne();
            return;
        }

        Produit produit = (Produit) cmbProduit.getSelectedItem();
        if (produit == null || produit.getId() == null) {
            JOptionPane.showMessageDialog(this, "Sélectionne un produit.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String type = (String) cmbType.getSelectedItem();
        BigDecimal quantite = lireBigDecimal(txtQuantite.getText(), "quantité");
        if (quantite == null) {
            return;
        }

        BigDecimal prix = BigDecimal.ZERO;
        if ("ENTREE".equals(type)) {
            prix = lireBigDecimal(txtPrix.getText(), "prix unitaire");
            if (prix == null) {
                return;
            }
        }

        String reference = txtReference.getText().trim();
        String notes = txtNotes.getText().trim();
        LocalDate date = ((java.util.Date) spDate.getValue()).toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate();

        try (Connection connection = DatabaseConfig.getConnection()) {
            connection.setAutoCommit(false);

            StockService service = new StockService(
                    new ProduitDao(connection),
                    new MouvementStockDao(connection),
                    new LigneStockDao(connection));

            if ("ENTREE".equals(type)) {
                service.entreeStock(produit.getId(), quantite, prix, reference, date);
                connection.commit();
                lblResultat.setText("Entrée enregistrée pour " + produit.getNom());
            } else {
                BigDecimal cout = service.sortieStock(produit.getId(), quantite, reference);
                connection.commit();
                lblResultat.setText("Sortie enregistrée, coût calculé = " + cout);
            }

            if (!notes.isBlank()) {
                try (Connection noteConnection = DatabaseConfig.getConnection()) {
                    noteConnection.setAutoCommit(false);
                    MouvementStockDao mouvementDao = new MouvementStockDao(noteConnection);
                    List<MouvementStock> mouvements = mouvementDao.findByProduitId(produit.getId());
                    if (!mouvements.isEmpty()) {
                        MouvementStock dernier = mouvements.get(0);
                        dernier.setNotes(notes);
                        mouvementDao.update(dernier);
                        noteConnection.commit();
                    }
                }
            }

            refreshProducts();
            refreshMouvementsList();
            resetForm();
        } catch (Exception exception) {
            lblResultat.setText("Erreur lors du traitement");
            JOptionPane.showMessageDialog(this,
                    "Impossible de traiter le mouvement :\n" + exception.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            exception.printStackTrace();
        }
    }

    private void modifierMouvementSelectionne() {
        String reference = txtReference.getText().trim();
        String notes = txtNotes.getText().trim();
        LocalDate date = ((java.util.Date) spDate.getValue()).toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate();

        try (Connection connection = DatabaseConfig.getConnection()) {
            MouvementStockDao mouvementDao = new MouvementStockDao(connection);
            MouvementStock mouvement = mouvementDao.findById(selectedMovementId);
            if (mouvement == null) {
                JOptionPane.showMessageDialog(this, "Mouvement introuvable.", "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }

            mouvement.setReferenceAchat(reference);
            mouvement.setReferenceVente(reference);
            mouvement.setNotes(notes);
            mouvement.setDateMouvement(date);
            mouvementDao.update(mouvement);

            lblResultat.setText("Mouvement modifié: #" + selectedMovementId);
            refreshMouvementsList();
            resetForm();
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this,
                    "Impossible de modifier le mouvement :\n" + exception.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            exception.printStackTrace();
        }
    }

    private BigDecimal lireBigDecimal(String text, String label) {
        try {
            String value = text.trim();
            if (value.isEmpty()) {
                throw new NumberFormatException("vide");
            }
            return new BigDecimal(value);
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this,
                    "La " + label + " est invalide.",
                    "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }
    }

    private void resetForm() {
        selectedMovementId = null;
        txtQuantite.setText("");
        txtPrix.setText("");
        txtReference.setText("");
        txtNotes.setText("");
        cmbType.setSelectedItem("ENTREE");
        updateFieldState();
        setEditionMode(false);
        spDate.setValue(java.sql.Date.valueOf(LocalDate.now()));
        if (cmbProduit.getItemCount() > 0) {
            cmbProduit.setSelectedIndex(0);
        }
        tablePanel.getTable().clearSelection();
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
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
