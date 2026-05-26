# OZ Rewards

OZ Rewards adds wallet rewards for recurring and event-based player activity in Rising World.

## Features

- Daily login reward once per GMT date
- Login streak multiplier with configurable cap
- Enemy NPC kill rewards by configurable NPC type IDs and definition names
- Aggressive and defensive-aggressive animal kill rewards
- Storm/lightning workaround reward for environment damage during thunder, storm, or hurricane weather
- First orbit visit reward when a player reaches a configurable vertical chunk
- First hell visit reward when a player reaches a configurable vertical chunk
- Configurable sector discovery rewards for first global or per-player discoveries
- Player notification toggles through the shared plugin settings overlay
- Admin-only debug toggle for discovering unrewarded NPC type IDs and definition names
- Optional Discord reward messages through OZ Discord Connect

## Dependencies

- Required: `rw-plugin-oz-tools`
- Required for useful reward deposits: `rw-plugin-oz-wallet`
- Optional: `rw-plugin-oz-discord-connect`

Wallet and Discord Connect are called by reflection. Missing Wallet disables deposits gracefully and logs a warning; missing Discord only disables Discord delivery.

## Commands

| Command | Description |
| ------- | ----------- |
| `/rewards open` | Open the plugin menu |
| `/rewards status` | Open the shared Tools Info/Status panel |
| `/rewards help` | Show command help |

The Rewards radial menu uses the shared Tools Info/Status icon for the same status panel.

## Settings

Settings are copied from `settings.default.properties` to `settings.properties` on first run.

| Key | Default | Description |
| --- | ------- | ----------- |
| `dailyLogin.enabled` | `true` | Enable daily login rewards |
| `dailyLogin.baseBonus` | `10` | Base login reward |
| `dailyLogin.factor` | `1.10` | Streak multiplier factor |
| `dailyLogin.streakLimit` | `7` | Maximum streak exponent |
| `enemyNpcKill.enabled` | `true` | Enable enemy NPC rewards |
| `enemyNpcKill.reward` | `10` | Default enemy NPC reward |
| `enemyNpcKill.typeIds` | `210,215` | Comma-separated NPC type IDs treated as rewardable enemy NPCs |
| `enemyNpcKill.definitionNames` | `bandit,skeleton` | Comma-separated case-insensitive definition-name fragments |
| `enemyNpcKill.rewardOverrides` | empty | Comma-separated `definition=amount` overrides |
| `aggressiveAnimalKill.enabled` | `true` | Enable aggressive and defensive-aggressive animal rewards |
| `aggressiveAnimalKill.reward` | `5` | Default aggressive animal reward |
| `aggressiveAnimalKill.rewardOverrides` | empty | Comma-separated `definition=amount` overrides |
| `lightning.enabled` | `true` | Enable storm environment-damage reward |
| `lightning.reward` | `250` | Lightning workaround reward |
| `lightning.messageType` | `yell` | `yell` or `chat` global announcement |
| `orbit.enabled` | `true` | Enable first orbit visit reward |
| `orbit.chunkY` | `64` | Vertical chunk threshold; rewards when player reaches this chunk or higher |
| `orbit.reward` | `5000` | First orbit visit reward |
| `hell.enabled` | `true` | Enable first hell visit reward |
| `hell.chunkY` | `-10` | Vertical chunk threshold; rewards when player reaches this chunk or lower |
| `hell.reward` | `500` | First hell visit reward |
| `sectorDiscovery.enabled` | `true` | Enable sector discovery rewards; sector `(0, 0)` is ignored |
| `sectorDiscovery.mode` | `firstOnly` | `firstOnly` rewards only the first player per sector; `perPlayer` rewards each player once per sector |
| `sectorDiscovery.baseReward` | `50` | Base multiplier for sector distance reward |
| `sectorDiscovery.firstDiscovererMultiplier` | `2.0` | Multiplier for the global first discoverer |
| `sectorDiscovery.messageType` | `yell` | `yell` or `chat` global announcement for true first discoveries |
| `discordRewardsChannelId` | `0` | Discord text channel id, `0` disables messages |
| `sendPluginWelcome` | `false` | Send welcome/help hint on spawn |
| `logLevel` | `ALL` | Plugin log level |

Daily reward formula:

```text
floor(baseBonus * factor ^ min(streakCount, streakLimit))
```

Computed positive values below `1` are deposited as `1`.

Sector discovery formula:

```text
(abs(sectorX) + abs(sectorY)) * baseReward
```

In `firstOnly` mode, only the global first discoverer receives the formula result multiplied by `firstDiscovererMultiplier`. In `perPlayer` mode, the global first discoverer receives the multiplied amount, and later players receive the base amount once for that sector.

## Player Settings

The shared player settings overlay exposes these per-player toggles, defaulting to enabled:

- `oz.rewards.notify.login`
- `oz.rewards.notify.enemyNpcKill`
- `oz.rewards.notify.animalKill`

Admins additionally see this toggle, defaulting to disabled:

- `oz.rewards.debug.enemyNpcKill`

The first orbit and first hell visit timestamps are stored as hidden player settings:

- `oz.rewards.milestone.orbit.reachedAt`
- `oz.rewards.milestone.hell.reachedAt`

## Runtime Notes

Enemy NPC detection defaults to NPC type IDs `210` and `215` plus NPC definition names containing `bandit` or `skeleton`. Verify the actual server NPC definitions and adjust `enemyNpcKill.typeIds`, `enemyNpcKill.definitionNames`, or `enemyNpcKill.rewardOverrides` if needed. Admins can enable Enemy NPC debug mode in the shared player settings overlay to show the type ID and definition name for killed NPCs that do not currently grant a reward.

The old `banditKill.*` settings and `oz.rewards.notify.banditKill` player setting key were renamed and are no longer read. Existing servers must migrate their `settings.properties` values to `enemyNpcKill.*`.

The lightning reward intentionally uses `PlayerDamageEvent.Cause.Environment` plus thunder/storm/hurricane weather checks until Rising World exposes a dedicated lightning hook.

Orbit and hell rewards are checked when a player enters a new chunk and again on spawn. The configured thresholds use Rising World's vertical chunk coordinate (`Vector3i.y`), not raw altitude.

Sector discovery rewards are checked on `PlayerEnterSectorEvent`. The plugin stores global first discoveries and per-player sector reward entries in its SQLite database. Region names are stored as `Unknown` until Rising World exposes a safe region lookup through the PluginAPI. Unreleased development databases with the old `biome` sector discovery column are recreated with the new `region` column on startup.
