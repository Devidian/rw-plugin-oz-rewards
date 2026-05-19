package de.omegazirkel.risingworld.rewards;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;

import de.omegazirkel.risingworld.Rewards;

public class SectorDiscoveryStore {
    private final Connection connection;

    public SectorDiscoveryStore(Connection connection) {
        this.connection = connection;
        initSchema();
    }

    private void initSchema() {
        try (Statement statement = connection.createStatement()) {
            dropUnreleasedBiomeSchema(statement);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS sector_discoveries (
                        sector_x INTEGER NOT NULL,
                        sector_y INTEGER NOT NULL,
                        first_player_db_id INTEGER NOT NULL,
                        discovered_at TEXT NOT NULL,
                        region TEXT NOT NULL,
                        PRIMARY KEY (sector_x, sector_y)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS sector_player_discoveries (
                        sector_x INTEGER NOT NULL,
                        sector_y INTEGER NOT NULL,
                        player_db_id INTEGER NOT NULL,
                        rewarded_at TEXT NOT NULL,
                        reward_amount INTEGER NOT NULL,
                        PRIMARY KEY (sector_x, sector_y, player_db_id)
                    )
                    """);
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to initialize sector discovery tables", ex);
        }
    }

    private void dropUnreleasedBiomeSchema(Statement statement) throws SQLException {
        if (hasColumn("sector_discoveries", "biome") && !hasColumn("sector_discoveries", "region")) {
            statement.executeUpdate("DROP TABLE sector_discoveries");
        }
    }

    private boolean hasColumn(String tableName, String columnName) throws SQLException {
        try (ResultSet result = connection.getMetaData().getColumns(null, null, tableName, columnName)) {
            return result.next();
        }
    }

    public synchronized boolean recordGlobalDiscovery(int sectorX, int sectorY, int playerDbId, String region) {
        String sql = """
                INSERT OR IGNORE INTO sector_discoveries
                    (sector_x, sector_y, first_player_db_id, discovered_at, region)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, sectorX);
            statement.setInt(2, sectorY);
            statement.setInt(3, playerDbId);
            statement.setString(4, Instant.now().toString());
            statement.setString(5, region);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            Rewards.logger().error("Failed to record sector discovery: " + ex.getMessage());
            return false;
        }
    }

    public synchronized boolean hasPlayerDiscovery(int sectorX, int sectorY, int playerDbId) {
        String sql = """
                SELECT 1
                FROM sector_player_discoveries
                WHERE sector_x = ? AND sector_y = ? AND player_db_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, sectorX);
            statement.setInt(2, sectorY);
            statement.setInt(3, playerDbId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException ex) {
            Rewards.logger().error("Failed to read sector player discovery: " + ex.getMessage());
            return true;
        }
    }

    public synchronized boolean recordPlayerReward(int sectorX, int sectorY, int playerDbId, long rewardAmount) {
        String sql = """
                INSERT OR IGNORE INTO sector_player_discoveries
                    (sector_x, sector_y, player_db_id, rewarded_at, reward_amount)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, sectorX);
            statement.setInt(2, sectorY);
            statement.setInt(3, playerDbId);
            statement.setString(4, Instant.now().toString());
            statement.setLong(5, rewardAmount);
            return statement.executeUpdate() > 0;
        } catch (SQLException ex) {
            Rewards.logger().error("Failed to record sector reward: " + ex.getMessage());
            return false;
        }
    }
}
