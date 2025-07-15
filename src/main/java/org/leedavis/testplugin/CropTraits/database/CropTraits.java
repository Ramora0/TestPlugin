package org.leedavis.testplugin.CropTraits.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.leedavis.testplugin.CropTraits.utils.CropNBT;

public class CropTraits {
    // ========================================
    // FIELDS
    // ========================================

    private final Connection connection;

    // ========================================
    // CONSTRUCTOR
    // ========================================

    public CropTraits(String path) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + path);

        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS wheat_traits(\n" +
                    "    world TEXT,\n" +
                    "    x INTEGER,\n" +
                    "    y INTEGER,\n" +
                    "    z INTEGER,\n" +
                    "    quality INTEGER,\n" +
                    "    resistance INTEGER,\n" +
                    "    yield INTEGER,\n" +
                    "    PRIMARY KEY (world, x, y, z)\n" +
                    ");");
        }
    }

    // ========================================
    // CRUD OPERATIONS
    // ========================================

    public void createCrop(String world, int x, int y, int z, CropNBT cropData) throws SQLException {
        String sql = "INSERT OR REPLACE INTO wheat_traits (world, x, y, z, quality, resistance, yield) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, world);
            statement.setInt(2, x);
            statement.setInt(3, y);
            statement.setInt(4, z);
            statement.setInt(5, cropData == null ? 0 : cropData.getQuality());
            statement.setInt(6, cropData == null ? 0 : cropData.getResistance());
            statement.setInt(7, cropData == null ? 0 : cropData.getYield());

            statement.executeUpdate();
        }
    }

    public CropNBT getCropTraits(String world, int x, int y, int z) {
        String sql = "SELECT quality, resistance, yield FROM wheat_traits WHERE world = ? AND x = ? AND y = ? AND z = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, world);
            statement.setInt(2, x);
            statement.setInt(3, y);
            statement.setInt(4, z);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new CropNBT(
                            resultSet.getInt("quality"),
                            resultSet.getInt("resistance"),
                            resultSet.getInt("yield"));
                } else {
                    return new CropNBT(0, 0, 0);
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            System.out.println("Failed to get crop traits for location: " +
                    world + " " + x + "," + y + "," + z);
            return null;
        }
    }

    public boolean hasCrop(String world, int x, int y, int z) {
        String sql = "SELECT COUNT(*) FROM wheat_traits WHERE world = ? AND x = ? AND y = ? AND z = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, world);
            statement.setInt(2, x);
            statement.setInt(3, y);
            statement.setInt(4, z);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            System.out.println("Failed to check crop existence for location: " +
                    world + " " + x + "," + y + "," + z);
        }
        return false;
    }

    public void deleteCrop(String world, int x, int y, int z) throws SQLException {
        String sql = "DELETE FROM wheat_traits WHERE world = ? AND x = ? AND y = ? AND z = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, world);
            statement.setInt(2, x);
            statement.setInt(3, y);
            statement.setInt(4, z);

            statement.executeUpdate();
        }
    }

    // ========================================
    // CONNECTION MANAGEMENT
    // ========================================

    public void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
