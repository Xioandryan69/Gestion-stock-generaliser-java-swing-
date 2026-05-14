package ui.listener;

import crud.CrudRepository;

public class CrudFormListener implements FormListener {
    
    private final CrudRepository<?> crudRepository;
    private FormSuccessCallback successCallback;
    private FormErrorCallback errorCallback;

    public CrudFormListener(CrudRepository<?> crudRepository) {
        this.crudRepository = crudRepository;
    }

    @Override
    public void onFormAction(FormEvent event) {
        try {
            @SuppressWarnings("unchecked")
            CrudRepository<Object> repo = (CrudRepository<Object>) crudRepository;

            switch (event.getAction()) {
                case SAVE -> handleSave(event, repo);
                case UPDATE -> handleUpdate(event, repo);
                case DELETE -> handleDelete(event, repo);
                default -> {
                }
            }
        } catch (Exception e) {
            onFormError(event, e);
        }
    }

    private void handleSave(FormEvent event, CrudRepository<Object> repo) throws Exception {
        Object entity = createEntityFromFormData(event);
        repo.save(entity);
        if (successCallback != null) {
            successCallback.onSuccess(event, entity);
        }
    }

    private void handleUpdate(FormEvent event, CrudRepository<Object> repo) throws Exception {
        Object entity = event.getSourceEntity();
        if (entity == null) {
            throw new IllegalStateException("Pas d'entité source pour UPDATE");
        }
        applyFormDataToEntity(event, entity);
        repo.update(entity);
        if (successCallback != null) {
            successCallback.onSuccess(event, entity);
        }
    }

    private void handleDelete(FormEvent event, CrudRepository<Object> repo) throws Exception {
        Object entity = event.getSourceEntity();
        if (entity == null) {
            throw new IllegalStateException("Pas d'entité source pour DELETE");
        }
        repo.delete(entity);
        if (successCallback != null) {
            successCallback.onSuccess(event, entity);
        }
    }

    private Object createEntityFromFormData(FormEvent event) throws Exception {
        Object entity = event.getEntityClass().getDeclaredConstructor().newInstance();
        applyFormDataToEntity(event, entity);
        return entity;
    }

    private void applyFormDataToEntity(FormEvent event, Object entity) throws Exception {
        for (var entry : event.getFormData().entrySet()) {
            String fieldName = entry.getKey();
            Object fieldValue = entry.getValue();
            try {
                java.lang.reflect.Field field = 
                    event.getEntityClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(entity, fieldValue);
            } catch (NoSuchFieldException e) {
                // Champ ignoré
            }
        }
    }

    @Override
    public void onFormError(FormEvent event, Exception exception) {
        System.err.println("Erreur lors de l'action " + event.getAction() + " : " 
                          + exception.getMessage());
        exception.printStackTrace();
        if (errorCallback != null) {
            errorCallback.onError(event, exception);
        }
    }

    public void setSuccessCallback(FormSuccessCallback callback) {
        this.successCallback = callback;
    }

    public void setErrorCallback(FormErrorCallback callback) {
        this.errorCallback = callback;
    }

    public interface FormSuccessCallback {
        void onSuccess(FormEvent event, Object entity);
    }

    public interface FormErrorCallback {
        void onError(FormEvent event, Exception exception);
    }
}