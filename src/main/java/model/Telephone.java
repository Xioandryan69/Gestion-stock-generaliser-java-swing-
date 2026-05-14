package model;

import annotation.Id;
import annotation.Table;

@Table(name = "telephone")
public class Telephone {

    @Id
    private int id;             // INT AUTO_INCREMENT

    private String numero;
    private String type;        // "mobile", "fixe", "fax"

    // --- constructeurs ---
    public Telephone() {}

    public Telephone(String numero, String type) {
        this.numero = numero;
        this.type = type;
    }
    public Telephone(int id, String numero, String type) {
        this.id = id;
        this.numero = numero;
        this.type = type;
    }

    // --- getters / setters ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    @Override
    public String toString() {
        return numero + " (" + type + ")";
    }
}
