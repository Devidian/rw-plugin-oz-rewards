# PLANS.md

- [ ] Validate the next-300926 change on Development with a controlled player check.

Planning is stored in repository-local docs.

- Active implementation tasks: [docs/active/](docs/active/)
- Roadmaps and larger plans: [docs/roadmaps/](docs/roadmaps/)
- Completed phase summaries: [docs/phase-archive.md](docs/phase-archive.md)
- Planning and documentation standards: [docs/policies/repository-policy.md](docs/policies/repository-policy.md)

Every implementation plan must include objective, ownership, dependencies, risks, validation strategy, affected repositories/plugins, rollback considerations, and a markdown checkbox checklist.

## Player erasure

- [x] Delete personal sector rewards and anonymize retained global discoveries in one SQLite transaction.
- [x] Stop delayed first-discovery announcements after erasure.
- [ ] Connect to the shared erasure coordinator and verify a pending announcement on Development.

The SQLite test covers idempotence and unrelated reward retention. A database backup is the rollback point before the full erasure action is enabled.

Discord event thread routing is implemented locally; Development Discord validation and the separately authorized release remain open. Configuration is documented in README.md.
