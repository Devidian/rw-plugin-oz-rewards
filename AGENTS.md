# AGENTS.md

## Repository Purpose

This repository owns reward-specific economy rules for Rising World servers.

## Ownership

Owns:
- daily login reward rules and streak state
- combat reward rules for enemy NPCs and aggressive animals
- storm/lightning workaround reward rules
- reward player notifications and optional Discord reward messages

Does not own:
- wallet balances, currencies, or transaction persistence; those belong to `rw-plugin-oz-wallet`
- reusable storage, i18n, logging, and UI helpers; those belong to `rw-plugin-oz-tools`
- Discord transport internals; those belong to `rw-plugin-oz-discord-connect`

## Dependencies

- Hard runtime dependency: `rw-plugin-oz-tools`
- Required runtime integration: `rw-plugin-oz-wallet`
- Optional runtime integration: `rw-plugin-oz-discord-connect`

Wallet and Discord integrations must remain reflection based unless the dependency policy changes.

## Mandatory Workflow Rules

- Keep reward business logic inside this plugin.
- Do not duplicate reusable helpers from `rw-plugin-oz-tools`.
- Keep Wallet calls limited to the public default-currency API unless the task explicitly requires another currency.
- Treat changes to player settings keys or config keys as migration-sensitive.
- Follow `.codex/agents.toml` and `docs/policies/repository-policy.md`.
- Keep `README.md`, `HISTORY.md`, and `PLANS.md` aligned with behavior changes.

## Validation

- Run `scripts/verify-plugin-api.sh --summary`.
- Verify new Rising World API symbols with `scripts/verify-plugin-api.sh --class` or `--method`.
- Run `mvn -B -DskipTests package`.
- Run `mvn -B test` when tests are present.
- Runtime-smoke login, NPC kill, lightning workaround, missing Wallet, and optional Discord scenarios before release.
