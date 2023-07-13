package unipi.iot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DBManager {
    private static DBManager instance = null;
    private static final String databaseIp;
    private static final String databaseUsername;
    private static final String databasePassword;
    private static final String databaseName;

    public static DBManager getInstance() {
        if(instance == null)
            instance = new DBManager();

        return instance;
    }

    static {
        databaseIp = "127.0.0.1";
        databaseUsername = "root";
        databasePassword = "rootroot";
        databaseName = "ioT_Project";
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://"+ databaseIp +
                        "/" + databaseName + "?zeroDateTimeBehavior=CONVERT_TO_NULL&serverTimezone=CET",
                databaseUsername, databasePassword);
    }

    public void insertTempSample(int subzone,int temperature) {
        try (
                Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement("INSERT INTO temperature(subzone, value) VALUES (?, ?)")
        ) {
            statement.setInt(1, subzone);
            statement.setFloat(2, temperature / 100);
            statement.executeUpdate();
        }
        catch (final SQLException e) {
            e.printStackTrace();
            System.err.println("Error in the SQL insert!");
        }
    }

    public void insertCPSample(int subzone,int consumed_power) {
        try (
                Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement("INSERT INTO `consumed power`(subzone, value) VALUES (?, ?)")
        ) {
            statement.setInt(1, subzone);
            statement.setInt(2, consumed_power);
            statement.executeUpdate();
        }
        catch (final SQLException e) {
            e.printStackTrace();
            System.err.println("Error in the SQL insert!");
        }
    }
}
