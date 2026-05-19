package de.omegazirkel.risingworld.rewards;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.logging.log4j.Level;

import de.omegazirkel.risingworld.Rewards;
import de.omegazirkel.risingworld.tools.OZLogger;

public class PluginSettings {
    private static PluginSettings instance = null;
    private static Rewards plugin;

    public boolean dailyLoginEnabled = true;
    public double dailyLoginBaseBonus = 10;
    public double dailyLoginFactor = 1.25;
    public int dailyLoginStreakLimit = 7;
    public boolean enemyNpcKillEnabled = true;
    public long enemyNpcKillReward = 10;
    public List<Integer> enemyNpcTypeIds = List.of(210, 215);
    public List<String> enemyNpcDefinitionNames = List.of("bandit", "skeleton");
    public Map<String, Long> enemyNpcKillRewardOverrides = Map.of();
    public boolean aggressiveAnimalKillEnabled = true;
    public long aggressiveAnimalKillReward = 5;
    public Map<String, Long> aggressiveAnimalKillRewardOverrides = Map.of();
    public boolean lightningEnabled = true;
    public long lightningReward = 250;
    public String lightningMessageType = "yell";
    public boolean orbitEnabled = true;
    public int orbitChunkY = 64;
    public long orbitReward = 5000;
    public boolean hellEnabled = true;
    public int hellChunkY = -10;
    public long hellReward = 500;
    public boolean sectorDiscoveryEnabled = true;
    public String sectorDiscoveryMode = "firstOnly";
    public long sectorDiscoveryBaseReward = 50;
    public double sectorDiscoveryFirstDiscovererMultiplier = 2.0;
    public String sectorDiscoveryMessageType = "yell";
    public long discordRewardsChannelId = 0;
    public boolean sendPluginWelcome = false;
    public String logLevel = Level.ALL.name();

    private static OZLogger logger() {
        return OZLogger.getInstance("OZ.Rewards.Settings");
    }

    public static PluginSettings getInstance(Rewards p) {
        plugin = p;
        return getInstance();
    }

    public static PluginSettings getInstance() {
        if (instance == null) {
            instance = new PluginSettings();
        }
        return instance;
    }

    private PluginSettings() {
    }

    public void initSettings() {
        initSettings((plugin.getPath() != null ? plugin.getPath() : ".") + "/settings.properties");
    }

    public void initSettings(String filePath) {
        Path settingsFile = Paths.get(filePath);
        Path defaultSettingsFile = settingsFile.resolveSibling("settings.default.properties");

        try {
            if (Files.notExists(settingsFile) && Files.exists(defaultSettingsFile)) {
                logger().info("settings.properties not found, copying from settings.default.properties...");
                Files.copy(defaultSettingsFile, settingsFile);
            }

            Properties settings = new Properties();
            if (Files.exists(settingsFile)) {
                try (FileInputStream in = new FileInputStream(settingsFile.toFile())) {
                    settings.load(new InputStreamReader(in, "UTF8"));
                }
            } else {
                logger().warn("Neither settings.properties nor settings.default.properties found. Using defaults.");
            }

            dailyLoginEnabled = bool(settings, "dailyLogin.enabled", dailyLoginEnabled);
            dailyLoginBaseBonus = dbl(settings, "dailyLogin.baseBonus", dailyLoginBaseBonus);
            dailyLoginFactor = dbl(settings, "dailyLogin.factor", dailyLoginFactor);
            dailyLoginStreakLimit = integer(settings, "dailyLogin.streakLimit", dailyLoginStreakLimit);
            enemyNpcKillEnabled = bool(settings, "enemyNpcKill.enabled", enemyNpcKillEnabled);
            enemyNpcKillReward = lng(settings, "enemyNpcKill.reward", enemyNpcKillReward);
            enemyNpcTypeIds = integerList(settings.getProperty("enemyNpcKill.typeIds", "210,215"),
                    "enemyNpcKill.typeIds");
            enemyNpcDefinitionNames = list(settings.getProperty("enemyNpcKill.definitionNames", "bandit,skeleton"));
            enemyNpcKillRewardOverrides = overrides(settings.getProperty("enemyNpcKill.rewardOverrides", ""));
            aggressiveAnimalKillEnabled = bool(settings, "aggressiveAnimalKill.enabled", aggressiveAnimalKillEnabled);
            aggressiveAnimalKillReward = lng(settings, "aggressiveAnimalKill.reward", aggressiveAnimalKillReward);
            aggressiveAnimalKillRewardOverrides = overrides(
                    settings.getProperty("aggressiveAnimalKill.rewardOverrides", ""));
            lightningEnabled = bool(settings, "lightning.enabled", lightningEnabled);
            lightningReward = lng(settings, "lightning.reward", lightningReward);
            lightningMessageType = settings.getProperty("lightning.messageType", lightningMessageType).trim();
            orbitEnabled = bool(settings, "orbit.enabled", orbitEnabled);
            orbitChunkY = integer(settings, "orbit.chunkY", orbitChunkY);
            orbitReward = lng(settings, "orbit.reward", orbitReward);
            hellEnabled = bool(settings, "hell.enabled", hellEnabled);
            hellChunkY = integer(settings, "hell.chunkY", hellChunkY);
            hellReward = lng(settings, "hell.reward", hellReward);
            sectorDiscoveryEnabled = bool(settings, "sectorDiscovery.enabled", sectorDiscoveryEnabled);
            sectorDiscoveryMode = sectorDiscoveryMode(settings.getProperty("sectorDiscovery.mode", sectorDiscoveryMode));
            sectorDiscoveryBaseReward = lng(settings, "sectorDiscovery.baseReward", sectorDiscoveryBaseReward);
            sectorDiscoveryFirstDiscovererMultiplier = dbl(settings, "sectorDiscovery.firstDiscovererMultiplier",
                    sectorDiscoveryFirstDiscovererMultiplier);
            sectorDiscoveryMessageType = settings.getProperty("sectorDiscovery.messageType", sectorDiscoveryMessageType)
                    .trim();
            discordRewardsChannelId = lng(settings, "discordRewardsChannelId", discordRewardsChannelId);
            sendPluginWelcome = bool(settings, "sendPluginWelcome", sendPluginWelcome);
            logLevel = settings.getProperty("logLevel", "ALL");

            logger().info(plugin.getName() + " Plugin settings loaded");
            logger().info("Loglevel is set to " + logLevel);
            logger().setLevel(logLevel);
        } catch (IOException ex) {
            logger().error("IOException on initSettings: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public long rewardForDefinition(String definitionName, long defaultReward, Map<String, Long> overrides) {
        String normalized = definitionName == null ? "" : definitionName.toLowerCase();
        return overrides.entrySet().stream()
                .filter(entry -> normalized.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(defaultReward);
    }

    private boolean bool(Properties settings, String key, boolean fallback) {
        return settings.getProperty(key, Boolean.toString(fallback)).trim().equalsIgnoreCase("true");
    }

    private int integer(Properties settings, String key, int fallback) {
        try {
            return Integer.parseInt(settings.getProperty(key, Integer.toString(fallback)).trim());
        } catch (NumberFormatException ex) {
            logger().warn("Invalid integer setting " + key + ". Using " + fallback + ".");
            return fallback;
        }
    }

    private long lng(Properties settings, String key, long fallback) {
        try {
            return Long.parseLong(settings.getProperty(key, Long.toString(fallback)).trim());
        } catch (NumberFormatException ex) {
            logger().warn("Invalid long setting " + key + ". Using " + fallback + ".");
            return fallback;
        }
    }

    private double dbl(Properties settings, String key, double fallback) {
        try {
            return Double.parseDouble(settings.getProperty(key, Double.toString(fallback)).trim());
        } catch (NumberFormatException ex) {
            logger().warn("Invalid decimal setting " + key + ". Using " + fallback + ".");
            return fallback;
        }
    }

    private String sectorDiscoveryMode(String value) {
        String normalized = value == null ? "" : value.trim();
        if ("firstOnly".equalsIgnoreCase(normalized)) {
            return "firstOnly";
        }
        if ("perPlayer".equalsIgnoreCase(normalized)) {
            return "perPlayer";
        }
        logger().warn("Invalid sectorDiscovery.mode. Using firstOnly.");
        return "firstOnly";
    }

    private List<String> list(String value) {
        List<String> result = new ArrayList<>();
        for (String item : value.split(",")) {
            String normalized = item.trim().toLowerCase();
            if (!normalized.isBlank()) {
                result.add(normalized);
            }
        }
        return result.isEmpty() ? List.of("bandit", "skeleton") : result;
    }

    private List<Integer> integerList(String value, String key) {
        List<Integer> result = new ArrayList<>();
        for (String item : value.split(",")) {
            String normalized = item.trim();
            if (normalized.isBlank()) {
                continue;
            }
            try {
                result.add(Integer.parseInt(normalized));
            } catch (NumberFormatException ex) {
                logger().warn("Invalid integer list item in setting " + key + " ignored: " + item);
            }
        }
        return result;
    }

    private Map<String, Long> overrides(String value) {
        Map<String, Long> result = new HashMap<>();
        for (String item : value.split(",")) {
            String[] parts = item.split("=", 2);
            if (parts.length != 2) {
                continue;
            }
            String key = parts[0].trim().toLowerCase();
            if (key.isBlank()) {
                continue;
            }
            try {
                result.put(key, Long.parseLong(parts[1].trim()));
            } catch (NumberFormatException ex) {
                logger().warn("Invalid reward override ignored: " + item);
            }
        }
        return result;
    }
}
