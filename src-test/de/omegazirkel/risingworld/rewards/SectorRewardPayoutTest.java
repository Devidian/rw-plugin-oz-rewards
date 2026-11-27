package de.omegazirkel.risingworld.rewards;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import de.omegazirkel.risingworld.rewards.SectorDiscoveryStore.RewardTerms;
import de.omegazirkel.risingworld.tools.bridge.WalletBridge.WalletCallResult;

public class SectorRewardPayoutTest {
    private static final RewardTerms FIRST = new RewardTerms(20, "First discovery");
    private static final RewardTerms PERSONAL = new RewardTerms(5, "Personal discovery");

    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test public void unavailableWalletKeepsFirstEntitlementAndAmountAcrossRestart() throws Exception {
        String url = "jdbc:sqlite:" + folder.newFile("rewards.db").getAbsolutePath();
        try (Connection connection = DriverManager.getConnection(url)) {
            SectorDiscoveryStore store = new SectorDiscoveryStore(connection);
            store.recordDiscoveryClaim(1, 2, 17, true, FIRST, PERSONAL);
            assertTrue(SectorRewardPayout.payPending(store, 17, false, claim -> {
                throw new AssertionError("Unavailable Wallet must not receive a deposit");
            }).isEmpty());
            assertEquals(0L, store.pendingAnnouncement(1, 2).reward());
            store.recordDiscoveryClaim(1, 2, 18, true, FIRST, PERSONAL);
            assertTrue(store.pendingRewardClaims(18).isEmpty());
        }
        try (Connection connection = DriverManager.getConnection(url)) {
            SectorDiscoveryStore store = new SectorDiscoveryStore(connection);
            RewardTerms changed = new RewardTerms(99, "Changed amount");
            store.recordDiscoveryClaim(1, 2, 17, true, changed, changed);
            assertEquals(1, SectorRewardPayout.payPending(store, 17, true, claim -> {
                assertEquals(20, claim.amount());
                assertEquals("First discovery", claim.reason());
                assertTrue(claim.firstDiscovery());
                return WalletCallResult.success("");
            }).size());
            assertEquals(20L, store.pendingAnnouncement(1, 2).reward());
            store.recordDiscoveryClaim(1, 2, 17, false, changed, changed);
            assertTrue(SectorRewardPayout.payPending(store, 17, true, claim -> {
                throw new AssertionError("Payout receipt must prevent replay");
            }).isEmpty());
        }
    }

    @Test public void definitePreWriteFailureCanBeRetried() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            SectorDiscoveryStore store = new SectorDiscoveryStore(connection);
            store.recordDiscoveryClaim(1, 2, 17, true, FIRST, PERSONAL);
            String claimId = store.pendingRewardClaims(17).getFirst().id();
            for (String message : new String[] { "Wallet database is not available.", "Currency is not registered." }) {
                assertTrue(SectorRewardPayout.payPending(store, 17, true,
                        claim -> new WalletCallResult(false, message)).isEmpty());
                assertEquals(claimId, store.pendingRewardClaims(17).getFirst().id());
                assertFalse(store.hasPlayerDiscovery(1, 2, 17));
            }
            assertEquals(1, SectorRewardPayout.payPending(store, 17, true,
                    claim -> WalletCallResult.success("")).size());
            assertTrue(store.hasPlayerDiscovery(1, 2, 17));
        }
    }

    @Test public void ambiguousWalletOutcomeStaysForReviewAcrossRestart() throws Exception {
        String url = "jdbc:sqlite:" + folder.newFile("ambiguous.db").getAbsolutePath();
        try (Connection connection = DriverManager.getConnection(url)) {
            SectorDiscoveryStore store = new SectorDiscoveryStore(connection);
            store.recordDiscoveryClaim(1, 2, 17, true, FIRST, PERSONAL);
            assertTrue(SectorRewardPayout.payPending(store, 17, true,
                    claim -> new WalletCallResult(false, "")).isEmpty());
        }
        try (Connection connection = DriverManager.getConnection(url)) {
            SectorDiscoveryStore store = new SectorDiscoveryStore(connection);
            store.recordDiscoveryClaim(1, 2, 17, false, FIRST, PERSONAL);
            assertHeldForReview(connection, store);
        }
    }

    @Test public void crashAfterAttemptFenceCannotCauseAnotherDeposit() throws Exception {
        String url = "jdbc:sqlite:" + folder.newFile("attempt.db").getAbsolutePath();
        try (Connection connection = DriverManager.getConnection(url)) {
            SectorDiscoveryStore store = new SectorDiscoveryStore(connection);
            store.recordDiscoveryClaim(1, 2, 17, true, FIRST, PERSONAL);
            String id = store.pendingRewardClaims(17).getFirst().id();
            assertTrue(store.beginRewardAttempt(id));
            assertFalse(store.beginRewardAttempt(id));
        }
        try (Connection connection = DriverManager.getConnection(url)) {
            assertHeldForReview(connection, new SectorDiscoveryStore(connection));
        }
    }

    @Test public void failedLocalReceiptAfterWalletSuccessCannotCauseAnotherDeposit() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            SectorDiscoveryStore store = new SectorDiscoveryStore(connection);
            store.recordDiscoveryClaim(1, 2, 17, true, FIRST, PERSONAL);
            try (var statement = connection.createStatement()) {
                statement.executeUpdate("""
                        CREATE TRIGGER fail_receipt BEFORE INSERT ON sector_player_discoveries
                        BEGIN SELECT RAISE(ABORT, 'fixture receipt failure'); END
                        """);
            }
            AtomicInteger deposits = new AtomicInteger();
            assertTrue(SectorRewardPayout.payPending(store, 17, true, claim -> {
                deposits.incrementAndGet();
                return WalletCallResult.success("");
            }).isEmpty());
            assertEquals(1, deposits.get());
            assertHeldForReview(connection, store);
        }
    }

    @Test public void failedClaimInsertDoesNotConsumeFirstDiscovery() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            SectorDiscoveryStore store = new SectorDiscoveryStore(connection);
            try (var statement = connection.createStatement()) {
                statement.executeUpdate("""
                        CREATE TRIGGER fail_claim BEFORE INSERT ON sector_reward_claims
                        BEGIN SELECT RAISE(ABORT, 'fixture claim failure'); END
                        """);
                try {
                    store.recordDiscoveryClaim(1, 2, 17, true, FIRST, PERSONAL);
                    fail("Expected the injected claim insert failure");
                } catch (SQLException ex) {
                    assertTrue(ex.getMessage().contains("fixture claim failure"));
                }
                statement.executeUpdate("DROP TRIGGER fail_claim");
            }
            store.recordDiscoveryClaim(1, 2, 17, true, FIRST, PERSONAL);
            assertTrue(store.pendingRewardClaims(17).getFirst().firstDiscovery());
        }
    }

    @Test public void perPlayerModeKeepsSeparateClaimsAndErasesOnlySelectedPlayer() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            SectorDiscoveryStore store = new SectorDiscoveryStore(connection);
            store.recordDiscoveryClaim(1, 2, 17, false, FIRST, PERSONAL);
            store.recordDiscoveryClaim(1, 2, 18, false, FIRST, PERSONAL);
            assertEquals(20, store.pendingRewardClaims(17).getFirst().amount());
            assertEquals(5, store.pendingRewardClaims(18).getFirst().amount());
            assertFalse(store.pendingRewardClaims(18).getFirst().firstDiscovery());
            assertTrue(store.erasePlayerData(17));
            assertTrue(store.pendingRewardClaims(17).isEmpty());
            assertEquals(1, store.pendingRewardClaims(18).size());
        }
    }

    @Test public void historicalFirstWithoutReceiptAndHistoricalPaidPlayerAreNotReplayed() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            SectorDiscoveryStore store = new SectorDiscoveryStore(connection);
            store.recordGlobalDiscovery(1, 2, 17, "Forest");
            store.recordPlayerReward(1, 2, 18, 5);
            store.recordDiscoveryClaim(1, 2, 17, false, FIRST, PERSONAL);
            store.recordDiscoveryClaim(1, 2, 18, false, FIRST, PERSONAL);
            assertTrue(store.pendingRewardClaims(17).isEmpty());
            assertTrue(store.pendingRewardClaims(18).isEmpty());
        }
    }

    private static void assertHeldForReview(Connection connection, SectorDiscoveryStore store) throws Exception {
        assertTrue(SectorRewardPayout.payPending(store, 17, true, claim -> {
            throw new AssertionError("Review hold must prevent another deposit");
        }).isEmpty());
        assertFalse(store.hasPlayerDiscovery(1, 2, 17));
        try (var statement = connection.createStatement();
                var result = statement.executeQuery("SELECT state FROM sector_reward_claims WHERE player_db_id=17")) {
            assertTrue(result.next());
            assertEquals("REVIEW_REQUIRED", result.getString(1));
            assertFalse(result.next());
        }
    }
}
