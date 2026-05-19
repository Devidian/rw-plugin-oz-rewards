package de.omegazirkel.risingworld.rewards.ui;

import java.util.ArrayList;
import java.util.Arrays;

import de.omegazirkel.risingworld.Rewards;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.BasePlayerPluginDataPanel;
import de.omegazirkel.risingworld.tools.ui.PlayerPluginData;
import de.omegazirkel.risingworld.tools.ui.table.TableCell;
import de.omegazirkel.risingworld.tools.ui.table.TableRow;
import de.omegazirkel.risingworld.tools.ui.table.TableScrollView;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UILabel;
import net.risingworld.api.ui.style.Font;
import net.risingworld.api.ui.style.TextAnchor;
import net.risingworld.api.ui.style.Unit;

public class RewardsPlayerPluginData extends PlayerPluginData {

    public RewardsPlayerPluginData(String pluginVersion) {
        this.pluginLabel = Rewards.name;
        this.pluginVersion = pluginVersion;
    }

    private I18n t() {
        return I18n.getInstance(Rewards.name);
    }

    @Override
    public BasePlayerPluginDataPanel createPlayerPluginDataUIElement(Player uiPlayer) {
        return new BasePlayerPluginDataPanel(uiPlayer, pluginLabel) {
            @Override
            protected void redrawContent() {
                flexWrapper.removeAllChilds();

                TableScrollView table = new TableScrollView(
                        Arrays.asList(
                                t().get("TC_DATA_COL_DESCRIPTION", uiPlayer),
                                "key",
                                "value"),
                        Arrays.asList(38f, 42f, 20f));
                table.setPosition(0, 0, false);
                table.style.width.set(100, Unit.Percent);
                table.setScrollBodyHeight(320);

                int playerDbId = uiPlayer.getDbID();
                addRow(table, t().get("TC_DATA_LOGIN_LAST_GMT_DATE", uiPlayer), Rewards.LOGIN_LAST_GMT_DATE_KEY,
                        stringValue(playerDbId, Rewards.LOGIN_LAST_GMT_DATE_KEY));
                addRow(table, t().get("TC_DATA_LOGIN_STREAK_COUNT", uiPlayer), Rewards.LOGIN_STREAK_COUNT_KEY,
                        intValue(playerDbId, Rewards.LOGIN_STREAK_COUNT_KEY));
                addRow(table, t().get("TC_DATA_ORBIT_REACHED_AT", uiPlayer), Rewards.ORBIT_REACHED_AT_KEY,
                        stringValue(playerDbId, Rewards.ORBIT_REACHED_AT_KEY));
                addRow(table, t().get("TC_DATA_HELL_REACHED_AT", uiPlayer), Rewards.HELL_REACHED_AT_KEY,
                        stringValue(playerDbId, Rewards.HELL_REACHED_AT_KEY));
                addRow(table, t().get("TC_LABEL_NOTIFY_LOGIN", uiPlayer), RewardsPlayerPluginSettings.NOTIFY_LOGIN_KEY,
                        booleanValue(playerDbId, RewardsPlayerPluginSettings.NOTIFY_LOGIN_KEY, true));
                addRow(table, t().get("TC_LABEL_NOTIFY_ENEMY_NPC_KILL", uiPlayer),
                        RewardsPlayerPluginSettings.NOTIFY_ENEMY_NPC_KILL_KEY,
                        booleanValue(playerDbId, RewardsPlayerPluginSettings.NOTIFY_ENEMY_NPC_KILL_KEY, true));
                addRow(table, t().get("TC_LABEL_NOTIFY_ANIMAL_KILL", uiPlayer),
                        RewardsPlayerPluginSettings.NOTIFY_ANIMAL_KILL_KEY,
                        booleanValue(playerDbId, RewardsPlayerPluginSettings.NOTIFY_ANIMAL_KILL_KEY, true));
                if (uiPlayer.isAdmin()) {
                    addRow(table, t().get("TC_LABEL_DEBUG_ENEMY_NPC_KILL", uiPlayer),
                            RewardsPlayerPluginSettings.DEBUG_ENEMY_NPC_KILL_KEY,
                            booleanValue(playerDbId, RewardsPlayerPluginSettings.DEBUG_ENEMY_NPC_KILL_KEY, false));
                }

                flexWrapper.addChild(table.getRoot());
            }

            private void addRow(TableScrollView table, String description, String key, String value) {
                table.addRow(new TableRow(new ArrayList<>(Arrays.asList(
                        cell(description, 38f),
                        cell(key, 42f),
                        cell(value, 20f)))));
            }

            private String stringValue(int playerDbId, String key) {
                if (Rewards.playerSettings == null) {
                    return "-";
                }
                return Rewards.playerSettings.getString(playerDbId, key).orElse("-");
            }

            private String intValue(int playerDbId, String key) {
                if (Rewards.playerSettings == null) {
                    return "-";
                }
                return Rewards.playerSettings.getInt(playerDbId, key).map(String::valueOf).orElse("-");
            }

            private String booleanValue(int playerDbId, String key, boolean defaultValue) {
                if (Rewards.playerSettings == null) {
                    return String.valueOf(defaultValue);
                }
                return String.valueOf(Rewards.playerSettings.getBoolean(playerDbId, key).orElse(defaultValue));
            }

            private TableCell cell(String text, float width) {
                UILabel label = new UILabel(text == null ? "" : text);
                label.setFont(Font.Default);
                label.setFontSize(13);
                label.setTextWrap(false);
                label.setTextAlign(TextAnchor.MiddleLeft);
                return new TableCell(label, width);
            }
        };
    }
}
