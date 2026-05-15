package modele;

import annotation.Column;
import annotation.Id;
import annotation.ManyToOne;
import annotation.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Table(name = "ligne_stock")
public class LigneStock {
    @Id
    @Column(name = "id")
    private int id;

    @ManyToOne
    @Column(name = "id_produit")
    private int idProduit;

    @ManyToOne
    @Column(name = "id_mouvement_entree")
    private int idMouvementEntree;

    @Column(name = "quantite_initiale")
    private BigDecimal quantiteInitiale;

    @Column(name = "quantite_restante")
    private BigDecimal quantiteRestante;

    @Column(name = "prix_unitaire")
    private BigDecimal prixUnitaire;

    @Column(name = "date_entree")
    private LocalDate dateEntree;

    @Column(name = "date_consommation")
    private LocalDate dateConsommation;

    public LigneStock() {
        this.dateEntree = LocalDate.now();
    }

    public LigneStock(int idProduit, int idMouvementEntree, BigDecimal quantiteInitiale, 
                      BigDecimal prixUnitaire, LocalDate dateEntree) {
        this.idProduit = idProduit;
        this.idMouvementEntree = idMouvementEntree;
        this.quantiteInitiale = quantiteInitiale;
        this.quantiteRestante = quantiteInitiale;
        this.prixUnitaire = prixUnitaire;
        this.dateEntree = dateEntree;
    }

    // Getters et Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIdProduit() {
        return idProduit;
    }

    public void setIdProduit(int idProduit) {
        this.idProduit = idProduit;
    }

    public int getIdMouvementEntree() {
        return idMouvementEntree;
    }

    public void setIdMouvementEntree(int idMouvementEntree) {
        this.idMouvementEntree = idMouvementEntree;
    }

    public BigDecimal getQuantiteInitiale() {
        return quantiteInitiale;
    }

    public void setQuantiteInitiale(BigDecimal quantiteInitiale) {
        this.quantiteInitiale = quantiteInitiale;
    }

    public BigDecimal getQuantiteRestante() {
        return quantiteRestante;
    }

    public void setQuantiteRestante(BigDecimal quantiteRestante) {
        this.quantiteRestante = quantiteRestante;
    }

    public BigDecimal getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setPrixUnitaire(BigDecimal prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }

    public BigDecimal getValeurRestante() {
        if (quantiteRestante != null && prixUnitaire != null) {
            return quantiteRestante.multiply(prixUnitaire);
        }
        return BigDecimal.ZERO;
    }

    public LocalDate getDateEntree() {
        return dateEntree;
    }

    public void setDateEntree(LocalDate dateEntree) {
        this.dateEntree = dateEntree;
    }

    public LocalDate getDateConsommation() {
        return dateConsommation;
    }

    public void setDateConsommation(LocalDate dateConsommation) {
        this.dateConsommation = dateConsommation;
    }

    @Override
    public String toString() {
        return "LigneStock{" +
                "id=" + id +
                ", idProduit=" + idProduit +
                ", quantiteInitiale=" + quantiteInitiale +
                ", quantiteRestante=" + quantiteRestante +
                ", prixUnitaire=" + prixUnitaire +
                ", dateEntree=" + dateEntree +
                '}';
    }
}
