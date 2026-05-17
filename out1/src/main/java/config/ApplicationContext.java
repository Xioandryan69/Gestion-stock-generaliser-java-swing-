package config;

import java.util.HashMap;
import java.util.Map;

public class ApplicationContext {
    private static ApplicationContext instance;
    private Map<String, Object> beans = new HashMap<>();

    private ApplicationContext() {}

    public static ApplicationContext getInstance() {
        if (instance == null) instance = new ApplicationContext();
        return instance;
    }

    public <T> void registerBean(String name, T bean) {
        beans.put(name, bean);
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(String name) {
        return (T) beans.get(name);
    }
}