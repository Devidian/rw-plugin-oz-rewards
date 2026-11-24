# rw-plugin-oz-rewards
<!-- codemap-id: rw-plugin-oz-rewards -->

## Purpose

Login, combat, lightning and discovery reward rules with player notifications. [Descriptor](../src/resources/plugin.yml) names [`Rewards`](../src/de/omegazirkel/risingworld/Rewards.java); [`RewardsRuntime`](../src/de/omegazirkel/risingworld/RewardsRuntime.java) handles lifecycle and dispatch.

## Modules

- [Reward rules and integrations](modules/rewards.md).
- [Settings and player UI](modules/settings.md).

## Build and tests

[Maven](../pom.xml) builds against the Rising World API and OZ Tools. Explore [tests](../src-test/de/omegazirkel/risingworld/rewards/) and follow [repository instructions](../AGENTS.md) for validation.

## Dependencies

- `rw-plugin-oz-tools/shared-api` — build and shared runtime infrastructure. [Build configuration](../pom.xml)
