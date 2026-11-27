# Reward rules and integrations
<!-- codemap-id: rw-plugin-oz-rewards/rewards -->

## Responsibility

Handle player and world events, calculate reward eligibility and request Wallet payments. Wallet owns balances.

## Source and tests

- [`RewardsRuntime`](../../src/de/omegazirkel/risingworld/RewardsRuntime.java) — event processing and payment.
- [`KillRewardCatalog`](../../src/de/omegazirkel/risingworld/rewards/KillRewardCatalog.java) — kill reward catalog.
- [`SectorDiscoveryStore`](../../src/de/omegazirkel/risingworld/rewards/SectorDiscoveryStore.java) — first discoveries, pending claims, payout receipts, and player erasure.
- [`SectorRewardPayout`](../../src/de/omegazirkel/risingworld/rewards/SectorRewardPayout.java) — attempt and recovery policy for default-currency sector rewards.
- [`Wallet`](../../src/de/omegazirkel/risingworld/rewards/Wallet.java) — shared Wallet bridge adapter.
- [Sector payout tests](../../src-test/de/omegazirkel/risingworld/rewards/SectorRewardPayoutTest.java) exercise claim persistence, Wallet outcomes and replay protection.
- [Repository index](../README.md) and [settings defaults](../../src/settings.default.json).

## Dependencies

- `rw-plugin-oz-wallet/ledger` — runtime default-currency reward payments. [Integration source](../../src/de/omegazirkel/risingworld/RewardsRuntime.java)
- `rw-plugin-oz-discord-connect/discord` — optional reward notifications. [Integration source](../../src/de/omegazirkel/risingworld/rewards/DiscordConnect.java)
