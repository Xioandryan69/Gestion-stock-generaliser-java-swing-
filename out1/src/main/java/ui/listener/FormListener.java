package ui.listener;

/**
 * Interface générique pour écouter les événements de formulaire
 * Permet de gérer SAVE, UPDATE, DELETE et autres actions de manière uniforme
 */
public interface FormListener {
    
    /**
     * Appelé quand une action de formulaire est déclenchée
     * @param event L'événement contenant l'action, les données et la classe d'entité
     */
    void onFormAction(FormEvent event);
    
    /**
     * Appelé en cas d'erreur lors du traitement de l'action
     * @param event L'événement qui a causé l'erreur
     * @param exception L'exception levée
     */
    default void onFormError(FormEvent event, Exception exception) {
        exception.printStackTrace();
    }
    
    /**
     * Appelé après un traitement réussi de l'action
     * @param event L'événement traité avec succès
     */
    default void onFormSuccess(FormEvent event) {
        System.out.println("Action " + event.getAction() + " complétée avec succès");
    }
}