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
        databaseIp = "localhost";
        databaseUsername = "ops";
        databasePassword = "opsops";
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
            statement.setDouble(2, temperature / 100.0);
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

    // public void updateBounds(int subzone,int t_low, int t_high, int p_low, int p_high) {
    //     try (
    //             Connection connection = getConnection();
    //             PreparedStatement statement = connection.prepareStatement("UPDATE bounds SET t_low = ?, t_high = ?, p_low = ?, p_high = ? WHERE subzone = ?")
    //     ) {
    //         statement.setFloat(1, t_low / 100);
    //         statement.setFloat(2, t_high / 100);
    //         statement.setInt(3, p_low);
    //         statement.setInt(4, p_high);
    //         statement.setInt(5, subzone);
    //         statement.executeUpdate();
    //     }
    //     catch (final SQLException e) {
    //         e.printStackTrace();
    //         System.err.println("Error in the SQL insert!");
    //     }
    // }

}
