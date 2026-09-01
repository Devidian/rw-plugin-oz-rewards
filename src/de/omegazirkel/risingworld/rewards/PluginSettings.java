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

import de.omegazirkel.risingworld.Rewards;
import de.omegazirkel.risingworld.tools.OZLogger;
import de.omegazirkel.risingworld.tools.settings.AdminSettingsEntry;
import de.omegazirkel.risingworld.tools.settings.AdminSettingsType;
import de.omegazirkel.risingworld.tools.settings.JsonSettingsFile;
import de.omegazirkel.risingworld.tools.settings.SettingsFileEditor;

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

    private static OZLogger logger() {
        return Rewards.logger();
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
        initSettings(JsonSettingsFile.worldSettingsFile(plugin.getPath() != null ? plugin.getPath() : ".").toString());
    }

    public void initSettings(String filePath) {
        Path settingsFile = Paths.get(filePath);
        Path defaultSettingsFile = settingsFile.resolveSibling("settings.default.json");
        Path legacySettingsFile = settingsFile.resolveSibling("settings.properties");

        try {
            if (JsonSettingsFile.migrateLegacyProperties(legacySettingsFile, settingsFile))
                logger().info("Migrated legacy settings.properties to " + settingsFile.getFileName());
            if (Files.notExists(settingsFile) && Files.exists(defaultSettingsFile))
                JsonSettingsFile.writeFlatAtomically(settingsFile, JsonSettingsFile.loadFlat(defaultSettingsFile));
            Properties settings = loadSettings(settingsFile);
            if (settings.isEmpty()) {
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
            logger().info(plugin.getName() + " Plugin settings loaded");
        } catch (IOException ex) {
            logger().error("IOException on initSettings: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public List<AdminSettingsEntry> adminSettingsEntries() {
        return List.of(
                AdminSettingsEntry.group("general", "General", "Welcome behavior."),
                entry("sendPluginWelcome", "Welcome message", "Shows a short Rewards message when a player joins.",
                        sendPluginWelcome, "false", AdminSettingsType.BOOLEAN),
                AdminSettingsEntry.group("dailyLogin", "Daily login", "Daily login reward formula."),
                entry("dailyLogin.enabled", "Daily login", "Enables daily login rewards.", dailyLoginEnabled, "true",
                        AdminSettingsType.BOOLEAN),
                entry("dailyLogin.baseBonus", "Daily login base", "Base amount for daily login rewards.",
                        dailyLoginBaseBonus, "10", AdminSettingsType.DECIMAL),
                entry("dailyLogin.factor", "Daily login factor",
                        "Decimal streak multiplier for daily login rewards.", dailyLoginFactor, "1.10",
                        AdminSettingsType.DECIMAL),
                entry("dailyLogin.streakLimit", "Daily login streak limit",
                        "Maximum exponent used by the daily login streak formula.", dailyLoginStreakLimit, "7",
                        AdminSettingsType.INTEGER),
                AdminSettingsEntry.group("enemyNpc", "Enemy NPC kills", "Enemy NPC kill reward detection and payout."),
                entry("enemyNpcKill.enabled", "Enemy NPC rewards", "Enables enemy NPC kill rewards.",
                        enemyNpcKillEnabled, "true", AdminSettingsType.BOOLEAN),
                entry("enemyNpcKill.reward", "Enemy NPC reward", "Default reward for matched enemy NPC kills.",
                        enemyNpcKillReward, "10", AdminSettingsType.INTEGER),
                readOnlyEntry("enemyNpcKill.typeIds", "Enemy NPC type ids",
                        "Comma-separated NPC type ids treated as rewardable enemies.",
                        joinIntegers(enemyNpcTypeIds), "210,215", AdminSettingsType.STRING),
                readOnlyEntry("enemyNpcKill.definitionNames", "Enemy NPC definitions",
                        "Comma-separated definition-name fragments treated as rewardable enemies.",
                        String.join(",", enemyNpcDefinitionNames), "bandit,skeleton", AdminSettingsType.STRING),
                readOnlyEntry("enemyNpcKill.rewardOverrides", "Enemy NPC reward overrides",
                        "Comma-separated definition reward overrides such as skeleton=5.",
                        joinOverrides(enemyNpcKillRewardOverrides), "", AdminSettingsType.STRING),
                AdminSettingsEntry.group("animals", "Aggressive animals", "Aggressive animal kill reward settings."),
                entry("aggressiveAnimalKill.enabled", "Aggressive animal rewards",
                        "Enables aggressive animal kill rewards.", aggressiveAnimalKillEnabled, "true",
                        AdminSettingsType.BOOLEAN),
                entry("aggressiveAnimalKill.reward", "Aggressive animal reward",
                        "Default reward for aggressive animal kills.", aggressiveAnimalKillReward, "5",
                        AdminSettingsType.INTEGER),
                readOnlyEntry("aggressiveAnimalKill.rewardOverrides", "Animal reward overrides",
                        "Comma-separated animal reward overrides such as wolf=15.",
                        joinOverrides(aggressiveAnimalKillRewardOverrides), "", AdminSettingsType.STRING),
                AdminSettingsEntry.group("survival", "Survival milestones",
                        "Lightning, orbit, and hell visit reward settings."),
                entry("lightning.enabled", "Lightning rewards", "Enables lightning/storm rewards.", lightningEnabled,
                        "true", AdminSettingsType.BOOLEAN),
                entry("lightning.reward", "Lightning reward", "Reward paid after the lightning workaround triggers.",
                        lightningReward, "2500", AdminSettingsType.INTEGER),
                readOnlyEntry("lightning.messageType", "Lightning message type",
                        "Announcement mode for lightning rewards: yell or chat.", lightningMessageType, "yell",
                        AdminSettingsType.STRING),
                entry("orbit.enabled", "Orbit reward", "Enables the first orbit visit reward.", orbitEnabled, "true",
                        AdminSettingsType.BOOLEAN),
                entry("orbit.chunkY", "Orbit chunk Y", "Minimum vertical chunk for the orbit reward.", orbitChunkY,
                        "64", AdminSettingsType.INTEGER),
                entry("orbit.reward", "Orbit reward amount", "Reward for first orbit visit.", orbitReward, "5000",
                        AdminSettingsType.INTEGER),
                entry("hell.enabled", "Hell reward", "Enables the first hell visit reward.", hellEnabled, "true",
                        AdminSettingsType.BOOLEAN),
                entry("hell.chunkY", "Hell chunk Y", "Maximum vertical chunk for the hell reward.", hellChunkY,
                        "-10", AdminSettingsType.INTEGER),
                entry("hell.reward", "Hell reward amount", "Reward for first hell visit.", hellReward, "500",
                        AdminSettingsType.INTEGER),
                AdminSettingsEntry.group("sectorDiscovery", "Sector discovery",
                        "Sector discovery reward formula and announcement behavior."),
                entry("sectorDiscovery.enabled", "Sector discovery rewards",
                        "Enables sector discovery rewards.", sectorDiscoveryEnabled, "true",
                        AdminSettingsType.BOOLEAN),
                readOnlyEntry("sectorDiscovery.mode", "Sector discovery mode",
                        "Reward mode: firstOnly or perPlayer.", sectorDiscoveryMode, "perPlayer",
                        AdminSettingsType.STRING),
                entry("sectorDiscovery.baseReward", "Sector base reward",
                        "Base reward multiplied by sector distance.", sectorDiscoveryBaseReward, "50",
                        AdminSettingsType.INTEGER),
                entry("sectorDiscovery.firstDiscovererMultiplier", "First discoverer multiplier",
                        "Decimal multiplier for the global first discoverer.",
                        sectorDiscoveryFirstDiscovererMultiplier, "2.0", AdminSettingsType.DECIMAL),
                readOnlyEntry("sectorDiscovery.messageType", "Sector message type",
                        "Announcement mode for first discoveries: yell or chat.", sectorDiscoveryMessageType, "yell",
                        AdminSettingsType.STRING),
                AdminSettingsEntry.group("discord", "Discord", "Optional Discord reward announcements."),
                entry("discordRewardsChannelId", "Discord rewards channel",
                        "Discord channel id for reward announcements; 0 disables Discord reward messages.",
                        discordRewardsChannelId, "0", AdminSettingsType.STRING));
    }

    private AdminSettingsEntry entry(String key, String label, String description, Object value, String defaultValue,
            AdminSettingsType type) {
        return new AdminSettingsEntry(
                key,
                label,
                description,
                String.valueOf(value),
                defaultValue,
                type,
                false,
                newValue -> SettingsFileEditor.writeValue(settingsPath(), key, newValue));
    }

    private AdminSettingsEntry readOnlyEntry(String key, String label, String description, Object value,
            String defaultValue, AdminSettingsType type) {
        return new AdminSettingsEntry(
                key,
                label,
                description,
                String.valueOf(value),
                defaultValue,
                type,
                false,
                null);
    }

    private Path settingsPath() {
        return JsonSettingsFile.worldSettingsFile(plugin.getPath() != null ? plugin.getPath() : ".");
    }

    private Properties loadSettings(Path file) throws IOException {
        if (!file.getFileName().toString().endsWith(".properties")) return JsonSettingsFile.loadProperties(file);
        Properties properties = new Properties();
        if (Files.exists(file)) try (FileInputStream input = new FileInputStream(file.toFile())) {
            properties.load(new InputStreamReader(input, "UTF8"));
        }
        return properties;
    }

    private String joinIntegers(List<Integer> values) {
        return values.stream().map(String::valueOf).reduce((left, right) -> left + "," + right).orElse("");
    }

    private String joinOverrides(Map<String, Long> values) {
        return values.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + "," + right)
                .orElse("");
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
