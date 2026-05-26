package com.borrowit.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {
    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException exception) {
            throw new SQLException("MySQL JDBC driver was not found. Check the Maven dependency.", exception);
        }

        return DriverManager.getConnection(
                DatabaseConfig.getUrl(),
                DatabaseConfig.getUsername(),
                DatabaseConfig.getPassword()
        );
    }

    public static boolean testConnection() {
        try (Connection connection = getConnection()) {
            return connection.isValid(3);
        } catch (SQLException exception) {
            return false;
        }
    }
}
