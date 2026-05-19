package de.omegazirkel.risingworld.rewards;

import java.lang.reflect.Method;

import de.omegazirkel.risingworld.Rewards;
import net.risingworld.api.Plugin;

public final class DiscordConnect {
    private static Plugin pluginRef = null;

    private DiscordConnect() {
    }

    public static void init(Plugin plugin) {
        pluginRef = plugin.getPluginByName("OZ - Discord Connect");
        if (pluginRef != null) {
            Rewards.logger().info(pluginRef.getName() + " found. ID: " + pluginRef.getID());
        } else {
            Rewards.logger().info("OZ - Discord Connect not available. Discord reward delivery disabled.");
        }
    }

    public static boolean isAvailable() {
        try {
            Class.forName("de.omegazirkel.risingworld.DiscordConnect");
            return pluginRef != null;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static String botLang() {
        Object value = callPluginMethod("getBotLanguage", null, null);
        return value instanceof String ? (String) value : "en";
    }

    public static void sendDiscordMessage(String message, long channelId) {
        callPluginMethod("sendDiscordMessageToTextChannel",
                new Class<?>[] { String.class, long.class, byte[].class },
                new Object[] { message, channelId, null });
    }

    private static Object callPluginMethod(String methodName, Class<?>[] paramTypes, Object[] args) {
        if (!isAvailable()) {
            return null;
        }

        try {
            Method method = pluginRef.getClass().getMethod(methodName, paramTypes);
            return method.invoke(pluginRef, args);
        } catch (Exception e) {
            Rewards.logger().warn("Error while calling DiscordConnect method " + methodName + ": " + e.getMessage());
            return null;
        }
    }
}
