package gestion.service;

import gestion.dao.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import modele.*;

/**
 * Service métier pour la gestion du stock.
 * Gère les entrées, sorties et calculs de valorisation (FIFO, LIFO, CUMP).
 */
public class StockService {
    private ProduitDao produitDao;
    private MouvementStockDao mouvementDao;
    private LigneStockDao ligneStockDao;

    public StockService(ProduitDao produitDao, MouvementStockDao mouvementDao, LigneStockDao ligneStockDao) {
        this.produitDao = produitDao;
        this.mouvementDao = mouvementDao;
        this.ligneStockDao = ligneStockDao;
    }

    /**
     * Enregistrer une entrée de stock
     */
    public void entreeStock(int idProduit, BigDecimal quantite, BigDecimal prixUnitaire, 
                            String referenceAchat, LocalDate dateEntree) throws Exception {
        // Créer le mouvement d'entrée
        MouvementStock mouvement = new MouvementStock(idProduit, "ENTREE", quantite, prixUnitaire);
        mouvement.setReferenceAchat(referenceAchat);
        mouvement.setDateMouvement(dateEntree);
        mouvementDao.save(mouvement);

        // Créer la ligne de stock pour tracer le lot
        LigneStock ligne = new LigneStock(idProduit, mouvement.getId(), quantite, prixUnitaire, dateEntree);
        ligneStockDao.save(ligne);
    }

    /**
     * Sortie de stock avec application de la méthode configurée
     */
    public BigDecimal sortieStock(int idProduit, BigDecimal quantite, String referenceVente) throws Exception {
        Produit produit = produitDao.findById(idProduit);
        if (produit == null) {
            throw new Exception("Produit non trouvé");
        }

        BigDecimal coutSortie = BigDecimal.ZERO;

        switch (produit.getMethodeValorisation()) {
            case "FIFO":
                coutSortie = sortieStockFIFO(idProduit, quantite, referenceVente);
                break;
            case "LIFO":
                coutSortie = sortieStockLIFO(idProduit, quantite, referenceVente);
                break;
            case "CUMP":
                coutSortie = sortieStockCUMP(idProduit, quantite, referenceVente);
                break;
            default:
                throw new Exception("Méthode de valorisation inconnue: " + produit.getMethodeValorisation());
        }

        // Enregistrer le mouvement de sortie
        MouvementStock mouvement = new MouvementStock(idProduit, "SORTIE", quantite, BigDecimal.ZERO);
        mouvement.setReferenceVente(referenceVente);
        mouvement.setDateMouvement(LocalDate.now());
        mouvementDao.save(mouvement);

        return coutSortie;
    }

    /**
     * FIFO : First In First Out
     * Consomme les lots les plus anciens en premier
     */
    public BigDecimal sortieStockFIFO(int idProduit, BigDecimal quantiteASortir, String referenceVente) throws Exception {
        List<LigneStock> lignes = ligneStockDao.findByProduitIdWithRestant(idProduit);
        BigDecimal quantiteConsommee = BigDecimal.ZERO;
        BigDecimal coutTotal = BigDecimal.ZERO;

        for (LigneStock ligne : lignes) {
            if (quantiteConsommee.compareTo(quantiteASortir) >= 0) {
                break;
            }

            BigDecimal aConsommer = quantiteASortir.subtract(quantiteConsommee);
            if (ligne.getQuantiteRestante().compareTo(aConsommer) <= 0) {
                // Consommer toute la ligne
                aConsommer = ligne.getQuantiteRestante();
            }

            coutTotal = coutTotal.add(aConsommer.multiply(ligne.getPrixUnitaire()));
            ligne.setQuantiteRestante(ligne.getQuantiteRestante().subtract(aConsommer));
            ligne.setDateConsommation(LocalDate.now());
            ligneStockDao.save(ligne);

            quantiteConsommee = quantiteConsommee.add(aConsommer);
        }

        if (quantiteConsommee.compareTo(quantiteASortir) < 0) {
            throw new Exception("Stock insuffisant. Demandé: " + quantiteASortir + ", Disponible: " + quantiteConsommee);
        }

        return coutTotal;
    }

    /**
     * LIFO : Last In First Out
     * Consomme les lots les plus récents en premier
     */
    public BigDecimal sortieStockLIFO(int idProduit, BigDecimal quantiteASortir, String referenceVente) throws Exception {
        List<LigneStock> lignes = ligneStockDao.findByProduitIdDescending(idProduit);
        BigDecimal quantiteConsommee = BigDecimal.ZERO;
        BigDecimal coutTotal = BigDecimal.ZERO;

        for (LigneStock ligne : lignes) {
            if (quantiteConsommee.compareTo(quantiteASortir) >= 0) {
                break;
            }

            BigDecimal aConsommer = quantiteASortir.subtract(quantiteConsommee);
            if (ligne.getQuantiteRestante().compareTo(aConsommer) <= 0) {
                // Consommer toute la ligne
                aConsommer = ligne.getQuantiteRestante();
            }

            coutTotal = coutTotal.add(aConsommer.multiply(ligne.getPrixUnitaire()));
            ligne.setQuantiteRestante(ligne.getQuantiteRestante().subtract(aConsommer));
            ligne.setDateConsommation(LocalDate.now());
            ligneStockDao.save(ligne);

            quantiteConsommee = quantiteConsommee.add(aConsommer);
        }

        if (quantiteConsommee.compareTo(quantiteASortir) < 0) {
            throw new Exception("Stock insuffisant. Demandé: " + quantiteASortir + ", Disponible: " + quantiteConsommee);
        }

        return coutTotal;
    }

    /**
     * CUMP : Coût Unitaire Moyen Pondéré
     * Utilise le CUMP actuel du produit
     */
    public BigDecimal sortieStockCUMP(int idProduit, BigDecimal quantiteASortir, String referenceVente) throws Exception {
        BigDecimal cump = calculerCUMP(idProduit);
        
        // Réduire le stock disponible
        List<LigneStock> lignes = ligneStockDao.findByProduitIdWithRestant(idProduit);
        BigDecimal quantiteRestante = BigDecimal.ZERO;
        for (LigneStock ligne : lignes) {
            quantiteRestante = quantiteRestante.add(ligne.getQuantiteRestante());
        }

        if (quantiteRestante.compareTo(quantiteASortir) < 0) {
            throw new Exception("Stock insuffisant. Demandé: " + quantiteASortir + ", Disponible: " + quantiteRestante);
        }

        // Consommer au CUMP actuel
        BigDecimal coutTotal = quantiteASortir.multiply(cump);

        // Répartir la consommation sur les lots (FIFO)
        BigDecimal aConsommer = quantiteASortir;
        for (LigneStock ligne : lignes) {
            if (aConsommer.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal consommer = aConsommer;
            if (ligne.getQuantiteRestante().compareTo(aConsommer) < 0) {
                consommer = ligne.getQuantiteRestante();
            }

            ligne.setQuantiteRestante(ligne.getQuantiteRestante().subtract(consommer));
            ligne.setDateConsommation(LocalDate.now());
            ligneStockDao.save(ligne);

            aConsommer = aConsommer.subtract(consommer);
        }

        return coutTotal;
    }

    /**
     * Calculer le CUMP actuel d'un produit
     * CUMP = Valeur Stock Total / Quantité Stock Total
     */
    public BigDecimal calculerCUMP(int idProduit) throws Exception {
        List<LigneStock> lignes = ligneStockDao.findByProduitIdWithRestant(idProduit);
        
        BigDecimal valeurTotal = BigDecimal.ZERO;
        BigDecimal quantiteTotal = BigDecimal.ZERO;

        for (LigneStock ligne : lignes) {
            valeurTotal = valeurTotal.add(ligne.getValeurRestante());
            quantiteTotal = quantiteTotal.add(ligne.getQuantiteRestante());
        }

        if (quantiteTotal.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return valeurTotal.divide(quantiteTotal, 4, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * Obtenir l'état du stock à une date donnée
     */
    public EtatStock getEtatStock(int idProduit, LocalDate date) throws Exception {
        EtatStock etat = new EtatStock();
        etat.setIdProduit(idProduit);
        etat.setDate(date);

        List<LigneStock> lignes = ligneStockDao.findByProduitIdWithRestant(idProduit);
        BigDecimal quantiteTotal = BigDecimal.ZERO;
        BigDecimal valeurTotal = BigDecimal.ZERO;

        for (LigneStock ligne : lignes) {
            if (ligne.getDateEntree().compareTo(date) <= 0) {
                quantiteTotal = quantiteTotal.add(ligne.getQuantiteRestante());
                valeurTotal = valeurTotal.add(ligne.getValeurRestante());
            }
        }

        etat.setQuantite(quantiteTotal);
        etat.setValeurStock(valeurTotal);
        etat.setCump(calculerCUMP(idProduit));

        return etat;
    }

    /**
     * Classe interne pour représenter l'état du stock
     */
    public static class EtatStock {
        private int idProduit;
        private LocalDate date;
        private BigDecimal quantite;
        private BigDecimal valeurStock;
        private BigDecimal cump;

        public int getIdProduit() { return idProduit; }
        public void setIdProduit(int idProduit) { this.idProduit = idProduit; }

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }

        public BigDecimal getQuantite() { return quantite; }
        public void setQuantite(BigDecimal quantite) { this.quantite = quantite; }

        public BigDecimal getValeurStock() { return valeurStock; }
        public void setValeurStock(BigDecimal valeurStock) { this.valeurStock = valeurStock; }

        public BigDecimal getCump() { return cump; }
        public void setCump(BigDecimal cump) { this.cump = cump; }

        @Override
        public String toString() {
            return "EtatStock{" +
                    "quantite=" + quantite +
                    ", valeurStock=" + valeurStock +
                    ", cump=" + cump +
                    '}';
        }
    }
}
