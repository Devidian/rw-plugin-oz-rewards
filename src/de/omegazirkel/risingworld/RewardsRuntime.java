package de.omegazirkel.risingworld;

import java.nio.file.Path;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;

import de.omegazirkel.risingworld.rewards.DiscordConnect;
import de.omegazirkel.risingworld.rewards.PluginGUI;
import de.omegazirkel.risingworld.rewards.KillRewardCatalog;
import de.omegazirkel.risingworld.rewards.PluginSettings;
import de.omegazirkel.risingworld.rewards.RewardsPluginInfoStatusProvider;
import de.omegazirkel.risingworld.rewards.SectorDiscoveryStore;
import de.omegazirkel.risingworld.rewards.SectorDiscoveryStore.RewardClaim;
import de.omegazirkel.risingworld.rewards.SectorDiscoveryStore.RewardTerms;
import de.omegazirkel.risingworld.rewards.SectorRewardPayout;
import de.omegazirkel.risingworld.rewards.Wallet;
import de.omegazirkel.risingworld.rewards.ui.RewardsPlayerPluginData;
import de.omegazirkel.risingworld.rewards.ui.RewardsPlayerPluginSettings;
import de.omegazirkel.risingworld.tools.Colors;
import de.omegazirkel.risingworld.tools.I18n;
import de.omegazirkel.risingworld.tools.OZLogger;
import de.omegazirkel.risingworld.tools.PlayerSettings;
import de.omegazirkel.risingworld.tools.db.SQLiteConnectionFactory;
import de.omegazirkel.risingworld.tools.settings.PlayerPluginAdminSettings;
import de.omegazirkel.risingworld.tools.ui.AssetManager;
import de.omegazirkel.risingworld.tools.ui.MenuItem;
import de.omegazirkel.risingworld.tools.ui.PlayerPluginSettingsOverlay;
import de.omegazirkel.risingworld.tools.ui.PluginInfoStatusProviders;
import de.omegazirkel.risingworld.tools.ui.PluginMenuManager;
import de.omegazirkel.risingworld.tools.ui.PluginShortcutVisibility;
import de.omegazirkel.risingworld.tools.privacy.PlayerErasureService;
import net.risingworld.api.Plugin;
import net.risingworld.api.Server;
import net.risingworld.api.World;
import net.risingworld.api.definitions.Npcs;
import net.risingworld.api.definitions.Npcs.Behaviour;
import net.risingworld.api.events.npc.NpcDeathEvent;
import net.risingworld.api.events.player.PlayerCommandEvent;
import net.risingworld.api.events.player.PlayerEnterBiomeEvent;
import net.risingworld.api.events.player.PlayerEnterChunkEvent;
import net.risingworld.api.events.player.PlayerEnterSectorEvent;
import net.risingworld.api.events.player.PlayerSpawnEvent;
import net.risingworld.api.events.world.LightningStrikeEvent;
import net.risingworld.api.objects.Npc;
import net.risingworld.api.objects.Player;
import net.risingworld.api.utils.Vector2i;
import net.risingworld.api.utils.Vector3i;

class RewardsRuntime extends Plugin {
    private PlayerErasureService.Registration erasureRegistration;
    static final String PLUGIN_IDENTIFIER = "OZ - Rewards";
    static final String COMMAND = "rewards";
    static final String LOGIN_LAST_GMT_DATE_KEY = "oz.rewards.login.lastGmtDate";
    static final String LOGIN_STREAK_COUNT_KEY = "oz.rewards.login.streakCount";
    static final String ORBIT_REACHED_AT_KEY = "oz.rewards.milestone.orbit.reachedAt";
    static final String HELL_REACHED_AT_KEY = "oz.rewards.milestone.hell.reachedAt";

    static final Colors c = Colors.getInstance();
    private static I18n t = null;
    private static PluginSettings s = null;
    private static PluginGUI gui;
    private static Connection sqliteCon;
    private static SectorDiscoveryStore sectorDiscoveryStore;

    /** Called by the local erasure coordinator before shared player settings are removed. */
    public synchronized boolean erasePlayerData(int playerDbId) {
        if (sectorDiscoveryStore == null || playerSettings == null || !sectorDiscoveryStore.erasePlayerData(playerDbId)) return false;
        playerSettings.erasePlayer(playerDbId);
        return true;
    }
    private static KillRewardCatalog killRewardCatalog;
    public static PlayerSettings playerSettings;
    public static String name;

    public static OZLogger logger() {
        return OZLogger.getInstance("OZ.Rewards");
    }

    public static I18n i18n() {
        return t;
    }

    @Override
    public void onEnable() {
        erasureRegistration = PlayerErasureService.register("rewards", request -> erasePlayerData(request.playerDbId()));
        name = this.getDescription("name");
        s = PluginSettings.getInstance((Rewards) this);
        t = I18n.getInstance(this);
        s.initSettings();
        try {
            killRewardCatalog = new KillRewardCatalog(getPath(), s);
        } catch (IOException | RuntimeException ex) {
            killRewardCatalog = null;
            logger().error("Kill rewards are disabled: " + ex.getMessage());
        }

        try {
            sqliteCon = SQLiteConnectionFactory.open(this);
            playerSettings = new PlayerSettings(sqliteCon);
            sectorDiscoveryStore = new SectorDiscoveryStore(sqliteCon);
        } catch (RuntimeException ex) {
            logger().error("Failed to initialize Rewards player settings database: " + ex.getMessage());
            ex.printStackTrace();
        }

        gui = PluginGUI.getInstance(this);
        PluginMenuManager.registerPluginMenu(new MenuItem(name, "oz-rewards", "Rewards", p -> {
            p.hideRadialMenu(true);
            gui.openMainMenu(p);
        }));
        PluginShortcutVisibility.register(name, RewardsPlayerPluginSettings::shortcutVisible);

        Wallet.init(this);
        DiscordConnect.init(this);
        PlayerPluginSettingsOverlay.registerPlayerPluginSettings(new RewardsPlayerPluginSettings(getDescription("version")));
        PlayerPluginSettingsOverlay.registerPlayerPluginData(new RewardsPlayerPluginData(getDescription("version")));
        PlayerPluginSettingsOverlay.registerPlayerPluginAdminSettings(
                new PlayerPluginAdminSettings(name, getDescription("version"), () -> s.adminSettingsEntries(),
                        s::initSettings));
        PluginInfoStatusProviders
                .registerProvider(new RewardsPluginInfoStatusProvider((Rewards) this, getDescription("version")));
        logger().info(this.getName() + " Plugin is enabled version:" + this.getDescription("version"));
    }

    @Override
    public void onDisable() {
        if (erasureRegistration != null) erasureRegistration.close();
        if (name != null) {
            PluginShortcutVisibility.unregister(name);
            PluginInfoStatusProviders.unregisterProvider(name);
        }
        if (sqliteCon != null) {
            try {
                sqliteCon.close();
            } catch (SQLException ex) {
                logger().error("Failed to close Rewards database connection: " + ex.getMessage());
            }
        }
    }

    public void onSettingsChanged(Path settingsPath) {
        s.initSettings(settingsPath.toString());
    }

    public void onPlayerCommand(PlayerCommandEvent event) {
        Player player = event.getPlayer();
        String[] cmdParts = event.getCommand().split(" ", 2);
        if (!cmdParts[0].equals("/" + COMMAND)) {
            return;
        }

        if (cmdParts.length < 2 || cmdParts[1].equals("open")) {
            gui.openMainMenu(player);
            return;
        }

        switch (cmdParts[1]) {
            case "status":
                PluginInfoStatusProviders.show(player, name);
                break;
            case "help":
                player.sendTextMessage(t.get("tc.cmd.help", player).replace("PH_PLUGIN_CMD", COMMAND));
                break;
            default:
                player.sendTextMessage(t.get("tc.err.cmd.unknown", player).replace("PH_PLUGIN_CMD", COMMAND));
                break;
        }
    }

    public void onPlayerSpawnEvent(PlayerSpawnEvent event) {
        Player player = event.getPlayer();
        if (s.sendPluginWelcome) {
            player.sendTextMessage(t.get("tc.msg.plugin.welcome", player)
                    .replace("PH_PLUGIN_NAME", getDescription("name"))
                    .replace("PH_PLUGIN_CMD", COMMAND)
                    .replace("PH_PLUGIN_VERSION", getDescription("version")));
        }
        resumeSectorRewards(player, null);
        grantDailyLoginReward(player);
        grantLocationMilestoneRewards(player, player.getChunkPosition());
    }

    public void onPlayerEnterChunk(PlayerEnterChunkEvent event) {
        grantLocationMilestoneRewards(event.getPlayer(), event.getNewChunkCoordinates());
    }

    public void onPlayerEnterSector(PlayerEnterSectorEvent event) {
        grantSectorDiscoveryReward(event.getPlayer(), event.getNewSectorCoordinates());
    }

    public void onPlayerEnterBiome(PlayerEnterBiomeEvent event) {
        Player player = event.getPlayer();
        grantSectorDiscoveryReward(player, player.getSectorPosition());
    }

    public void onNpcDeath(NpcDeathEvent event) {
        if (event.getCause() != NpcDeathEvent.Cause.KilledByPlayer || !(event.getKiller() instanceof Player player)) {
            return;
        }

        Npc npc = event.getNpc();
        if (npc == null || npc.getDefinition() == null) {
            return;
        }

        String definitionName = npc.getDefinition().name == null ? "" : npc.getDefinition().name;
        long reward = killRewardCatalog == null ? 0L : killRewardCatalog.reward(npc.getTypeID());
        if (npc.getDefinition().type != Npcs.Type.Animal && s.enemyNpcKillEnabled && reward != 0L) {
            grantNpcReward(player, npc, definitionName, reward, "tc.msg.enemy.npc.reward",
                    "tc.discord.enemy.npc.reward", RewardsPlayerPluginSettings.NOTIFY_ENEMY_NPC_KILL_KEY);
            return;
        }

        if (s.aggressiveAnimalKillEnabled && npc.getDefinition().type == Npcs.Type.Animal && reward != 0L) {
            grantNpcReward(player, npc, definitionName, reward, "tc.msg.animal.reward", "tc.discord.animal.reward",
                    RewardsPlayerPluginSettings.NOTIFY_ANIMAL_KILL_KEY);
            return;
        }

        sendUnrewardedNpcDebugMessage(player, npc, definitionName);
    }

    public void onLightningStrike(LightningStrikeEvent event) {
        if (!s.lightningEnabled || !(event.getTarget() instanceof Player player)) {
            return;
        }
        long reward = s.lightningReward;
        if (!depositReward(player, reward, t.get("tc.reason.lightning", player))) {
            return;
        }

        for (Player recipient : Server.getAllPlayers()) {
            String message = t.get("tc.msg.lightning.reward", recipient)
                    .replace("PH_PLAYER", player.getName())
                    .replace("PH_AMOUNT", Long.toString(reward));
            if ("chat".equalsIgnoreCase(s.lightningMessageType)) {
                recipient.sendTextMessage(message);
            } else {
                recipient.sendYellMessage(message, 5, true);
            }
        }
        sendDiscord("tc.discord.lightning.reward", player, reward);
    }

    private void grantDailyLoginReward(Player player) {
        if (!s.dailyLoginEnabled || playerSettings == null) {
            return;
        }

        int playerDbId = player.getDbID();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        String todayString = today.toString();
        String lastDateString = playerSettings.getString(playerDbId, LOGIN_LAST_GMT_DATE_KEY).orElse("");
        if (todayString.equals(lastDateString)) {
            return;
        }

        int previousStreak = playerSettings.getInt(playerDbId, LOGIN_STREAK_COUNT_KEY).orElse(0);
        int nextStreak = 1;
        if (!lastDateString.isBlank()) {
            try {
                LocalDate lastDate = LocalDate.parse(lastDateString);
                nextStreak = lastDate.plusDays(1).equals(today) ? previousStreak + 1 : 1;
            } catch (RuntimeException ex) {
                logger().warn("Invalid stored login reward date for player " + playerDbId + ": " + lastDateString);
            }
        }

        long reward = calculateDailyReward(nextStreak);
        if (!depositReward(player, reward, dailyLoginWalletReason(player, nextStreak))) {
            return;
        }

        playerSettings.setString(playerDbId, LOGIN_LAST_GMT_DATE_KEY, todayString);
        playerSettings.setInt(playerDbId, LOGIN_STREAK_COUNT_KEY, nextStreak);

        if (isPlayerNotificationEnabled(player, RewardsPlayerPluginSettings.NOTIFY_LOGIN_KEY)) {
            player.sendTextMessage(t.get("tc.msg.login.reward", player)
                    .replace("PH_AMOUNT", Long.toString(reward))
                    .replace("PH_STREAK", Integer.toString(nextStreak)));
        }
        sendDiscord("tc.discord.login.reward", player, reward);
    }

    private void grantNpcReward(Player player, Npc npc, String definitionName, long reward, String playerMessageKey,
            String discordMessageKey, String notificationKey) {
        String reason = t.get("tc.reason.npc.kill", player)
                .replace("PH_NPC_TYPE", npcTypeName(npc, definitionName));
        long applied = reward;
        if (reward < 0) {
            applied = -Math.min(Math.max(0L, Wallet.balanceDefault(player.getDbID())),
                    reward == Long.MIN_VALUE ? Long.MAX_VALUE : -reward);
            if (applied == 0 || !Wallet.isSuccess(Wallet.withdrawDefault(player.getDbID(), -applied, reason, PLUGIN_IDENTIFIER))) return;
        } else if (!depositReward(player, reward, reason)) return;

        if (isPlayerNotificationEnabled(player, notificationKey)) {
            player.sendTextMessage(t.get(reward < 0 ? "tc.msg.npc.penalty" : playerMessageKey, player)
                    .replace("PH_AMOUNT", Long.toString(Math.abs(applied))));
        }
        if (reward > 0) sendDiscord(discordMessageKey, player, reward);
    }

    static KillRewardCatalog killRewardCatalog() { return killRewardCatalog; }

    private void grantLocationMilestoneRewards(Player player, Vector3i chunkPosition) {
        if (chunkPosition == null) {
            return;
        }

        grantLocationMilestoneReward(player, chunkPosition.y, s.orbitEnabled, true, s.orbitChunkY, s.orbitReward,
                ORBIT_REACHED_AT_KEY, "tc.reason.orbit", "tc.msg.orbit.reward", "tc.discord.orbit.reward");
        grantLocationMilestoneReward(player, chunkPosition.y, s.hellEnabled, false, s.hellChunkY, s.hellReward,
                HELL_REACHED_AT_KEY, "tc.reason.hell", "tc.msg.hell.reward", "tc.discord.hell.reward");
    }

    private void grantLocationMilestoneReward(Player player, int chunkY, boolean enabled, boolean minimumThreshold,
            int thresholdChunkY, long reward, String reachedAtKey, String reasonKey, String messageKey,
            String discordMessageKey) {
        if (!enabled || playerSettings == null) {
            return;
        }

        boolean reached = minimumThreshold ? chunkY >= thresholdChunkY : chunkY <= thresholdChunkY;
        if (!reached || playerSettings.getString(player.getDbID(), reachedAtKey).isPresent()) {
            return;
        }

        if (!depositReward(player, reward, t.get(reasonKey, player))) {
            return;
        }

        playerSettings.setString(player.getDbID(), reachedAtKey, Instant.now().toString());
        Server.broadcastTextMessage(t.get(messageKey, player)
                .replace("PH_PLAYER", player.getName())
                .replace("PH_AMOUNT", Long.toString(reward)));
        sendDiscord(discordMessageKey, player, reward);
    }

    private synchronized void grantSectorDiscoveryReward(Player player, Vector2i sector) {
        if (!s.sectorDiscoveryEnabled || sectorDiscoveryStore == null || sector == null || sector.equals(0, 0)) {
            return;
        }

        int sectorX = sector.x;
        int sectorY = sector.y;
        long baseReward = calculateSectorDiscoveryBaseReward(sectorX, sectorY);
        try {
            sectorDiscoveryStore.recordDiscoveryClaim(sectorX, sectorY, player.getDbID(),
                    "firstOnly".equalsIgnoreCase(s.sectorDiscoveryMode),
                    new RewardTerms(multiplyReward(baseReward, s.sectorDiscoveryFirstDiscovererMultiplier),
                            sectorDiscoveryReason(player, sectorX, sectorY, true)),
                    new RewardTerms(baseReward, sectorDiscoveryReason(player, sectorX, sectorY, false)));
        } catch (SQLException ex) {
            logger().error("Could not save sector discovery: " + ex.getMessage());
            return;
        }
        resumeSectorRewards(player, sector);
        scheduleSectorAnnouncement(player, sectorX, sectorY);
    }

    private synchronized void resumeSectorRewards(Player player, Vector2i sector) {
        if (!s.sectorDiscoveryEnabled || sectorDiscoveryStore == null) return;
        try {
            for (RewardClaim claim : SectorRewardPayout.payPending(sectorDiscoveryStore, player.getDbID(),
                    Wallet.isAvailable(), pending -> Wallet.depositDefault(pending.playerDbId(), pending.amount(),
                            pending.reason() + " [" + pending.id() + "]", PLUGIN_IDENTIFIER))) {
                boolean inDiscoveredSector = sector != null && sector.equals(claim.sectorX(), claim.sectorY());
                if (inDiscoveredSector && claim.firstDiscovery()
                        && sectorDiscoveryStore.pendingAnnouncement(claim.sectorX(), claim.sectorY()) != null) {
                    continue;
                }
                if (inDiscoveredSector && !claim.firstDiscovery()) {
                    executeDelayed(Math.max(0.1f, s.sectorDiscoveryRegionDelaySeconds), () -> {
                        if (player.isConnected()) sendSectorRewardNotice(player, claim,
                                player.getSectorPosition().equals(claim.sectorX(), claim.sectorY())
                                        ? localizedRegion(player, sectorRegion(player)) : null);
                    });
                } else {
                    sendSectorRewardNotice(player, claim, null);
                }
            }
        } catch (SQLException ex) {
            logger().error("Could not load pending sector rewards: " + ex.getMessage());
        }
    }

    private void sendSectorRewardNotice(Player player, RewardClaim claim, String region) {
        String key = region == null ? "tc.msg.sector.personal.payout" : "tc.msg.sector.personal.discovery";
        player.sendTextMessage(t.get(key, player)
                .replace("PH_AMOUNT", Long.toString(claim.amount()))
                .replace("PH_SECTOR_X", Integer.toString(claim.sectorX()))
                .replace("PH_SECTOR_Y", Integer.toString(claim.sectorY()))
                .replace("PH_REGION", region == null ? "" : region));
    }

    private void scheduleSectorAnnouncement(Player observer, int sectorX, int sectorY) {
        SectorDiscoveryStore.PendingAnnouncement pending = sectorDiscoveryStore.pendingAnnouncement(sectorX, sectorY);
        if (pending == null) return;
        long elapsed = Duration.between(pending.discoveredAt(), Instant.now()).toMillis();
        float remaining = Math.max(0.1f, (s.sectorDiscoveryRegionDelaySeconds * 1000L - elapsed) / 1000f);
        executeDelayed(remaining, () -> {
            if (!observer.isConnected() || !observer.getSectorPosition().equals(sectorX, sectorY)) return;
            SectorDiscoveryStore.PendingAnnouncement current = sectorDiscoveryStore.pendingAnnouncement(sectorX, sectorY);
            if (current == null || current.firstPlayerDbId() != pending.firstPlayerDbId()) return;
            String region = sectorRegion(observer);
            if ("Unknown".equalsIgnoreCase(region)
                    || !sectorDiscoveryStore.markRegionAnnounced(sectorX, sectorY, region)) return;
            String firstName = Server.getLastKnownPlayerName(pending.firstPlayerDbId());
            if (firstName == null || firstName.isBlank()) firstName = "#" + pending.firstPlayerDbId();
            for (Player recipient : Server.getAllPlayers()) {
                String key = current.reward() > 0 ? "tc.msg.sector.first.discovery" : "tc.msg.sector.first.discovery_unpaid";
                String message = t.get(key, recipient)
                        .replace("PH_PLAYER", firstName)
                        .replace("PH_AMOUNT", Long.toString(current.reward()))
                        .replace("PH_SECTOR_X", Integer.toString(sectorX))
                        .replace("PH_SECTOR_Y", Integer.toString(sectorY))
                        .replace("PH_REGION", localizedRegion(recipient, region));
                if ("chat".equalsIgnoreCase(s.sectorDiscoveryMessageType)) recipient.sendTextMessage(message);
                else recipient.sendYellMessage(message, 5, true);
            }
            Player firstPlayer = Server.getPlayerByDbID(pending.firstPlayerDbId());
            if (firstPlayer != null) sendSectorDiscoveryDiscord(firstPlayer, current.reward(), sectorX, sectorY, region);
        });
    }

    private long calculateDailyReward(int streakCount) {
        int exponent = Math.min(streakCount, s.dailyLoginStreakLimit);
        double raw = s.dailyLoginBaseBonus * Math.pow(s.dailyLoginFactor, exponent);
        double computed = Math.floor(raw);
        if (raw > 0 && computed < 1) {
            return 1L;
        }
        return Math.max(0L, (long) computed);
    }

    private long calculateSectorDiscoveryBaseReward(int sectorX, int sectorY) {
        long coordinateCap = Math.max(0L, s.sectorDiscoveryCoordinateCap);
        long distance = Math.min(Math.abs((long) sectorX), coordinateCap)
                + Math.min(Math.abs((long) sectorY), coordinateCap);
        return Math.max(0L, distance * s.sectorDiscoveryBaseReward);
    }

    private long multiplyReward(long baseReward, double multiplier) {
        double raw = baseReward * multiplier;
        double computed = Math.floor(raw);
        if (raw > 0 && computed < 1) {
            return 1L;
        }
        return Math.max(0L, (long) computed);
    }

    private boolean depositReward(Player player, long amount, String reason) {
        if (amount <= 0) {
            return false;
        }
        if (!Wallet.isAvailable()) {
            logger().warn("OZ - Wallet is not available. Skipping reward for player " + player.getDbID() + ".");
            return false;
        }

        de.omegazirkel.risingworld.tools.bridge.WalletBridge.WalletCallResult result = Wallet.depositDefault(
                player.getDbID(), amount, reason, PLUGIN_IDENTIFIER);
        if (!Wallet.isSuccess(result)) {
            logger().warn("Reward deposit failed for player " + player.getDbID() + ": " + Wallet.message(result));
            return false;
        }
        return true;
    }

    private String dailyLoginWalletReason(Player player, int streakCount) {
        if (streakCount <= 1) {
            return t.get("tc.reason.daily.login", player);
        }
        return t.get("tc.reason.daily.login.streak", player)
                .replace("PH_STREAK", Integer.toString(streakCount));
    }

    private String sectorDiscoveryReason(Player player, int sectorX, int sectorY, boolean firstDiscoverer) {
        String reasonKey = firstDiscoverer ? "tc.reason.sector.discovery.pending_first"
                : "tc.reason.sector.discovery.pending";
        return t.get(reasonKey, player)
                .replace("PH_SECTOR_X", Integer.toString(sectorX))
                .replace("PH_SECTOR_Y", Integer.toString(sectorY));
    }

    private String sectorRegion(Player player) {
        try {
            var region = player.getPosition() == null ? null : World.getRegion(player.getPosition());
            return region == null || region.name == null || region.name.isBlank()
                    ? "Unknown"
                    : region.name;
        } catch (RuntimeException ex) {
            logger().warn("Could not resolve sector discovery region: " + ex.getMessage());
            return "Unknown";
        }
    }

    private String localizedRegion(Player player, String region) {
        if (region == null || region.isBlank() || "Unknown".equalsIgnoreCase(region)) {
            return t.get("tc.region.unknown", player);
        }
        return region;
    }

    private String npcTypeName(Npc npc, String definitionName) {
        if (definitionName == null || definitionName.isBlank()) {
            return "NPC " + npc.getTypeID();
        }
        String[] words = definitionName.replace('_', ' ').replace('-', ' ').trim().split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                result.append(word.substring(1).toLowerCase());
            }
        }
        return result.length() == 0 ? "NPC " + npc.getTypeID() : result.toString();
    }

    private boolean isEnemyNpc(Npc npc, String definitionName) {
        if (s.enemyNpcTypeIds.contains((int) npc.getTypeID())) {
            return true;
        }
        String normalized = definitionName.toLowerCase();
        return s.enemyNpcDefinitionNames.stream().anyMatch(normalized::contains);
    }

    private boolean isRewardableAnimal(Npc npc) {
        if (npc.getDefinition().type != Npcs.Type.Animal) {
            return false;
        }
        Behaviour behaviour = npc.getDefinition().behaviour;
        return behaviour == Behaviour.Aggressive || behaviour == Behaviour.DefensiveAggressive;
    }

    private boolean isPlayerNotificationEnabled(Player player, String key) {
        if (playerSettings == null) {
            return true;
        }
        return playerSettings.getBoolean(player.getDbID(), key).orElse(true);
    }

    private boolean isPlayerSettingEnabled(Player player, String key, boolean defaultValue) {
        if (playerSettings == null) {
            return defaultValue;
        }
        return playerSettings.getBoolean(player.getDbID(), key).orElse(defaultValue);
    }

    private void sendUnrewardedNpcDebugMessage(Player player, Npc npc, String definitionName) {
        if (!player.isAdmin()
                || !isPlayerSettingEnabled(player, RewardsPlayerPluginSettings.DEBUG_ENEMY_NPC_KILL_KEY, false)) {
            return;
        }
        player.sendTextMessage(t.get("tc.msg.enemy.npc.debug.no.reward", player)
                .replace("PH_TYPE_ID", Long.toString(npc.getTypeID()))
                .replace("PH_DEFINITION_NAME", definitionName.isBlank() ? "<empty>" : definitionName));
    }

    private void sendDiscord(String messageKey, Player player, long amount) {
        if (s.discordRewardsChannelId == 0 || !DiscordConnect.isDiscordAvailable()) {
            return;
        }
        String lang = DiscordConnect.botLang();
        if (lang == null || lang.isBlank()) {
            lang = "en";
        }
        String message = t.get(messageKey, lang)
                .replace("PH_PLAYER", discordPlayerLabel(player))
                .replace("PH_AMOUNT", Long.toString(amount));
        DiscordConnect.sendDiscordMessage(message, s.discordRewardsChannelId);
    }

    private void sendSectorDiscoveryDiscord(Player player, long amount, int sectorX, int sectorY, String region) {
        if (s.discordRewardsChannelId == 0 || !DiscordConnect.isDiscordAvailable()) {
            return;
        }
        String lang = DiscordConnect.botLang();
        if (lang == null || lang.isBlank()) {
            lang = "en";
        }
        String localizedRegion = "Unknown".equalsIgnoreCase(region) ? t.get("tc.region.unknown", lang) : region;
        String key = amount > 0 ? "tc.discord.sector.first.discovery" : "tc.discord.sector.first.discovery_unpaid";
        String message = t.get(key, lang)
                .replace("PH_PLAYER", discordPlayerLabel(player))
                .replace("PH_AMOUNT", Long.toString(amount))
                .replace("PH_SECTOR_X", Integer.toString(sectorX))
                .replace("PH_SECTOR_Y", Integer.toString(sectorY))
                .replace("PH_REGION", localizedRegion);
        DiscordConnect.sendDiscordMessage(message, s.discordRewardsChannelId);
    }

    private static String discordPlayerLabel(Player player) {
        return de.omegazirkel.risingworld.tools.ExternalDataConsent.isGranted(player,
                de.omegazirkel.risingworld.tools.ExternalDataConsent.Purpose.DISCORD)
                ? player.getName() : "Spieler #" + player.getDbID();
    }
}
