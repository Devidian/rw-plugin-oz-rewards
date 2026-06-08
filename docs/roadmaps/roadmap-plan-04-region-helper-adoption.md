# Roadmap Plan 04 Region Helper Adoption

## Objective
Use the Tools `RegionHelper` for region detection in the reward rule that grants rewards for discovering new sectors.

## Ownership
Primary repository: `rw-plugin-oz-rewards`

Supporting repositories:
- `rw-plugin-oz-tools` for `RegionHelper`, shared player settings, i18n, persistence, and overlay behavior.
- `rw-plugin-oz-wallet` for reward deposits.
- `rw-plugin-oz-discord-connect` for optional reward announcements.

## Dependencies
- Hard runtime dependency: `rw-plugin-oz-tools`.
- Functional reward payout dependency: `rw-plugin-oz-wallet`.
- Optional Discord integration remains unchanged.

## Phases
- [x] Phase 1: Locate the current sector-discovery region calculation and compare it with Tools `RegionHelper`.
- [x] Phase 2: Replace local region logic with `RegionHelper` while preserving existing persisted discovery state.
- [x] Phase 3: Keep the implementation isolated so it can be replaced later if the Rising World API exposes a better region source.
- [x] Phase 4: Add Plan 04 player shortcut visibility setting, document the Escape-close API limitation, verify i18n loading, and migration away from deprecated Tools `SQLite` usage if present.
- [x] Phase 5: Update README/HISTORY and validate.

## Progress Notes
- Sector discovery previously stored `Unknown`; it now resolves region names through Tools `RegionHelper` and falls back to `Unknown` on helper/runtime failure.
- Persisted discovery keys remain sector coordinates, so existing reward de-duplication state is preserved.
- Rewards already used `SQLiteConnectionFactory`; no deprecated Tools `SQLite` migration was needed.
- Rewards has no plugin-owned persistent overlay beyond Tools-hosted settings/info panels.
- Rewards now registers player-aware shortcut visibility and exposes a player setting to hide the shortcut.

## Risks
- Changing region calculation can duplicate or skip rewards if persisted keys no longer match old behavior.
- The helper is a workaround until a better game API is available, so code should avoid locking business logic to helper internals.

## Validation Strategy
- Run `mvn -B test` and `mvn -B -DskipTests package`.
- Runtime-smoke sector discovery across adjacent regions, repeated login/re-enter behavior, Wallet deposit, optional Discord announcement, shortcut visibility, and explicit close controls.

## Affected Repositories/Plugins
- `rw-plugin-oz-rewards`
- `rw-plugin-oz-tools`
- `rw-plugin-oz-wallet`
- `rw-plugin-oz-discord-connect`

## Rollback Considerations
Keep persisted discovery data compatible. If helper behavior differs unexpectedly, retain the old calculation behind a small local fallback until data impact is understood.
