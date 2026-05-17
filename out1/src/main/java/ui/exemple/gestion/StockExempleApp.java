package ui.exemple.gestion;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

/**
 * Application principale de gestion de stock.
 * Exemple complet intégrant produits, mouvements et état du stock.
 */
public class StockExempleApp extends JFrame {
    private JTabbedPane tabbedPane;

    public StockExempleApp() {
        setTitle("Application de Gestion de Stock - FIFO/LIFO/CUMP");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        initComponents();
    }

    private void initComponents() {
        tabbedPane = new JTabbedPane();

        // Onglet Produits dynamique
        try {
            ProduitExempleApp produitPanel = new ProduitExempleApp();
            tabbedPane.addTab("Gestion Produits", produitPanel.getContentPane());
        } catch (Exception exception) {
            JPanel errorPanel = new JPanel(new BorderLayout());
            errorPanel.add(new JLabel("Impossible de charger l'exemple produit : " + exception.getMessage()), BorderLayout.CENTER);
            tabbedPane.addTab("Gestion Produits", errorPanel);
        }

        // Onglet Mouvements
        try {
            MouvementExempleApp mouvementPanel = new MouvementExempleApp();
            tabbedPane.addTab("Mouvements de Stock", mouvementPanel.getContentPane());
        } catch (Exception exception) {
            JPanel errorPanel = new JPanel(new BorderLayout());
            errorPanel.add(new JLabel("Impossible de charger l'exemple mouvement : " + exception.getMessage()), BorderLayout.CENTER);
            tabbedPane.addTab("Mouvements de Stock", errorPanel);
        }

        // Onglet État du Stock
        try {
            EtatStockExempleApp etatPanel = new EtatStockExempleApp();
            tabbedPane.addTab("État du Stock", etatPanel.getContentPane());
        } catch (Exception exception) {
            JPanel errorPanel = new JPanel(new BorderLayout());
            errorPanel.add(new JLabel("Impossible de charger l'exemple état stock : " + exception.getMessage()), BorderLayout.CENTER);
            tabbedPane.addTab("État du Stock", errorPanel);
        }

        // Onglet Rapports
        JPanel reportPanel = createReportPanel();
        tabbedPane.addTab("Rapports", reportPanel);

        // Onglet métier FIFO / LIFO / CUMP
        JPanel valorisationPanel = createValorisationPanel();
        tabbedPane.addTab("Valorisation", valorisationPanel);

        add(tabbedPane);
    }

    private JPanel createValorisationPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel header = new JPanel(new BorderLayout(5, 5));
        JLabel title = new JLabel("Fenêtrage métier FIFO / LIFO / CUMP");
        title.setFont(new Font("Arial", Font.BOLD, 16));
        header.add(title, BorderLayout.NORTH);
        header.add(new JLabel("Ouvre la fenêtre qui reprend les tableaux du document métier : mouvements, sorties, FIFO, LIFO et CUMP."), BorderLayout.SOUTH);
        panel.add(header, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new GridLayout(2, 2, 10, 10));
        buttons.setBorder(BorderFactory.createTitledBorder("Fenêtres"));

        JButton btnValorisation = new JButton("Ouvrir la valorisation");
        btnValorisation.addActionListener(e -> {
            try {
                new ValorisationStockExempleApp().setVisible(true);
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(this,
                        "Impossible d'ouvrir la fenêtre de valorisation : " + exception.getMessage(),
                        "Erreur",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        buttons.add(btnValorisation);

        JButton btnProduits = new JButton("Gestion produits");
        btnProduits.addActionListener(e -> ouvrirFenetreProduit());
        buttons.add(btnProduits);

        JButton btnMouvements = new JButton("Gestion mouvements");
        btnMouvements.addActionListener(e -> ouvrirFenetreMouvement());
        buttons.add(btnMouvements);

        JButton btnEtat = new JButton("État du stock");
        btnEtat.addActionListener(e -> ouvrirFenetreEtatStock());
        buttons.add(btnEtat);

        panel.add(buttons, BorderLayout.CENTER);
        return panel;
    }

    private void ouvrirFenetreProduit() {
        try {
            new ProduitExempleApp().setVisible(true);
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this,
                    "Impossible d'ouvrir la fenêtre produit : " + exception.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void ouvrirFenetreMouvement() {
        try {
            new MouvementExempleApp().setVisible(true);
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this,
                    "Impossible d'ouvrir la fenêtre mouvement : " + exception.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void ouvrirFenetreEtatStock() {
        try {
            new EtatStockExempleApp().setVisible(true);
        } catch (Exception exception) {
            JOptionPane.showMessageDialog(this,
                    "Impossible d'ouvrir la fenêtre état stock : " + exception.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createReportPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel titlePanel = new JPanel();
        JLabel titleLabel = new JLabel("Rapports de Gestion de Stock");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titlePanel.add(titleLabel);
        panel.add(titlePanel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        buttonPanel.setBorder(BorderFactory.createTitledBorder("Rapports Disponibles"));

        JButton btnHistorique = new JButton("Historique des Mouvements");
        btnHistorique.addActionListener(e -> afficherHistorique());
        buttonPanel.add(btnHistorique);

        JButton btnValorisation = new JButton("Valorisation du Stock");
        btnValorisation.addActionListener(e -> afficherValorisation());
        buttonPanel.add(btnValorisation);

        JButton btnComparaison = new JButton("Comparaison FIFO/LIFO/CUMP");
        btnComparaison.addActionListener(e -> afficherComparaison());
        buttonPanel.add(btnComparaison);

        JButton btnBenefice = new JButton("Calcul du Bénéfice");
        btnBenefice.addActionListener(e -> afficherBenefice());
        buttonPanel.add(btnBenefice);

        JButton btnExportPDF = new JButton("Exporter PDF");
        btnExportPDF.addActionListener(e -> afficherExport());
        buttonPanel.add(btnExportPDF);

        JButton btnExportExcel = new JButton("Exporter Excel");
        btnExportExcel.addActionListener(e -> afficherExportExcel());
        buttonPanel.add(btnExportExcel);

        panel.add(buttonPanel, BorderLayout.CENTER);

        return panel;
    }

    private void afficherHistorique() {
        JOptionPane.showMessageDialog(this, 
            "Rapport: Historique des mouvements\n" +
            "Période: Tous les mouvements\n" +
            "Format: PDF / Excel",
            "Historique", JOptionPane.INFORMATION_MESSAGE);
    }

    private void afficherValorisation() {
        JOptionPane.showMessageDialog(this, 
            "Rapport: Valorisation du Stock\n" +
            "- Quantité totale par produit\n" +
            "- Valeur stock par produit\n" +
            "- Valeur stock total",
            "Valorisation", JOptionPane.INFORMATION_MESSAGE);
    }

    private void afficherComparaison() {
        JOptionPane.showMessageDialog(this, 
            "Rapport: Comparaison des méthodes\n" +
            "Montre le coût de sortie différent selon:\n" +
            "- FIFO: First In First Out\n" +
            "- LIFO: Last In First Out\n" +
            "- CUMP: Coût Unitaire Moyen Pondéré",
            "Comparaison", JOptionPane.INFORMATION_MESSAGE);
    }

    private void afficherBenefice() {
        JOptionPane.showMessageDialog(this, 
            "Rapport: Calcul du Bénéfice\n" +
            "Bénéfice = Vente - Coût Réel\n" +
            "Montre l'impact du choix de valorisation\n" +
            "sur le résultat comptable",
            "Bénéfice", JOptionPane.INFORMATION_MESSAGE);
    }

    private void afficherExport() {
        JOptionPane.showMessageDialog(this, 
            "Export PDF en cours...\n" +
            "Fichier: rapport_stock_" + LocalDate.now() + ".pdf",
            "Export PDF", JOptionPane.INFORMATION_MESSAGE);
    }

    private void afficherExportExcel() {
        JOptionPane.showMessageDialog(this, 
            "Export Excel en cours...\n" +
            "Fichier: rapport_stock_" + LocalDate.now() + ".xlsx",
            "Export Excel", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            StockExempleApp frame = new StockExempleApp();
            frame.setVisible(true);
        });
    }
}
