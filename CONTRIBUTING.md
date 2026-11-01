# Contributing

Thanks for contributing to Witchbound.

## Before you start

- Open an issue for substantial changes before a large implementation.
- Keep changes focused. Avoid unrelated refactors in the same pull request.
- Preserve compatibility-sensitive identifiers: the mod id `better_witch_huts`, the structure id
  `better_witch_huts:witch_hut` and the 1.0 template `witch_hut.nbt`, entity, item and effect ids, pact, hex and
  errand ids (they are saved in player records).

## Development workflow

1. Fork the repository and create a branch from `main`.
2. Make the smallest coherent change that solves the problem.
3. Generated content comes from `tools/`: edit `structures.py`, `datagen.py` or `gen_textures.py` and run
   `python3 tools/gen_textures.py && python3 tools/structures.py && python3 tools/datagen.py` (needs `nbtlib` and
   Pillow). Do not hand-edit the generated JSON or NBT in the workspaces.
4. Java changes go into every version root (`26.1.2/`, `26.2/`, `26.3/`); the roots differ only where the Minecraft
   API does (see `docs/DESIGN-2.0.md`).
5. Build the version roots you changed from inside each folder (`./gradlew build`; the unit tests run with it). On
   26.3, run Gradle on Java 25.
6. Open a pull request with a clear summary and verification notes.

## Testing in game

`tools/qa/devrun.sh` prepares and starts dev servers and a QA client under Xvfb; `tools/qa/rcon.py` drives the server
and `tools/qa/client.py` the client. `docs/QA-2.0.0.md` lists what was checked for the last release.

## Coding expectations

- Follow the existing patterns in the targeted version root. Version folders are intentionally isolated.
- Keep game rules Minecraft-free in `core/` where possible, with unit tests in `common/src/test`.
- Do not share Java sources across versions with symlinks.
- Do not commit jars, logs, run directories, secrets, or local IDE files.

## Reporting issues

- Use GitHub issues for bugs and feature requests.
- Include Minecraft version, loader, mod version, reproduction steps, and logs when possible.

By contributing, you agree that your contributions are licensed under the Apache License 2.0 unless stated otherwise.
