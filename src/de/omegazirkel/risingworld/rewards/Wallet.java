package de.omegazirkel.risingworld.rewards;

import de.omegazirkel.risingworld.tools.bridge.WalletBridge;
import net.risingworld.api.Plugin;

public final class Wallet {
    private static WalletBridge bridge;

    private Wallet() {
    }

    public static void init(Plugin plugin) {
        bridge = new WalletBridge(plugin);
    }

    public static boolean isAvailable() {
        return bridge != null && bridge.isAvailable();
    }

    public static WalletBridge.WalletCallResult depositDefault(int playerDbId, long value, String reason,
            String pluginIdentifier) {
        return bridge == null ? new WalletBridge.WalletCallResult(false, "Wallet unavailable")
                : bridge.depositDefault(playerDbId, value, reason, pluginIdentifier);
    }

    public static boolean isSuccess(WalletBridge.WalletCallResult result) {
        return result != null && result.success();
    }

    public static String message(WalletBridge.WalletCallResult result) {
        return result == null ? "" : result.message();
    }
}
