package de.omegazirkel.risingworld.rewards;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import de.omegazirkel.risingworld.Rewards;
import de.omegazirkel.risingworld.rewards.SectorDiscoveryStore.RewardClaim;
import de.omegazirkel.risingworld.tools.bridge.WalletBridge.WalletCallResult;

/** Pays Rewards claims through Wallet's default-currency deposit. */
public final class SectorRewardPayout {
    private SectorRewardPayout() { }

    public static List<RewardClaim> payPending(SectorDiscoveryStore store, int playerDbId, boolean walletAvailable,
            Function<RewardClaim, WalletCallResult> deposit) throws SQLException {
        if (!walletAvailable) return List.of();
        List<RewardClaim> paid = new ArrayList<>();
        for (RewardClaim claim : store.pendingRewardClaims(playerDbId)) {
            try {
                if (!store.beginRewardAttempt(claim.id())) continue;
            } catch (SQLException ex) {
                Rewards.logger().error("Could not reserve sector reward " + claim.id() + ": " + ex.getMessage());
                break;
            }

            // A crash or lost response after this point leaves a durable hold for reconciliation.
            try {
                WalletCallResult result = deposit.apply(claim);
                if (result != null && result.success()) {
                    store.completeRewardClaim(claim.id());
                    paid.add(claim);
                } else if (definitelyNotDeposited(result)) {
                    store.allowRewardRetry(claim.id());
                    break;
                } else {
                    Rewards.logger().warn("Wallet outcome unknown for sector reward " + claim.id()
                            + ". Review the Wallet ledger before resolving the claim.");
                }
            } catch (SQLException | RuntimeException ex) {
                Rewards.logger().error("Sector reward " + claim.id() + " requires review: " + ex.getMessage());
                break;
            }
        }
        return paid;
    }

    private static boolean definitelyNotDeposited(WalletCallResult result) {
        if (result == null) return false;
        // These exact provider responses precede any mutation. Missing or other responses remain ambiguous.
        return "Wallet database is not available.".equals(result.message())
                || "Currency is not registered.".equals(result.message());
    }
}
