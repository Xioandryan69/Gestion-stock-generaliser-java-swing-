package model;

import annotation.Column;
import annotation.Enumerated;
import annotation.Enumerated.EnumType;
import annotation.Id;
import annotation.IgnoredField;
import annotation.ManyToOne;
import annotation.OneToMany;
import annotation.Table;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



@Table(name = "personne")
public class Personne {

    @Id
    private String id;

    private String nom;
    private int age;

    @Enumerated(EnumType.STRING)
    private Role role;   // ADMINISTRATEUR, CLIENT, INVITE

    @ManyToOne(joinColumn = "adresse_id")
    //@IgnoredField  // si tu veux l’ignorer dans le formulaire auto-généré
    private Addresse adresse;

    //@IgnoredField
    @OneToMany(joinTable = "personne_telephone",
               joinColumn = "personne_id",
               inverseJoinColumn = "telephone_id")
    private List<Telephone> telephones;

    @IgnoredField
    private Map<String, Object> infosSupplementaires; // déjà géré en JSON

    //@IgnoredField
    @Column(name = "infosSupplementaires")
    private String infosSupplementairesJson;

    @IgnoredField
    private String champInterne; // ne sera pas persisté ni affiché
    /*
        private Role role;                       // ENUM
    @IgnoredField
    private Addresse adresse;                // OBJET (classe créée)
    @IgnoredField
    private List<Telephone> telephones;      // LIST<classe créée>
    @IgnoredField
    private Map<String, Object> infosSupplementaires; // MAP

    @IgnoredField
    private String champInterne;             // ignoré par le framework
     */


    // --- constructeurs ---
    public Personne() {}

    public Personne(String id, String nom, int age) {
        this.id = id;
        this.nom = nom;
        this.age = age;
    }
    public Personne(String id, String nom, int age, Role role, Addresse adresse) {
        this.id = id;
        this.nom = nom;
        this.age = age;
        this.role = role;
        this.adresse = adresse;
    }

    // --- getters / setters (tous) ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Addresse getAdresse() { return adresse; }
    public void setAdresse(Addresse adresse) { this.adresse = adresse; }

    public List<Telephone> getTelephones() { return telephones; }
    public void setTelephones(List<Telephone> telephones) { this.telephones = telephones; }

    public Map<String, Object> getInfosSupplementaires() { return infosSupplementaires; }
    public void setInfosSupplementaires(Map<String, Object> infosSupplementaires) {
        this.infosSupplementaires = infosSupplementaires;
    }

    public String getInfosSupplementairesJson() {
        return infosSupplementairesJson;
    }

    public void setInfosSupplementairesJson(String infosSupplementairesJson) {
        this.infosSupplementairesJson = infosSupplementairesJson;
    }

    public String getChampInterne() { return champInterne; }
    public void setChampInterne(String champInterne) { this.champInterne = champInterne; }

    @Override
    public String toString() {
        return "Personne{id='" + id + "', nom='" + nom + "', age=" + age
                + ", role=" + role + ", adresse=" + adresse + "}";
    }



    // Avant sauvegarde, on sérialise la Map en JSON
    public void prepareForDb() {
        if (infosSupplementaires == null || infosSupplementaires.isEmpty()) {
            infosSupplementairesJson = null;
            return;
        }

        this.infosSupplementairesJson = toJson(infosSupplementaires);
    }

    // Après chargement, on désérialise le JSON vers la Map
    public void loadFromDb() {
        if (infosSupplementairesJson == null || infosSupplementairesJson.isEmpty()) {
            this.infosSupplementaires = new HashMap<>();
            return;
        }

        this.infosSupplementaires = fromJson(infosSupplementairesJson);
    }

    private static String toJson(Map<String, Object> values) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (!first) {
                json.append(',');
            }
            first = false;
            json.append('"').append(escapeJson(entry.getKey())).append('"').append(':');
            Object value = entry.getValue();
            if (value == null) {
                json.append("null");
            } else if (value instanceof Number || value instanceof Boolean) {
                json.append(value);
            } else {
                json.append('"').append(escapeJson(value.toString())).append('"');
            }
        }
        json.append('}');
        return json.toString();
    }

    private static Map<String, Object> fromJson(String json) {
        Map<String, Object> values = new HashMap<>();
        String content = json.trim();
        if (content.startsWith("{")) {
            content = content.substring(1);
        }
        if (content.endsWith("}")) {
            content = content.substring(0, content.length() - 1);
        }
        if (content.isBlank()) {
            return values;
        }

        for (String entry : content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")) {
            String[] parts = entry.split(":", 2);
            if (parts.length != 2) {
                continue;
            }
            String key = unquote(parts[0].trim());
            String rawValue = parts[1].trim();
            values.put(key, parseJsonValue(rawValue));
        }
        return values;
    }

    private static Object parseJsonValue(String rawValue) {
        if (rawValue.equals("null")) {
            return null;
        }
        if (rawValue.equals("true") || rawValue.equals("false")) {
            return Boolean.valueOf(rawValue);
        }
        if (rawValue.startsWith("\"") && rawValue.endsWith("\"")) {
            return unquote(rawValue);
        }
        try {
            if (rawValue.contains(".")) {
                return Double.valueOf(rawValue);
            }
            return Integer.valueOf(rawValue);
        } catch (NumberFormatException ex) {
            return rawValue;
        }
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String unquote(String value) {
        String trimmed = value.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed.replace("\\\"", "\"").replace("\\\\", "\\");
    }

}