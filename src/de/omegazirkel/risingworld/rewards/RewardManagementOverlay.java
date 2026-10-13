package de.omegazirkel.risingworld.rewards;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import de.omegazirkel.risingworld.Rewards;
import de.omegazirkel.risingworld.OZTools;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.ui.AdvancedButton;
import de.omegazirkel.risingworld.tools.ui.AdvancedButtonFactory;
import de.omegazirkel.risingworld.tools.ui.BasePluginOverlay;
import de.omegazirkel.risingworld.tools.ui.table.TableCell;
import de.omegazirkel.risingworld.tools.ui.table.TableRow;
import de.omegazirkel.risingworld.tools.ui.table.TableScrollView;
import net.risingworld.api.definitions.Definitions;
import net.risingworld.api.definitions.Npcs;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UILabel;
import net.risingworld.api.ui.UITarget;
import net.risingworld.api.ui.style.Pivot;

public final class RewardManagementOverlay extends BasePluginOverlay {
    private final Rewards plugin;

    public RewardManagementOverlay(Rewards plugin, Player player) {
        super(player, p -> { });
        this.plugin = plugin;
        rebuild();
    }

    @Override protected I18n t() { return Rewards.i18n(); }
    @Override protected String titleText() { return t().get("tc.rewards.manage.title", uiPlayer); }
    @Override protected String descriptionText() { return t().get("tc.rewards.manage.description", uiPlayer); }
    @Override protected String legendText() { return ""; }

    @Override protected void rebuild() {
        super.rebuild();
        TableScrollView table = new TableScrollView(Arrays.asList(
                t().get("tc.rewards.manage.name", uiPlayer),
                t().get("tc.rewards.manage.id", uiPlayer),
                t().get("tc.rewards.manage.reward", uiPlayer),
                t().get("tc.rewards.manage.action", uiPlayer)),
                Arrays.asList(48f, 15f, 20f, 17f));
        table.setScrollBodyHeight(390f);
        Npcs.NpcDefinition[] definitions = Definitions.getAllNpcDefinitions();
        if (definitions != null) Arrays.stream(definitions).filter(value -> value != null)
                .sorted(Comparator.comparingInt(value -> value.id)).forEach(value -> {
                    String name = value.getLocalizedName(uiPlayer.getLanguage());
                    if (name == null || name.isBlank()) name = value.name;
                    UILabel nameLabel = new UILabel(name == null ? "#" + value.id : name);
                    UILabel idLabel = new UILabel(Short.toString(value.id));
                    UILabel rewardLabel = new UILabel(Long.toString(Rewards.killRewardCatalog().reward(value.id)));
                    AdvancedButton edit = AdvancedButtonFactory.defaultButton("✎", event -> edit(value));
                    edit.setPivot(Pivot.UpperLeft);
                    edit.setSize(38, 25, false);
                    table.addRow(new TableRow(Arrays.asList(new TableCell(nameLabel, 48f),
                            new TableCell(idLabel, 15f), new TableCell(rewardLabel, 20f),
                            new TableCell(edit, 17f))));
                });
        body.addChild(table);
    }

    private void edit(Npcs.NpcDefinition definition) {
        if (!uiPlayer.isAdmin() || Rewards.killRewardCatalog() == null) return;
        uiPlayer.removeUIElement(this);
        uiPlayer.closeAllActiveUIWindows();
        OZTools.runAfterModalClose(uiPlayer, () -> uiPlayer.showInputMessageBox(t().get("tc.rewards.manage.title", uiPlayer),
                t().get("tc.rewards.manage.prompt", uiPlayer),
                Long.toString(Rewards.killRewardCatalog().reward(definition.id)), answer -> plugin.enqueue(() -> {
                    if (!uiPlayer.isConnected() || !uiPlayer.isAdmin()) return;
                    if (answer != null) {
                        try {
                            Rewards.killRewardCatalog().set(definition.id, Long.parseLong(answer.trim()));
                        } catch (NumberFormatException | IOException ex) {
                            uiPlayer.sendTextMessage(t().get("tc.rewards.manage.invalid", uiPlayer));
                        }
                    }
                    rebuild();
                    uiPlayer.addUIElement(this, UITarget.Modal);
                })));
    }
}
