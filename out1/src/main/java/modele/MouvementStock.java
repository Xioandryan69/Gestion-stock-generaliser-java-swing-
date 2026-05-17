package modele;

import annotation.Column;
import annotation.Id;
import annotation.ManyToOne;
import annotation.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Table(name = "mouvement_stock")
public class MouvementStock {
    @Id
    @Column(name = "id")
    private int id;

    @ManyToOne
    @Column(name = "id_produit")
    private int idProduit;

    @Column(name = "type_mouvement")
    private String typeMouvement; // ENTREE, SORTIE

    @Column(name = "quantite")
    private BigDecimal quantite;

    @Column(name = "prix_unitaire")
    private BigDecimal prixUnitaire;

    @Column(name = "reference_achat")
    private String referenceAchat;

    @Column(name = "reference_vente")
    private String referenceVente;

    @Column(name = "date_mouvement")
    private LocalDate dateMouvement;

    @Column(name = "statut")
    private String statut;

    @Column(name = "notes")
    private String notes;

    public MouvementStock() {
        this.dateMouvement = LocalDate.now();
        this.statut = "VALIDÉ";
    }

    public MouvementStock(int idProduit, String typeMouvement, BigDecimal quantite, BigDecimal prixUnitaire) {
        this.idProduit = idProduit;
        this.typeMouvement = typeMouvement;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
        this.dateMouvement = LocalDate.now();
        this.statut = "VALIDÉ";
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

    public String getTypeMouvement() {
        return typeMouvement;
    }

    public void setTypeMouvement(String typeMouvement) {
        this.typeMouvement = typeMouvement;
    }

    public BigDecimal getQuantite() {
        return quantite;
    }

    public void setQuantite(BigDecimal quantite) {
        this.quantite = quantite;
    }

    public BigDecimal getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setPrixUnitaire(BigDecimal prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }

    public BigDecimal getValeurTotal() {
        if (quantite != null && prixUnitaire != null) {
            return quantite.multiply(prixUnitaire);
        }
        return BigDecimal.ZERO;
    }

    public String getReferenceAchat() {
        return referenceAchat;
    }

    public void setReferenceAchat(String referenceAchat) {
        this.referenceAchat = referenceAchat;
    }

    public String getReferenceVente() {
        return referenceVente;
    }

    public void setReferenceVente(String referenceVente) {
        this.referenceVente = referenceVente;
    }

    public LocalDate getDateMouvement() {
        return dateMouvement;
    }

    public void setDateMouvement(LocalDate dateMouvement) {
        this.dateMouvement = dateMouvement;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "MouvementStock{" +
                "id=" + id +
                ", idProduit=" + idProduit +
                ", typeMouvement='" + typeMouvement + '\'' +
                ", quantite=" + quantite +
                ", prixUnitaire=" + prixUnitaire +
                ", dateMouvement=" + dateMouvement +
                ", statut='" + statut + '\'' +
                '}';
    }
}
