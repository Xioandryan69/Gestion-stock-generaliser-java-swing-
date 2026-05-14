package model;

import annotation.Id;
import annotation.Table;

@Table(name = "addresse")
public class Addresse {

    @Id
    private int id;             // INT AUTO_INCREMENT

    private String ville;
    private String codePostal;

    // --- constructeurs ---
    public Addresse() {}
    public Addresse(int id, String ville, String codePostal) {
        this.id = id;
        this.ville = ville;
        this.codePostal = codePostal;
    }

    public Addresse(String ville, String codePostal) {
        this.ville = ville;
        this.codePostal = codePostal;
    }

    // --- getters / setters ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public String getCodePostal() { return codePostal; }
    public void setCodePostal(String codePostal) { this.codePostal = codePostal; }

    @Override
    public String toString() {
        return ville + " (" + codePostal + ")";
    }
}