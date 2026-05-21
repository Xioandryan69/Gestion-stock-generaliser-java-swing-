package ui.exemple.gestion;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import modele.Produit;

final class MouvementRowPanel extends JPanel {

    private final JComboBox<Produit> cmbProduit = new JComboBox<>();
    private final JComboBox<String> cmbType = new JComboBox<>(new String[]{"ENTREE", "SORTIE"});
    private final JComboBox<String> cmbMethode = new JComboBox<>(new String[]{"PRODUIT", "FIFO", "LIFO", "CUMP"});
    private final JTextField txtQuantite = new JTextField(10);
    private final JTextField txtPrix = new JTextField(10);
    private final JTextField txtReference = new JTextField(14);
    private final JTextField txtNotes = new JTextField(18);
    private final JSpinner spDate = new JSpinner(new SpinnerDateModel());
    private final JButton btnCalculer = new JButton("Calculer PU");
    private final JButton btnSupprimer = new JButton("Supprimer");
    private final Consumer<MouvementRowPanel> deleteHandler;
    private final Consumer<MouvementRowPanel> computeHandler;

    MouvementRowPanel(Consumer<MouvementRowPanel> deleteHandler, Consumer<MouvementRowPanel> computeHandler) {
        this.deleteHandler = deleteHandler;
        this.computeHandler = computeHandler;

        setBorder(BorderFactory.createTitledBorder("Mouvement"));
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;
        addField(gbc, row++, "Produit", cmbProduit);
        addField(gbc, row++, "Type", cmbType);
        addField(gbc, row++, "Méthode", cmbMethode);
        addField(gbc, row++, "Quantité", txtQuantite);
        addField(gbc, row++, "Prix unitaire", txtPrix);
        addField(gbc, row++, "Référence", txtReference);
        addField(gbc, row++, "Notes", txtNotes);

        spDate.setEditor(new JSpinner.DateEditor(spDate, "dd/MM/yyyy"));
        spDate.setValue(MouvementFormUtils.toDate(LocalDate.now()));
        addField(gbc, row++, "Date", spDate);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnCalculer.addActionListener(e -> computeHandler.accept(this));
        btnSupprimer.addActionListener(e -> deleteHandler.accept(this));
        actions.add(btnCalculer);
        actions.add(btnSupprimer);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        add(actions, gbc);

        cmbType.addActionListener(e -> updateFieldState());
        cmbMethode.addActionListener(e -> {
            if ("SORTIE".equals(cmbType.getSelectedItem())) {
                computeHandler.accept(this);
            }
        });
        cmbProduit.addActionListener(e -> {
            if ("SORTIE".equals(cmbType.getSelectedItem())) {
                computeHandler.accept(this);
            }
        });

        cmbType.setSelectedItem("ENTREE");
        cmbMethode.setSelectedItem("PRODUIT");
        updateFieldState();
    }

    void applyProducts(List<Produit> produits) {
        Produit selected = (Produit) cmbProduit.getSelectedItem();
        cmbProduit.removeAllItems();
        for (Produit produit : produits) {
            cmbProduit.addItem(produit);
        }

        if (selected != null && selected.getId() != null) {
            for (int i = 0; i < cmbProduit.getItemCount(); i++) {
                Produit item = cmbProduit.getItemAt(i);
                if (item != null && selected.getId().equals(item.getId())) {
                    cmbProduit.setSelectedIndex(i);
                    return;
                }
            }
        }

        if (cmbProduit.getItemCount() > 0) {
            cmbProduit.setSelectedIndex(0);
        }
    }

    MouvementEntry readInput(Component parent) {
        Produit produit = (Produit) cmbProduit.getSelectedItem();
        if (produit == null || produit.getId() == null) {
            JOptionPane.showMessageDialog(parent,
                    "Sélectionne un produit pour chaque mouvement.",
                    "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }

        String type = getTypeSelection();
        BigDecimal quantite = MouvementFormUtils.parseBigDecimal(parent, txtQuantite.getText(), "quantité");
        if (quantite == null) {
            return null;
        }

        BigDecimal prix = BigDecimal.ZERO;
        if ("ENTREE".equals(type)) {
            prix = MouvementFormUtils.parseBigDecimal(parent, txtPrix.getText(), "prix unitaire");
            if (prix == null) {
                return null;
            }
        } else if (!txtPrix.getText().trim().isEmpty()) {
            prix = MouvementFormUtils.parseBigDecimal(parent, txtPrix.getText(), "prix unitaire");
            if (prix == null) {
                return null;
            }
        }

        String reference = txtReference.getText().trim();
        String notes = txtNotes.getText().trim();
        LocalDate date = MouvementFormUtils.toLocalDate((Date) spDate.getValue());

        String methode = getMethodeSelectionnee();
        if ("PRODUIT".equals(methode)) {
            methode = produit.getMethodeValorisation();
        }

        return new MouvementEntry(produit, type, methode, quantite, prix, reference, notes, date);
    }

    void resetRow() {
        txtQuantite.setText("");
        txtPrix.setText("");
        txtReference.setText("");
        txtNotes.setText("");
        cmbType.setSelectedItem("ENTREE");
        cmbMethode.setSelectedItem("PRODUIT");
        spDate.setValue(MouvementFormUtils.toDate(LocalDate.now()));
        updateFieldState();
        if (cmbProduit.getItemCount() > 0) {
            cmbProduit.setSelectedIndex(0);
        }
    }

    void setTitleText(String title) {
        setBorder(BorderFactory.createTitledBorder(title));
    }

    Produit getProduitSelectionne() {
        return (Produit) cmbProduit.getSelectedItem();
    }

    String getTypeSelection() {
        return (String) cmbType.getSelectedItem();
    }

    String getMethodeSelectionnee() {
        return (String) cmbMethode.getSelectedItem();
    }

    String getQuantiteText() {
        return txtQuantite.getText();
    }

    void setPrixText(String value) {
        txtPrix.setText(value);
    }

    void setPrixEditable(boolean editable) {
        txtPrix.setEditable(editable);
    }

    private void addField(GridBagConstraints gbc, int row, String label, Component field) {
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = row;
        add(new JLabel(label + " :"), gbc);

        gbc.gridx = 1;
        add(field, gbc);
    }

    private void updateFieldState() {
        boolean entree = "ENTREE".equals(cmbType.getSelectedItem());
        txtPrix.setEditable(entree);
        if (!entree && txtPrix.getText().trim().isEmpty()) {
            txtPrix.setText("0");
        }
    }
}
