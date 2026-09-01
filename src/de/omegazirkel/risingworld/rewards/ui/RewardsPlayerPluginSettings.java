package de.omegazirkel.risingworld.rewards.ui;

import de.omegazirkel.risingworld.Rewards;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.BasePlayerPluginSettingsPanel;
import de.omegazirkel.risingworld.tools.ui.OZUIElement;
import de.omegazirkel.risingworld.tools.ui.PlayerPluginSettings;
import de.omegazirkel.risingworld.tools.ui.PluginShortcutVisibility;
import net.risingworld.api.objects.Player;

public class RewardsPlayerPluginSettings extends PlayerPluginSettings {
    public static final String NOTIFY_LOGIN_KEY = "oz.rewards.notify.login";
    public static final String NOTIFY_ENEMY_NPC_KILL_KEY = "oz.rewards.notify.enemyNpcKill";
    public static final String NOTIFY_ANIMAL_KILL_KEY = "oz.rewards.notify.animalKill";
    public static final String DEBUG_ENEMY_NPC_KILL_KEY = "oz.rewards.debug.enemyNpcKill";

    public RewardsPlayerPluginSettings(String pluginVersion) {
        this.pluginLabel = Rewards.name;
        this.pluginVersion = pluginVersion;
    }

    private I18n t() {
        return I18n.getInstance(Rewards.name);
    }

    @Override
    public BasePlayerPluginSettingsPanel createPlayerPluginSettingsUIElement(Player uiPlayer) {
        return new BasePlayerPluginSettingsPanel(uiPlayer, pluginLabel) {
            @Override
            protected void redrawContent() {
                flexWrapper.removeAllChilds();
                flexWrapper.addChild(booleanSetting(uiPlayer, shortcutKey(), "tc.label.rewards.shortcut"));
                flexWrapper.addChild(booleanSetting(uiPlayer, NOTIFY_LOGIN_KEY, "tc.label.notify.login"));
                flexWrapper.addChild(booleanSetting(uiPlayer, NOTIFY_ENEMY_NPC_KILL_KEY,
                        "tc.label.notify.enemy.npc.kill"));
                flexWrapper.addChild(booleanSetting(uiPlayer, NOTIFY_ANIMAL_KILL_KEY, "tc.label.notify.animal.kill"));
                if (uiPlayer.isAdmin()) {
                    flexWrapper.addChild(booleanSetting(uiPlayer, DEBUG_ENEMY_NPC_KILL_KEY,
                            "tc.label.debug.enemy.npc.kill", false));
                }
            }

            protected OZUIElement booleanSetting(Player uiPlayer, String key, String labelKey) {
                return booleanSetting(uiPlayer, key, labelKey, true);
            }

            protected OZUIElement booleanSetting(Player uiPlayer, String key, String labelKey, boolean defaultValue) {
                OZUIElement element = defaultSettingsContainer();
                element.addChild(defaultSettingsLabel(t().get(labelKey, uiPlayer)));

                boolean currentValue = Rewards.playerSettings == null
                        ? defaultValue
                        : Rewards.playerSettings.getBoolean(uiPlayer.getDbID(), key).orElse(defaultValue);
                element.addChild(switchButtons(uiPlayer, currentValue, event -> {
                    boolean nextValue = !(Rewards.playerSettings == null
                            ? defaultValue
                            : Rewards.playerSettings.getBoolean(uiPlayer.getDbID(), key).orElse(defaultValue));
                    if (Rewards.playerSettings != null) {
                        Rewards.playerSettings.setBoolean(uiPlayer.getDbID(), key, nextValue);
                    }
                    redrawContent();
                }, t().get("tc.btn.off", uiPlayer), t().get("tc.btn.on", uiPlayer)));
                return element;
            }
        };
    }

    public static boolean shortcutVisible(Player player) {
        return Rewards.playerSettings == null
                || Rewards.playerSettings.getBoolean(player.getDbID(), shortcutKey()).orElse(true);
    }

    private static String shortcutKey() {
        return PluginShortcutVisibility.playerSettingKey(Rewards.name);
    }
}
