# History / Changelog / Commitlog

<https://www.conventionalcommits.org/en/v1.0.0/>

## [unreleased]

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
