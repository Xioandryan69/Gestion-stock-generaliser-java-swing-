package config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {
    private static String url = "jdbc:postgresql://localhost:5432/stock";
    private static String user = "dev1";
    private static String password = "dev";

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Driver PostgreSQL manquant", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public static void setUrl(String url) {
        DatabaseConfig.url = url;
    }
    public static void setUser(String user) {
        DatabaseConfig.user = user;
    }
    public static void setPassword(String password) {
        DatabaseConfig.password = password;
    }
}