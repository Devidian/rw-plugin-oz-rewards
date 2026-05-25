# Roadmap Plan 02 Plugin Standardization

## Objective
Adopt Roadmap Plan 02 portfolio standards for logger naming, admin settings visibility, localized settings text, and standardized plugin info/status panels.

## Ownership
Primary repository: `rw-plugin-oz-rewards`.

Supporting repositories:
- `rw-plugin-oz-tools`
- `rw-plugin-oz-wallet` for reward deposits.
- `rw-plugin-oz-discord-connect` for optional announcements.

## Work Packages
- [x] Package 1: Collapse specialized loggers into one main Rewards logger.
- [x] Package 2: Verify every safe `settings.default.properties` key appears in the admin `PluginSettings` tab.
- [x] Package 3: Mark list/enum settings as read-only where editing is not yet supported.
- [x] Package 4: Add missing English and German i18n labels/descriptions for settings.
- [x] Package 5: Group related settings with labels such as general settings, login rewards, kill rewards, survival rewards, Wallet, and Discord announcements.
- [x] Package 6: Add Rewards info/status panel content and redirect existing info/status commands to the shared Tools panel.

## Validation Strategy
- Run Maven package and tests.
- Verify Wallet and optional Discord integration statuses are represented clearly.
- Verify info/status panel opens from radial menu and commands.

## Progress Notes
- Package 1 is complete: Rewards settings logging now routes through the main `OZ.Rewards` logger.
- Packages 2-5 are complete for Root Step 9: Rewards admin settings cover every safe default key, grouped separators are present, list/enum/decimal settings without validated editing are read-only, and English/German setting labels are available.
- Package 6 is complete for Root Step 10: Rewards now registers a shared Tools Info/Status provider and routes `/rewards status` to the shared panel.

## Affected Repositories/Plugins
- `rw-plugin-oz-rewards`
- `rw-plugin-oz-tools`
- `rw-plugin-oz-wallet`
- `rw-plugin-oz-discord-connect`
