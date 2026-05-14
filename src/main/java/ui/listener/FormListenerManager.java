package ui.listener;

import java.util.*;

/**
 * Gestionnaire centralisé pour les listeners de formulaire
 * Permet d'enregistrer, de déclencher et de gérer les listeners de manière uniforme
 */
public class FormListenerManager {
    
    private final List<FormListener> listeners = new ArrayList<>();
    private final Map<FormAction, List<FormListener>> actionListeners = new EnumMap<>(FormAction.class);
    private boolean notifyingListeners = false;

    /**
     * Ajoute un listener qui écoute TOUS les événements de formulaire
     */
    public void addFormListener(FormListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Ajoute un listener pour une action spécifique
     */
    public void addFormListener(FormAction action, FormListener listener) {
        if (listener != null) {
            actionListeners.computeIfAbsent(action, k -> new ArrayList<>())
                           .add(listener);
        }
    }

    /**
     * Retire un listener global
     */
    public void removeFormListener(FormListener listener) {
        listeners.remove(listener);
    }

    /**
     * Retire un listener d'une action spécifique
     */
    public void removeFormListener(FormAction action, FormListener listener) {
        List<FormListener> actionList = actionListeners.get(action);
        if (actionList != null) {
            actionList.remove(listener);
        }
    }

    /**
     * Retire tous les listeners
     */
    public void clearListeners() {
        listeners.clear();
        actionListeners.clear();
    }

    /**
     * Déclenche un événement de formulaire
     * Notifie d'abord les listeners globaux, puis les listeners de l'action spécifique
     */
    public void fireFormEvent(FormEvent event) {
        if (notifyingListeners) return; // Éviter les appels récursifs
        
        notifyingListeners = true;
        try {
            // Notifier les listeners globaux
            for (FormListener listener : new ArrayList<>(listeners)) {
                try {
                    listener.onFormAction(event);
                } catch (Exception e) {
                    listener.onFormError(event, e);
                }
            }

            // Notifier les listeners de l'action spécifique
            List<FormListener> actionList = actionListeners.get(event.getAction());
            if (actionList != null) {
                for (FormListener listener : new ArrayList<>(actionList)) {
                    try {
                        listener.onFormAction(event);
                    } catch (Exception e) {
                        listener.onFormError(event, e);
                    }
                }
            }
        } finally {
            notifyingListeners = false;
        }
    }

    /**
     * Notifie les listeners d'une réussite
     */
    public void fireFormSuccess(FormEvent event) {
        for (FormListener listener : new ArrayList<>(listeners)) {
            listener.onFormSuccess(event);
        }
        
        List<FormListener> actionList = actionListeners.get(event.getAction());
        if (actionList != null) {
            for (FormListener listener : new ArrayList<>(actionList)) {
                listener.onFormSuccess(event);
            }
        }
    }

    /**
     * Notifie les listeners d'une erreur
     */
    public void fireFormError(FormEvent event, Exception exception) {
        for (FormListener listener : new ArrayList<>(listeners)) {
            listener.onFormError(event, exception);
        }
        
        List<FormListener> actionList = actionListeners.get(event.getAction());
        if (actionList != null) {
            for (FormListener listener : new ArrayList<>(actionList)) {
                listener.onFormError(event, exception);
            }
        }
    }

    /**
     * Retourne le nombre de listeners globaux
     */
    public int getListenerCount() {
        return listeners.size();
    }

    /**
     * Retourne le nombre de listeners pour une action spécifique
     */
    public int getListenerCount(FormAction action) {
        List<FormListener> list = actionListeners.get(action);
        return list != null ? list.size() : 0;
    }
}