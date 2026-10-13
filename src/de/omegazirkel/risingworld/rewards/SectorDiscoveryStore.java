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
                        region_announced INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY (sector_x, sector_y)
                    )
                    """);
            if (!hasColumn("sector_discoveries", "region_announced")) {
                statement.executeUpdate("ALTER TABLE sector_discoveries ADD COLUMN region_announced INTEGER NOT NULL DEFAULT 1");
            }
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
                    (sector_x, sector_y, first_player_db_id, discovered_at, region, region_announced)
                VALUES (?, ?, ?, ?, ?, 0)
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

    public synchronized PendingAnnouncement pendingAnnouncement(int sectorX, int sectorY) {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT d.first_player_db_id, d.discovered_at, p.reward_amount
                FROM sector_discoveries d LEFT JOIN sector_player_discoveries p
                  ON p.sector_x=d.sector_x AND p.sector_y=d.sector_y
                 AND p.player_db_id=d.first_player_db_id
                WHERE d.sector_x=? AND d.sector_y=? AND d.region_announced=0
                """)) {
            statement.setInt(1, sectorX);
            statement.setInt(2, sectorY);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? new PendingAnnouncement(result.getInt(1),
                        Instant.parse(result.getString(2)), result.getLong(3)) : null;
            }
        } catch (SQLException | RuntimeException ex) {
            Rewards.logger().error("Failed to load pending sector announcement: " + ex.getMessage());
            return null;
        }
    }

    public synchronized boolean markRegionAnnounced(int sectorX, int sectorY, String region) {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE sector_discoveries SET region=?, region_announced=1
                WHERE sector_x=? AND sector_y=? AND region_announced=0
                """)) {
            statement.setString(1, region);
            statement.setInt(2, sectorX);
            statement.setInt(3, sectorY);
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            Rewards.logger().error("Failed to finalize sector region: " + ex.getMessage());
            return false;
        }
    }

    public record PendingAnnouncement(int firstPlayerDbId, Instant discoveredAt, long reward) { }

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
