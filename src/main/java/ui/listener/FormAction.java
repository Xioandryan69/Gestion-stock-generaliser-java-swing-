package ui.listener;

/**
 * Enum des actions possibles sur un formulaire
 * Utilisé pour généraliser les traitements des listeners
 */
public enum FormAction {
    SAVE("Sauvegarder"),
    UPDATE("Mettre à jour"),
    DELETE("Supprimer"),
    RESET("Réinitialiser"),
    CANCEL("Annuler"),
    CUSTOM("Action personnalisée");

    private final String label;

    FormAction(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}