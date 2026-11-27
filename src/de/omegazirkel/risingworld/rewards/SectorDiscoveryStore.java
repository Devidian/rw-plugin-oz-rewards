package de.omegazirkel.risingworld.rewards;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.omegazirkel.risingworld.Rewards;

public class SectorDiscoveryStore {
    private final Connection connection;

    public SectorDiscoveryStore(Connection connection) {
        this.connection = connection;
        initSchema();
    }

    /** Deletes personal reward history and keeps global discoveries without a player reference. */
    public synchronized boolean erasePlayerData(int playerDbId) {
        if (playerDbId <= 0) return false;
        try {
            if (!connection.getAutoCommit()) return false;
            connection.setAutoCommit(false);
            try (PreparedStatement personal = connection.prepareStatement(
                    "DELETE FROM sector_player_discoveries WHERE player_db_id=?");
                    PreparedStatement claims = connection.prepareStatement(
                            "DELETE FROM sector_reward_claims WHERE player_db_id=?");
                    PreparedStatement global = connection.prepareStatement(
                            "UPDATE sector_discoveries SET first_player_db_id=0, region_announced=1 "
                                    + "WHERE first_player_db_id=?")) {
                personal.setInt(1, playerDbId);
                personal.executeUpdate();
                claims.setInt(1, playerDbId);
                claims.executeUpdate();
                global.setInt(1, playerDbId);
                global.executeUpdate();
                connection.commit();
                return true;
            } catch (SQLException ex) {
                connection.rollback();
                Rewards.logger().error("Sector discovery erasure failed: " + ex.getMessage());
                return false;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            Rewards.logger().error("Sector discovery erasure failed: " + ex.getMessage());
            return false;
        }
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
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS sector_reward_claims (
                        claim_id TEXT NOT NULL UNIQUE,
                        sector_x INTEGER NOT NULL,
                        sector_y INTEGER NOT NULL,
                        player_db_id INTEGER NOT NULL,
                        reward_amount INTEGER NOT NULL,
                        reason TEXT NOT NULL,
                        first_discovery INTEGER NOT NULL,
                        state TEXT NOT NULL CHECK (state IN ('PENDING', 'REVIEW_REQUIRED')),
                        PRIMARY KEY (sector_x, sector_y, player_db_id)
                    )
                    """);
            statement.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS sector_reward_claims_player_state
                        ON sector_reward_claims (player_db_id, state)
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
        try {
            return insertGlobalDiscovery(sectorX, sectorY, playerDbId, region);
        } catch (SQLException ex) {
            Rewards.logger().error("Failed to record sector discovery: " + ex.getMessage());
            return false;
        }
    }

    private boolean insertGlobalDiscovery(int sectorX, int sectorY, int playerDbId, String region) throws SQLException {
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
        }
    }

    /** Stores the first-discoverer decision and any payable claim before calling Wallet. */
    public synchronized void recordDiscoveryClaim(int sectorX, int sectorY, int playerDbId, boolean firstOnly,
            RewardTerms firstReward, RewardTerms personalReward) throws SQLException {
        if (!connection.getAutoCommit()) throw new SQLException("Sector discovery requires its own transaction");
        connection.setAutoCommit(false);
        try {
            boolean first = insertGlobalDiscovery(sectorX, sectorY, playerDbId, "Unknown");
            RewardTerms terms = first ? firstReward : personalReward;
            if (terms.amount() > 0 && (first || !firstOnly)) {
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO sector_reward_claims
                            (claim_id, sector_x, sector_y, player_db_id, reward_amount, reason, first_discovery, state)
                        SELECT ?, d.sector_x, d.sector_y, ?, ?, ?, ?, 'PENDING'
                        FROM sector_discoveries d
                        WHERE d.sector_x=? AND d.sector_y=?
                          AND (? OR d.first_player_db_id<>?)
                          AND NOT EXISTS (
                            SELECT 1 FROM sector_player_discoveries p
                            WHERE p.sector_x=d.sector_x AND p.sector_y=d.sector_y AND p.player_db_id=?
                          )
                        ON CONFLICT (sector_x, sector_y, player_db_id) DO NOTHING
                        """)) {
                    // A historical first discovery without a receipt has an unknown Wallet outcome.
                    statement.setString(1, UUID.randomUUID().toString());
                    statement.setInt(2, playerDbId);
                    statement.setLong(3, terms.amount());
                    statement.setString(4, terms.reason());
                    statement.setBoolean(5, first);
                    statement.setInt(6, sectorX);
                    statement.setInt(7, sectorY);
                    statement.setBoolean(8, first);
                    statement.setInt(9, playerDbId);
                    statement.setInt(10, playerDbId);
                    statement.executeUpdate();
                }
            }
            connection.commit();
        } catch (SQLException | RuntimeException ex) {
            connection.rollback();
            throw ex;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    synchronized List<RewardClaim> pendingRewardClaims(int playerDbId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT claim_id, sector_x, sector_y, reward_amount, reason, first_discovery
                FROM sector_reward_claims
                WHERE player_db_id=? AND state='PENDING'
                ORDER BY rowid LIMIT 16
                """)) {
            statement.setInt(1, playerDbId);
            try (ResultSet result = statement.executeQuery()) {
                List<RewardClaim> claims = new ArrayList<>();
                while (result.next()) {
                    claims.add(new RewardClaim(result.getString(1), result.getInt(2), result.getInt(3), playerDbId,
                            result.getLong(4), result.getString(5), result.getBoolean(6)));
                }
                return claims;
            }
        }
    }

    synchronized boolean beginRewardAttempt(String claimId) throws SQLException {
        return changeClaimState(claimId, "PENDING", "REVIEW_REQUIRED");
    }

    synchronized void allowRewardRetry(String claimId) throws SQLException {
        changeClaimState(claimId, "REVIEW_REQUIRED", "PENDING");
    }

    private boolean changeClaimState(String claimId, String from, String to) throws SQLException {
        if (!connection.getAutoCommit()) throw new SQLException("Reward attempt requires a committed claim state");
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE sector_reward_claims SET state=? WHERE claim_id=? AND state=?")) {
            statement.setString(1, to);
            statement.setString(2, claimId);
            statement.setString(3, from);
            return statement.executeUpdate() == 1;
        }
    }

    synchronized void completeRewardClaim(String claimId) throws SQLException {
        if (!connection.getAutoCommit()) throw new SQLException("Reward receipt requires its own transaction");
        connection.setAutoCommit(false);
        try (PreparedStatement receipt = connection.prepareStatement("""
                INSERT INTO sector_player_discoveries (sector_x, sector_y, player_db_id, rewarded_at, reward_amount)
                SELECT sector_x, sector_y, player_db_id, ?, reward_amount
                FROM sector_reward_claims WHERE claim_id=? AND state='REVIEW_REQUIRED'
                """);
                PreparedStatement claim = connection.prepareStatement(
                        "DELETE FROM sector_reward_claims WHERE claim_id=? AND state='REVIEW_REQUIRED'")) {
            receipt.setString(1, Instant.now().toString());
            receipt.setString(2, claimId);
            if (receipt.executeUpdate() != 1) throw new SQLException("Attempted reward claim is missing");
            claim.setString(1, claimId);
            claim.executeUpdate();
            connection.commit();
        } catch (SQLException ex) {
            connection.rollback();
            throw ex;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public record RewardTerms(long amount, String reason) { }

    public record RewardClaim(String id, int sectorX, int sectorY, int playerDbId, long amount, String reason,
            boolean firstDiscovery) { }

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
