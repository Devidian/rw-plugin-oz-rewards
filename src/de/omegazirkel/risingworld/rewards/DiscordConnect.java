package de.omegazirkel.risingworld.rewards;

import de.omegazirkel.risingworld.Rewards;
import de.omegazirkel.risingworld.tools.bridge.DiscordBridge;
import net.risingworld.api.Plugin;

public final class DiscordConnect extends DiscordBridge {
    private static DiscordConnect bridge;

    private DiscordConnect(Plugin owner) {
        super(owner);
    }

    public static void init(Plugin plugin) {
        bridge = new DiscordConnect(plugin);
        if (bridge.isAvailable()) {
            Rewards.logger().info("OZ - Discord Connect found.");
        } else {
            Rewards.logger().info("OZ - Discord Connect not available. Discord reward delivery disabled.");
        }
    }

    public static boolean isDiscordAvailable() {
        return bridge != null && bridge.isAvailable();
    }

    public static String botLang() {
        return bridge == null ? "en" : bridge.getBotLanguage();
    }

    public static void sendDiscordMessage(String message, long channelId) {
        if (bridge != null) bridge.sendTextMessage(message, channelId);
    }
}
