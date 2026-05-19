# OZ Rewards 0.1.0

This release publishes OZ Rewards with wallet rewards for recurring activity, combat events, milestones, and sector discoveries.

## Highlights

- Daily login rewards with configurable streak calculation.
- Enemy NPC rewards for bandits, skeletons, and additional configurable NPC definitions.
- Animal rewards for aggressive and defensive-aggressive animals, so wolves are rewarded too.
- One-time rewards for orbit and hell visits.
- Configurable sector discovery rewards with global first-discovery or per-player mode.
- Players can toggle reward messages in the shared plugin settings UI.
- Optional Discord notifications through OZ Discord Connect.

## Known Issue

- Lightning strike rewards do not work correctly yet. The current workaround only detects generic environment damage during thunder, storm, or hurricane weather, so it cannot reliably distinguish real lightning strikes yet.

## Installation

Make sure the required plugins are installed:

- `OZTools` `0.18.0` or newer
- `OZWallet` for reward deposits
- optionally `OZDiscordConnect` for Discord messages
- `OZRewards` `0.1.0`

No database migration is required.
