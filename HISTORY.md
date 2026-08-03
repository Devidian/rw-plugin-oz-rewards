# History / Changelog / Commitlog

<https://www.conventionalcommits.org/en/v1.0.0/>

## [0.3.8] - 2026-08-03 | Localized reward announcements

- fix: localize lightning and first sector-discovery announcements for every online recipient

## [0.3.7] - 2026-07-24 | Shared runtime bridges

- refactor: use the synchronized optional Discord bridge for configured bot-language messages
- refactor: keep the plugin entry point limited to lifecycle wiring and event delegation
- change: update the shared OZ Tools dependency to version 0.23.8

## [0.3.6] - 2026-07-24 | Ghoul rewards

- change: treat ghouls as rewardable enemy NPCs by default

## [0.3.5] - 2026-07-21 | Shared Tools update

- change: update the shared OZ Tools dependency to version 0.23.1

## [0.3.4] - 2026-07-20 | Advanced button controls

- change: update the shared OZ Tools UI dependency to the stable button controls

## [0.3.3] - 2026-07-20 | Decimal settings

- feat: expose decimal reward values in the in-game admin settings

## [0.3.2] - 2026-07-20 | Update metadata

- change: publish the canonical GitHub release source for OZ Tools update management

## [0.3.1] - 2026-07-14 | Icon set polish

- change: rename the Rewards plugin icon key to its final semantic name

## [0.3.0] - 2026-06-08 | Region-based discovery

- feat: resolve sector discovery regions through Tools `RegionHelper`
- feat: add player setting to hide the Rewards shortcut from `/ozt` and the inventory shortcut panel

## [0.2.0] - 2026-05-26 | Shared plugin status panel

- feat: use the shared Tools info icon for the Rewards radial Info/Status entry
- feat: add shared Tools Info/Status panel content for Rewards and route `/rewards status` to it
- feat: complete grouped admin settings metadata and i18n labels for Rewards settings
- refactor: route Rewards settings logging through the main `OZ.Rewards` logger

## [0.1.0] - 2026-05-19

- fix: reward defensive-aggressive animal kills such as wolves
- fix: restore colored one-line plugin welcome message
- feat: scaffold OZ Rewards from `rw-plugin-maven-template`
- feat: add daily login, bandit kill, aggressive animal kill, and storm lightning workaround rewards
- feat: integrate Wallet and Discord Connect by reflection
- feat: add player notification settings and English/German translations
- feat: add configurable bandit NPC type IDs with defaults for bandit and desert bandit
- feat: rename bandit rewards to enemy NPC rewards and add skeleton definition matching
- feat: add admin-only Enemy NPC debug setting for unrewarded NPC kills
- fix: count lightning environment damage during hurricane weather
- feat: include NPC type names and login streaks in Wallet reward reasons
- fix: close the rewards radial menu before showing status from the menu
- chore: update default reward values for bandit, animal, login streak factor, and lightning
- feat: add configurable one-time orbit and hell visit rewards with global chat announcements
- feat: add configurable sector discovery rewards with first-only and per-player modes
- initial development version
- fix: rename sector discovery metadata from biome to region and recreate the unreleased SQLite discovery table shape
