package de.omegazirkel.risingworld.rewards;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.Test;

public class SectorDiscoveryErasureTest {
    @Test public void anonymizesFirstDiscovererAndDeletesOnlyTheirRewardRows() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            SectorDiscoveryStore store = new SectorDiscoveryStore(connection);
            assertTrue(store.recordGlobalDiscovery(1, 2, 17, "Forest"));
            assertTrue(store.recordPlayerReward(1, 2, 17, 10));
            assertTrue(store.recordPlayerReward(1, 2, 18, 5));
            assertTrue(store.erasePlayerData(17));
            assertTrue(store.erasePlayerData(17));
            assertFalse(store.erasePlayerData(0));
            assertNull(store.pendingAnnouncement(1, 2));
            assertFalse(store.hasPlayerDiscovery(1, 2, 17));
            assertTrue(store.hasPlayerDiscovery(1, 2, 18));
            assertEquals(1, count(connection, "SELECT COUNT(*) FROM sector_discoveries "
                    + "WHERE sector_x=1 AND sector_y=2 AND first_player_db_id=0 AND region_announced=1"));
        }
    }

    private static int count(Connection connection, String sql) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            return result.next() ? result.getInt(1) : 0;
        }
    }
}
