package ui.exemple.gestion;

import java.math.BigDecimal;
import java.time.LocalDate;
import modele.Produit;

final class MouvementEntry {

    private final Produit produit;
    private final String type;
    private final String methode;
    private final BigDecimal quantite;
    private final BigDecimal prixUnitaire;
    private final String reference;
    private final String notes;
    private final LocalDate date;

    MouvementEntry(Produit produit, String type, String methode, BigDecimal quantite,
                   BigDecimal prixUnitaire, String reference, String notes, LocalDate date) {
        this.produit = produit;
        this.type = type;
        this.methode = methode;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
        this.reference = reference;
        this.notes = notes;
        this.date = date;
    }

    Produit getProduit() {
        return produit;
    }

    String getType() {
        return type;
    }

    String getMethode() {
        return methode;
    }

    BigDecimal getQuantite() {
        return quantite;
    }

    BigDecimal getPrixUnitaire() {
        return prixUnitaire;
    }

    String getReference() {
        return reference;
    }

    String getNotes() {
        return notes;
    }

    LocalDate getDate() {
        return date;
    }
}
