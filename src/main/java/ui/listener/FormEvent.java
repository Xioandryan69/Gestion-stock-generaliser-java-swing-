package ui.listener;

import java.util.Map;

/**
 * Événement déclenché lors d'une action de formulaire
 * Contient toutes les données nécessaires pour traiter l'action
 */
public class FormEvent {
    private final FormAction action;
    private final Class<?> entityClass;
    private final Map<String, Object> formData;
    private final Object sourceEntity; // Pour UPDATE/DELETE

    public FormEvent(FormAction action, Class<?> entityClass, Map<String, Object> formData) {
        this(action, entityClass, formData, null);
    }

    public FormEvent(FormAction action, Class<?> entityClass, 
                     Map<String, Object> formData, Object sourceEntity) {
        this.action = action;
        this.entityClass = entityClass;
        this.formData = formData;
        this.sourceEntity = sourceEntity;
    }

    public FormAction getAction() { return action; }
    public Class<?> getEntityClass() { return entityClass; }
    public Map<String, Object> getFormData() { return formData; }
    public Object getSourceEntity() { return sourceEntity; }
}