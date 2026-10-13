package de.omegazirkel.risingworld.rewards;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.risingworld.api.World;
import net.risingworld.api.definitions.Definitions;
import net.risingworld.api.definitions.Npcs;

import de.omegazirkel.risingworld.Rewards;

/** World-specific kill payouts. Missing IDs have no payout. */
public final class KillRewardCatalog {
    private final Path file;
    private final Map<Integer, Long> rewards = new LinkedHashMap<>();

    public KillRewardCatalog(String pluginPath, PluginSettings legacy) throws IOException {
        String world = World.getName();
        String safeWorld = (world == null || world.isBlank() ? "default" : world)
                .replaceAll("[^A-Za-z0-9._-]", "_");
        Path directory = Path.of(pluginPath == null ? "." : pluginPath);
        file = directory.resolve("killrewards." + safeWorld + ".json");
        if (Files.notExists(file)) {
            if (legacy.hasLegacyKillValues) migrate(legacy);
            if (!legacy.hasLegacyKillValues) {
                Path defaults = directory.resolve("killrewards.default.json");
                if (Files.exists(defaults)) Files.copy(defaults, file);
                else throw new IOException("Missing killrewards.default.json");
            } else save();
        }
        reload();
    }

    private void migrate(PluginSettings legacy) {
        Npcs.NpcDefinition[] definitions = Definitions.getAllNpcDefinitions();
        if (definitions == null) return;
        for (Npcs.NpcDefinition definition : definitions) {
            if (definition == null) continue;
            String name = definition.name == null ? "" : definition.name.toLowerCase();
            boolean enemy = legacy.enemyNpcTypeIds.contains((int) definition.id)
                    || legacy.enemyNpcDefinitionNames.stream().anyMatch(name::contains);
            boolean animal = definition.type == Npcs.Type.Animal
                    && (definition.behaviour == Npcs.Behaviour.Aggressive
                    || definition.behaviour == Npcs.Behaviour.DefensiveAggressive);
            long value = enemy
                    ? legacy.rewardForDefinition(name, legacy.enemyNpcKillReward, legacy.enemyNpcKillRewardOverrides)
                    : animal ? legacy.rewardForDefinition(name, legacy.aggressiveAnimalKillReward,
                            legacy.aggressiveAnimalKillRewardOverrides) : 0L;
            if (value != 0L) rewards.put((int) definition.id, value);
        }
    }

    public synchronized void reload() throws IOException {
        JsonElement root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
        if (!root.isJsonArray()) throw new IOException("Kill rewards must be a JSON array");
        Map<Integer, Long> loaded = new LinkedHashMap<>();
        for (JsonElement element : root.getAsJsonArray()) {
            if (!element.isJsonObject()) throw new IOException("Invalid kill reward entry");
            JsonObject entry = element.getAsJsonObject();
            if (!entry.has("typid") || !entry.has("reward")) throw new IOException("Missing typid or reward");
            int id = entry.get("typid").getAsInt();
            long value = entry.get("reward").getAsLong();
            if (id < 0 || id > Short.MAX_VALUE || loaded.containsKey(id))
                throw new IOException("Invalid or duplicate NPC ID: " + id);
            if (value != 0) loaded.put(id, value);
        }
        rewards.clear();
        rewards.putAll(loaded);
    }

    public synchronized long reward(int id) { return rewards.getOrDefault(id, 0L); }

    public synchronized void set(int id, long reward) throws IOException {
        if (Definitions.getNpcDefinition(id) == null) throw new IOException("Unknown NPC ID: " + id);
        Map<Integer, Long> old = new LinkedHashMap<>(rewards);
        if (reward == 0) rewards.remove(id); else rewards.put(id, reward);
        try { save(); } catch (IOException ex) { rewards.clear(); rewards.putAll(old); throw ex; }
    }

    private void save() throws IOException {
        Files.createDirectories(file.getParent());
        JsonArray array = new JsonArray();
        rewards.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            JsonObject item = new JsonObject();
            item.addProperty("typid", entry.getKey());
            item.addProperty("reward", entry.getValue());
            array.add(item);
        });
        Path temp = Files.createTempFile(file.getParent(), "killrewards-", ".tmp");
        try {
            Files.writeString(temp, new Gson().toJson(array) + "\n", StandardCharsets.UTF_8);
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
    }
}
