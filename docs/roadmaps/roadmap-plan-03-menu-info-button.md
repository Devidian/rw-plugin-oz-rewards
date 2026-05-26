# Roadmap Plan 03 Menu Info Button

## Objective
Add a Rewards radial-main-menu button that opens the existing shared Tools Info/Status panel.

## Ownership
Primary repository: `rw-plugin-oz-rewards`

Supporting repository:
- `rw-plugin-oz-tools` for the shared Info/Status panel contract.

## Dependencies
- Hard runtime dependency: `rw-plugin-oz-tools`.
- Required runtime integration: `rw-plugin-oz-wallet`.
- Optional runtime integration: `rw-plugin-oz-discord-connect`.

## Phases
- [x] Phase 1: Add the Info/Status action to the plugin's main radial menu.
- [x] Phase 2: Reuse the existing Info/Status provider and command behavior.
- [x] Phase 3: Update README/HISTORY and validate.

## Risks
- Menu changes should not alter reward state, Wallet deposits, or optional Discord announcements.

## Validation Strategy
- Run `scripts/verify-plugin-api.sh --summary`.
- Run `mvn -B -DskipTests package`.
- Run `mvn -B test`.
- Runtime-smoke the radial button and existing reward status command behavior.

## Affected Repositories/Plugins
- `rw-plugin-oz-rewards`
- `rw-plugin-oz-tools`
- `rw-plugin-oz-wallet`

## Rollback Considerations
The radial button can be removed without changing reward logic.

## Progress Notes
- Phase 1 complete: Rewards now uses the Tools-provided `icon-ki-info-status` icon for the radial Info/Status action.
- Phase 2 complete: the radial entry reuses the existing `/rewards status` behavior and shared Tools Info/Status provider.
- Phase 3 complete: README/HISTORY were updated.
- Validation passed with `mvn -B test` and `mvn -B -DskipTests package`.
