package de.omegazirkel.risingworld.rewards;

import java.util.ArrayList;
import java.util.List;

import de.omegazirkel.risingworld.Rewards;
import de.omegazirkel.risingworld.tools.ui.AssetManager;
import de.omegazirkel.risingworld.tools.ui.MenuItem;
import de.omegazirkel.risingworld.tools.ui.PluginInfoStatusProviders;
import de.omegazirkel.risingworld.tools.ui.PluginMenuManager;
import net.risingworld.api.Plugin;
import net.risingworld.api.objects.Player;
import net.risingworld.api.ui.UITarget;

public class PluginGUI {
    private static PluginGUI instance = null;
    private Rewards plugin;

    private PluginGUI() {
    }

    public static PluginGUI getInstance(Plugin p) {
        AssetManager.loadIconFromPlugin(p, "oz-rewards");
        AssetManager.loadIconFromPlugin(p, "rewards-manage");
        PluginGUI gui = getInstance();
        gui.plugin = (Rewards) p;
        return gui;
    }

    public static PluginGUI getInstance() {
        if (instance == null) {
            instance = new PluginGUI();
        }
        return instance;
    }

    public void openMainMenu(Player uiPlayer) {
        List<MenuItem> menuItems = new ArrayList<>();
        menuItems.add(PluginInfoStatusProviders.menuItem("Info / Status", Rewards.name));
        if (uiPlayer.isAdmin() && Rewards.killRewardCatalog() != null) {
            menuItems.add(new MenuItem("rewards-manage", Rewards.i18n().get("tc.rewards.manage.title", uiPlayer), p -> {
                p.hideRadialMenu(false);
                p.addUIElement(new RewardManagementOverlay(plugin, p), UITarget.Modal);
            }));
        }
        menuItems.add(MenuItem.closeMenu(uiPlayer));
        PluginMenuManager.showMenu(uiPlayer, menuItems);
    }
}
