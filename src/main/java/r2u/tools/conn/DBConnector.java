package r2u.tools.conn;

import oracle.jdbc.OracleDriver;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBConnector {
    private static String dbHostname;
    private static String dbUsername;
    private static String dbPassword;
    static Connection connection;
    static Logger logger;

    public DBConnector() {
    }

    public static ResultSet executeQuery(String query) {
        try {
            logger.log(Level.INFO, "Registering Oracle Driver");
            logger.log(Level.INFO, "Trying to open connection using: (" + dbHostname + "," + dbUsername + "," + dbPassword);
            DriverManager.registerDriver(new OracleDriver());
            connection = DriverManager.getConnection(dbHostname, dbUsername, dbPassword);
            Statement statement = connection.createStatement();
            return statement.executeQuery(query);
        } catch (SQLException exception) {
            logger.severe(exception.getMessage());
            exception.printStackTrace();
        }
        return null;
    }

    public static void disconnect() throws SQLException {
        connection.close();
    }

    public static void initDB(String dbHostname, String dbUsername, String dbPassword) {
        DBConnector.dbHostname = dbHostname;
        DBConnector.dbUsername = dbUsername;
        DBConnector.dbPassword = dbPassword;
    }

    public static void initLogger(Logger logger) {
        DBConnector.logger = logger;
    }
}
