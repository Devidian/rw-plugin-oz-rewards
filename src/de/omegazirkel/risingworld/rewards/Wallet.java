package de.omegazirkel.risingworld.rewards;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.risingworld.api.Plugin;

public final class Wallet {
    private static Plugin walletPlugin;

    private Wallet() {
    }

    public static void init(Plugin plugin) {
        walletPlugin = plugin.getPluginByName("OZ - Wallet");
    }

    public static boolean isAvailable() {
        try {
            Class.forName("de.omegazirkel.risingworld.Wallet");
            return walletPlugin != null;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public static Object depositDefault(int playerDbId, long value, String reason, String pluginIdentifier) {
        return callWalletMethod(
                "depositDefault",
                new Class<?>[] { int.class, long.class, String.class, String.class },
                new Object[] { playerDbId, value, reason, pluginIdentifier });
    }

    public static boolean isSuccess(Object result) {
        Object success = getResultField(result, "success");
        return success instanceof Boolean && (Boolean) success;
    }

    public static String message(Object result) {
        Object message = getResultField(result, "message");
        return message instanceof String ? (String) message : "";
    }

    private static Object callWalletMethod(String methodName, Class<?>[] paramTypes, Object[] args) {
        if (!isAvailable()) {
            return null;
        }

        try {
            Method method = walletPlugin.getClass().getMethod(methodName, paramTypes);
            return method.invoke(walletPlugin, args);
        } catch (Exception e) {
            return null;
        }
    }

    private static Object getResultField(Object result, String fieldName) {
        if (result == null) {
            return null;
        }

        try {
            Field field = result.getClass().getField(fieldName);
            return field.get(result);
        } catch (Exception e) {
            return null;
        }
    }
}
