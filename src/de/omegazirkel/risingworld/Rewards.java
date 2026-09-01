package de.omegazirkel.risingworld;

import java.nio.file.Path;

import de.omegazirkel.risingworld.tools.FileChangeListener;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.OZLogger;
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.npc.NpcDeathEvent;
import net.risingworld.api.events.player.PlayerCommandEvent;
import net.risingworld.api.events.player.PlayerEnterBiomeEvent;
import net.risingworld.api.events.player.PlayerEnterChunkEvent;
import net.risingworld.api.events.player.PlayerEnterSectorEvent;
import net.risingworld.api.events.player.PlayerSpawnEvent;
import net.risingworld.api.events.world.LightningStrikeEvent;

/** Rising World entry point; reward workflows live in {@link RewardsRuntime}. */
public final class Rewards extends RewardsRuntime implements Listener, FileChangeListener {
    public static final String PLUGIN_IDENTIFIER = RewardsRuntime.PLUGIN_IDENTIFIER;
    public static final String COMMAND = RewardsRuntime.COMMAND;
    public static final String LOGIN_LAST_GMT_DATE_KEY = RewardsRuntime.LOGIN_LAST_GMT_DATE_KEY;
    public static final String LOGIN_STREAK_COUNT_KEY = RewardsRuntime.LOGIN_STREAK_COUNT_KEY;
    public static final String ORBIT_REACHED_AT_KEY = RewardsRuntime.ORBIT_REACHED_AT_KEY;
    public static final String HELL_REACHED_AT_KEY = RewardsRuntime.HELL_REACHED_AT_KEY;

    public static OZLogger logger() {
        return RewardsRuntime.logger();
    }

    public static I18n i18n() {
        return RewardsRuntime.i18n();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        registerEventListener(this);
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @Override
    public void onSettingsChanged(Path settingsPath) {
        super.onSettingsChanged(settingsPath);
    }

    @Override @EventMethod
    public void onPlayerCommand(PlayerCommandEvent event) { super.onPlayerCommand(event); }

    @Override @EventMethod
    public void onPlayerSpawnEvent(PlayerSpawnEvent event) { super.onPlayerSpawnEvent(event); }

    @Override @EventMethod
    public void onPlayerEnterChunk(PlayerEnterChunkEvent event) { super.onPlayerEnterChunk(event); }

    @Override @EventMethod
    public void onPlayerEnterSector(PlayerEnterSectorEvent event) { super.onPlayerEnterSector(event); }

    @EventMethod
    public void onPlayerEnterBiome(PlayerEnterBiomeEvent event) { super.onPlayerEnterBiome(event); }

    @Override @EventMethod
    public void onNpcDeath(NpcDeathEvent event) { super.onNpcDeath(event); }

    @Override @EventMethod
    public void onLightningStrike(LightningStrikeEvent event) { super.onLightningStrike(event); }
}
