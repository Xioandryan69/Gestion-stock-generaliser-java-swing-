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
    public BigDecimal sortieStock(int idProduit, BigDecimal quantite, String referenceVente, LocalDate date, String methodeOverride) throws Exception {
        Produit produit = produitDao.findById(idProduit);
        if (produit == null) {
            throw new Exception("Produit non trouvé");
        }

        BigDecimal coutSortie = BigDecimal.ZERO;

        String methode = (methodeOverride != null && !methodeOverride.isBlank()) ? methodeOverride : produit.getMethodeValorisation();
        if (methode == null) methode = "CUMP";
        switch (methode) {
            case "FIFO":
                coutSortie = sortieStockFIFO(idProduit, quantite, referenceVente, date);
                break;
            case "LIFO":
                coutSortie = sortieStockLIFO(idProduit, quantite, referenceVente, date);
                break;
            case "CUMP":
                coutSortie = sortieStockCUMP(idProduit, quantite, referenceVente, date);
                break;
            default:
                throw new Exception("Méthode de valorisation inconnue: " + produit.getMethodeValorisation());
        }

        // Enregistrer le mouvement de sortie
        MouvementStock mouvement = new MouvementStock(idProduit, "SORTIE", quantite, BigDecimal.ZERO);
        mouvement.setReferenceVente(referenceVente);
        mouvement.setDateMouvement(date != null ? date : LocalDate.now());
        mouvementDao.save(mouvement);

        return coutSortie;
    }

    /**
     * Estime le coût total d'une sortie sans modifier la base (aperçu).
     * Retourne la valeur totale (non le PU). Pour obtenir le PU, diviser par la quantité.
     */
    public BigDecimal estimateSortieCost(int idProduit, BigDecimal quantiteASortir, String methode) throws Exception {
        if (methode == null || methode.isBlank()) {
            Produit produit = produitDao.findById(idProduit);
            methode = produit != null ? produit.getMethodeValorisation() : "CUMP";
        }

        if ("CUMP".equalsIgnoreCase(methode)) {
            BigDecimal cump = calculerCUMP(idProduit);
            return cump.multiply(quantiteASortir);
        }

        List<LigneStock> lignes;
        if ("LIFO".equalsIgnoreCase(methode)) {
            lignes = ligneStockDao.findByProduitIdDescending(idProduit);
        } else {
            lignes = ligneStockDao.findByProduitIdWithRestant(idProduit);
        }

        BigDecimal reste = quantiteASortir;
        BigDecimal total = BigDecimal.ZERO;
        for (LigneStock ligne : lignes) {
            if (reste.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal prendre = ligne.getQuantiteRestante().min(reste);
            total = total.add(prendre.multiply(ligne.getPrixUnitaire()));
            reste = reste.subtract(prendre);
        }

        if (reste.compareTo(BigDecimal.ZERO) > 0) {
            throw new Exception("Stock insuffisant pour estimer la sortie. Disponible diff: " + reste);
        }

        return total;
    }

    /**
     * FIFO : First In First Out
     * Consomme les lots les plus anciens en premier
     */
    public BigDecimal sortieStockFIFO(int idProduit, BigDecimal quantiteASortir, String referenceVente, LocalDate date) throws Exception {
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
            ligne.setDateConsommation(date != null ? date : LocalDate.now());
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
    public BigDecimal sortieStockLIFO(int idProduit, BigDecimal quantiteASortir, String referenceVente, LocalDate date) throws Exception {
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
            ligne.setDateConsommation(date != null ? date : LocalDate.now());
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
    public BigDecimal sortieStockCUMP(int idProduit, BigDecimal quantiteASortir, String referenceVente, LocalDate date) throws Exception {
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
            ligne.setDateConsommation(date != null ? date : LocalDate.now());
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
     * Reconstitue l'état du stock à une date donnée en rejouant les mouvements
     * depuis le début jusqu'à la date (inclus) et en appliquant la méthode demandée.
     * Cette méthode n'altère pas la base de données.
     *
     * @param idProduit identifiant du produit
     * @param date date cible (inclus)
     * @param methodeOverride si non-null, force l'utilisation de cette méthode (FIFO/LIFO/CUMP), sinon utilise celle du produit
     */
    public EtatStock computeSnapshotAtDate(int idProduit, LocalDate date, String methodeOverride) throws Exception {
        Produit produit = produitDao.findById(idProduit);
        if (produit == null) throw new Exception("Produit non trouvé");

        String methode = (methodeOverride != null && !methodeOverride.isBlank()) ? methodeOverride : produit.getMethodeValorisation();
        if (methode == null) methode = "CUMP";

        // récupérer tous les mouvements jusqu'à la date (ascendant)
        List<MouvementStock> mouvements = mouvementDao.findByProduitIdUntilDate(idProduit, date);

        // structure locale pour lots
        class Lot {
            java.time.LocalDate dateEntree;
            BigDecimal quantite;
            BigDecimal prixUnitaire;

            Lot(java.time.LocalDate d, BigDecimal q, BigDecimal p) { dateEntree = d; quantite = q; prixUnitaire = p; }
        }

        java.util.LinkedList<Lot> lots = new java.util.LinkedList<>();

        for (MouvementStock m : mouvements) {
            if ("ENTREE".equalsIgnoreCase(m.getTypeMouvement())) {
                // créer un lot avec la quantité entrée
                BigDecimal q = m.getQuantite() != null ? m.getQuantite() : BigDecimal.ZERO;
                BigDecimal pu = m.getPrixUnitaire() != null ? m.getPrixUnitaire() : BigDecimal.ZERO;
                lots.add(new Lot(m.getDateMouvement(), q, pu));
            } else {
                // SORTIE -> consommer selon methode
                BigDecimal toConsume = m.getQuantite() != null ? m.getQuantite() : BigDecimal.ZERO;
                if (toConsume.compareTo(BigDecimal.ZERO) <= 0) continue;

                if ("CUMP".equalsIgnoreCase(methode)) {
                    // valeur et quantité totales disponibles
                    BigDecimal totalQty = BigDecimal.ZERO;
                    BigDecimal totalVal = BigDecimal.ZERO;
                    for (Lot l : lots) {
                        totalQty = totalQty.add(l.quantite);
                        totalVal = totalVal.add(l.quantite.multiply(l.prixUnitaire));
                    }
                    if (totalQty.compareTo(BigDecimal.ZERO) == 0) {
                        throw new Exception("Stock insuffisant pour sortie au CUMP");
                    }
                    BigDecimal cump = totalVal.divide(totalQty, 8, java.math.RoundingMode.HALF_UP);

                    // consommer FIFO mais utiliser le CUMP comme valeur
                    BigDecimal reste = toConsume;
                    java.util.Iterator<Lot> it = lots.iterator();
                    while (it.hasNext() && reste.compareTo(BigDecimal.ZERO) > 0) {
                        Lot l = it.next();
                        BigDecimal prendre = l.quantite.min(reste);
                        l.quantite = l.quantite.subtract(prendre);
                        reste = reste.subtract(prendre);
                        if (l.quantite.compareTo(BigDecimal.ZERO) == 0) it.remove();
                    }
                    if (reste.compareTo(BigDecimal.ZERO) > 0) {
                        throw new Exception("Stock insuffisant. Manque: " + reste);
                    }
                } else if ("LIFO".equalsIgnoreCase(methode)) {
                    BigDecimal reste = toConsume;
                    while (!lots.isEmpty() && reste.compareTo(BigDecimal.ZERO) > 0) {
                        Lot l = lots.getLast();
                        BigDecimal prendre = l.quantite.min(reste);
                        l.quantite = l.quantite.subtract(prendre);
                        reste = reste.subtract(prendre);
                        if (l.quantite.compareTo(BigDecimal.ZERO) == 0) lots.removeLast();
                    }
                    if (reste.compareTo(BigDecimal.ZERO) > 0) {
                        throw new Exception("Stock insuffisant. Manque: " + reste);
                    }
                } else { // FIFO par défaut
                    BigDecimal reste = toConsume;
                    while (!lots.isEmpty() && reste.compareTo(BigDecimal.ZERO) > 0) {
                        Lot l = lots.getFirst();
                        BigDecimal prendre = l.quantite.min(reste);
                        l.quantite = l.quantite.subtract(prendre);
                        reste = reste.subtract(prendre);
                        if (l.quantite.compareTo(BigDecimal.ZERO) == 0) lots.removeFirst();
                    }
                    if (reste.compareTo(BigDecimal.ZERO) > 0) {
                        throw new Exception("Stock insuffisant. Manque: " + reste);
                    }
                }
            }
        }

        // calculer quantite et valeur restantes
        BigDecimal quantiteRestante = BigDecimal.ZERO;
        BigDecimal valeurRestante = BigDecimal.ZERO;
        for (Lot l : lots) {
            quantiteRestante = quantiteRestante.add(l.quantite);
            valeurRestante = valeurRestante.add(l.quantite.multiply(l.prixUnitaire));
        }

        BigDecimal cumpFinal = quantiteRestante.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : valeurRestante.divide(quantiteRestante, 4, java.math.RoundingMode.HALF_UP);

        EtatStock result = new EtatStock();
        result.setIdProduit(idProduit);
        result.setDate(date);
        result.setQuantite(quantiteRestante);
        result.setValeurStock(valeurRestante);
        result.setCump(cumpFinal);
        return result;
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
