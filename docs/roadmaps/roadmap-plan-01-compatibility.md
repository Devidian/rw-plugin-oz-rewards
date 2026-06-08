# Roadmap Plan 01 Compatibility

## Objective
Record that Roadmap Plan 01 introduces no direct Rewards implementation work, while keeping compatibility watch over Wallet, Shop, and GPS economy decisions.

## Ownership
Primary repository: `rw-plugin-oz-rewards`.

Supporting repositories:
- `rw-plugin-oz-tools` for shared settings reload/admin settings tab adoption if rolled out portfolio-wide.
- `rw-plugin-oz-wallet` remains the required economy backend.
- `rw-plugin-oz-gps` owns teleport-token kill rewards if that feature stays GPS-specific.

## Dependencies
- Hard dependency: `rw-plugin-oz-tools`.
- Required runtime integration: `rw-plugin-oz-wallet`.
- Optional integration: `rw-plugin-oz-discord-connect`.

## Confirmed Decisions
- Roadmap Plan 01 keeps GPS teleport-token kill rewards in GPS for now because the reward currency is GPS-specific.
- Discord event messages remain welcome for feature-specific events when routed through explicit channel ids.
- A generic plugin-currency reward extension point is out of scope for Roadmap Plan 01.

## Work Packages
- [x] Package 1: Adopt shared settings reload/admin settings tab metadata if the portfolio-wide prework is applied to all plugins.
- [x] Package 2: Review GPS teleport-token kill rewards before implementation to confirm they should not move into Rewards.
- [x] Package 3: Verify Wallet API compatibility after Shop/Marketplace/GPS/LandClaim economy work changes currency usage patterns.

## Completion Notes
- Rewards adopted shared admin settings metadata and current Tools runtime standards.
- Generic enemy-NPC rewards, including bandit and skeleton matching, now belong to Rewards. A separate GPS-specific kill-reward implementation is superseded.
- Wallet compatibility was validated during the completed cross-plugin Plan 03 and Plan 04 Maven validation.

## Risks
- Rewards could become the natural owner for generalized kill rewards. GPS token rewards are currently scoped to GPS because they award a GPS-specific currency.
- Wallet API changes made for other plugins could accidentally affect existing reward deposits.

## Validation Strategy
- Existing reward validation remains unchanged unless shared settings/admin-tab adoption changes UI behavior.
- Re-run missing Wallet and Wallet-present reward smoke tests after major Wallet API changes.

## Affected Repositories/Plugins
- `rw-plugin-oz-rewards`
- `rw-plugin-oz-tools`
- `rw-plugin-oz-wallet`
- `rw-plugin-oz-gps`

## Rollback Considerations
No feature behavior changes are planned. Rollback only applies to shared settings/admin-tab adoption if implemented.

## Open Questions
- None.
