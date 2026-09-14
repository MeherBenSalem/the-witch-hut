# Contributing

Thanks for contributing to The Witch Hut.

## Before you start

- Open an issue for substantial changes before a large implementation.
- Keep changes focused. Avoid unrelated refactors in the same pull request.
- Preserve compatibility-sensitive identifiers (`modId`, package names, registry IDs) unless a change explicitly requires them.

## Development workflow

1. Fork the repository and create a branch from `main`.
2. Make the smallest coherent change that solves the problem.
3. Build the version root you changed from inside that folder (`26.1.2/` or `26.2/`).
4. Keep `shared/resources` and each workspace `common/src/main/resources` datapack in sync when editing structures or worldgen.
5. Open a pull request with a clear summary and verification notes.

## Coding expectations

- Follow the existing patterns in the targeted version root. Version folders are intentionally isolated.
- Do not share Java sources across versions with symlinks.
- Do not commit jars, logs, run directories, secrets, or local IDE files.

## Reporting issues

- Use GitHub issues for bugs and feature requests.
- Include Minecraft version, loader, mod version, reproduction steps, and logs when possible.

By contributing, you agree that your contributions are licensed under the Apache License 2.0 unless stated otherwise.
