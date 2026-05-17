package modele;

import annotation.Column;
import annotation.Id;
import annotation.Table;

@Table(name = "produit")
public class Produit {
    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "nom")
    private String nom;

    @Column(name = "description")
    private String description;

    @Column(name = "methode_valorisation")
    private String methodeValorisation;

    @Column(name = "code_interne")
    private String codeInterne;

    @Column(name = "actif")
    private boolean actif;

    public Produit() {
        this.actif = true;
        this.methodeValorisation = "CUMP";
    }

    public Produit(String nom, String methodeValorisation) {
        this.nom = nom;
        this.methodeValorisation = methodeValorisation;
        this.actif = true;
    }

    public Produit(String nom, String description, String methodeValorisation, String codeInterne) {
        this.nom = nom;
        this.description = description;
        this.methodeValorisation = methodeValorisation;
        this.codeInterne = codeInterne;
        this.actif = true;
    }

    // Getters et Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMethodeValorisation() {
        return methodeValorisation;
    }

    public void setMethodeValorisation(String methodeValorisation) {
        this.methodeValorisation = methodeValorisation;
    }

    public String getCodeInterne() {
        return codeInterne;
    }

    public void setCodeInterne(String codeInterne) {
        this.codeInterne = codeInterne;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    @Override
    public String toString() {
        return nom != null ? nom : ("Produit#" + (id != null ? id : "?"));
    }
}
