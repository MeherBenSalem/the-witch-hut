# The Witch Hut

MultiLoader Minecraft mod that adds a custom **witch hut** structure.

## Features

- Jigsaw structure `better_witch_huts:witch_hut`
- Spawns in swamp, dark forest, and plains
- Structure-block stripping processors
- Fabric and NeoForge builds for Minecraft 26.1.2 and 26.2

## Project layout

```
the-witch-hut/
  shared/resources/   # Canonical datapack (NBT + worldgen + lang)
  26.1.2/             # Fabric + NeoForge
  26.2/               # Fabric + NeoForge
```

## Supported versions

| Minecraft | Fabric | NeoForge | JDK |
|-----------|--------|----------|-----|
| 26.1.2    | Yes    | Yes      | 25  |
| 26.2      | Yes    | Yes      | 25  |

## Build

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64   # or your JDK 25 path

cd 26.1.2 && ./gradlew build --no-daemon
cd ../26.2 && ./gradlew build --no-daemon
```

Jars land in each loader's `build/libs/` as
`better_witch_huts-{fabric|neoforge}-{mc}-1.0.0.jar`.

## Contributing / Security / License

- [CONTRIBUTING.md](CONTRIBUTING.md)
- [SECURITY.md](.github/SECURITY.md)
- Licensed under the [Apache License 2.0](LICENSE)
