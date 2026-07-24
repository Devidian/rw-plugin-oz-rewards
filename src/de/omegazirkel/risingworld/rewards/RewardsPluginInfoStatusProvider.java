package de.omegazirkel.risingworld.rewards;

import de.omegazirkel.risingworld.Rewards;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.PluginInfoStatusProvider;
import net.risingworld.api.objects.Player;

public class RewardsPluginInfoStatusProvider implements PluginInfoStatusProvider {
    private final Rewards plugin;
    private final String pluginName;
    private final String version;

    public RewardsPluginInfoStatusProvider(Rewards plugin, String version) {
        this.plugin = plugin;
        this.pluginName = Rewards.name == null || Rewards.name.isBlank() ? "OZ - Rewards" : Rewards.name;
        this.version = version == null ? "" : version;
    }

    @Override
    public String getPluginName() {
        return pluginName;
    }

    @Override
    public String getInfo(Player player) {
        return t().get("TC_REWARDS_INFO_PANEL_INFO", player)
                .replace("PH_PLUGIN_NAME", pluginName)
                .replace("PH_VERSION", version)
                .replace("PH_PLUGIN_CMD", "rewards");
    }

    @Override
    public String getStatus(Player player) {
        PluginSettings settings = PluginSettings.getInstance();
        return t().get("TC_REWARDS_INFO_PANEL_STATUS", player)
                .replace("PH_WALLET_STATUS", available(Wallet.isAvailable()))
                .replace("PH_DISCORD_STATUS", available(DiscordConnect.isDiscordAvailable()))
                .replace("PH_DAILY_LOGIN", String.valueOf(settings.dailyLoginEnabled))
                .replace("PH_ENEMY_NPC", String.valueOf(settings.enemyNpcKillEnabled))
                .replace("PH_AGGRESSIVE_ANIMAL", String.valueOf(settings.aggressiveAnimalKillEnabled))
                .replace("PH_LIGHTNING", String.valueOf(settings.lightningEnabled))
                .replace("PH_ORBIT", String.valueOf(settings.orbitEnabled))
                .replace("PH_HELL", String.valueOf(settings.hellEnabled))
                .replace("PH_SECTOR_DISCOVERY", String.valueOf(settings.sectorDiscoveryEnabled));
    }

    private I18n t() {
        return I18n.getInstance(plugin);
    }

    private static String available(boolean value) {
        return value ? "available" : "missing";
    }
}
