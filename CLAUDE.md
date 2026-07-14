# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Mod Does

**Path To Dirt** is a small NeoForge Minecraft mod (1.21.1) that allows players to convert dirt paths and farmland back to regular dirt using any shovel. Transformations are configurable, and specific items/tags can be blacklisted.

## Build & Run Commands

```bash
./gradlew build         # Compile and package the mod JAR
./gradlew runClient     # Launch Minecraft client with the mod loaded
./gradlew runServer     # Launch dedicated server with the mod
./gradlew runData       # Run data generator (outputs to src/generated/resources/)
```

- Java 21 toolchain is required (ModDevGradle 2 + NeoForge).
- Build artifacts go to `build/libs/`.
- There are no unit tests; verification is done by running the client.

## Architecture

The mod uses **InsaneLib's single-module feature** pattern (InsaneLib is pulled from the Modrinth maven, version in `gradle.properties`):

```
PathToDirt (mod entry, @Mod)
  └─ ILModConfig (single "main" module, COMMON config)
       └─ BaseFeature (Feature, discovered via @LoadFeature annotation scan)
            └─ onRightClick() – BlockEvent.BlockToolModificationEvent
```

- `Feature` subclasses annotated with `@LoadFeature` are discovered automatically from the mod's annotation scan data; with a single module, no `module = ...` id is needed on the annotation.
- `@Config` fields (must be static) are registered automatically, including `List<String>`, `Blacklist<T>` and enums.

**Key files:**
- `src/main/java/insane96mcp/pathtodirt/PathToDirt.java` — mod entry point, creates the `ILModConfig` single module and registers the config.
- `src/main/java/insane96mcp/pathtodirt/feature/BaseFeature.java` — all mod logic; listens for `ItemAbilities.SHOVEL_FLATTEN` tool modifications, applies transform list, checks item blacklist.
- `src/main/templates/META-INF/neoforge.mods.toml` — mod metadata template; `${...}` properties are expanded from `gradle.properties` by the `generateModMetadata` task.
- `src/main/resources/data/pathtodirt/advancement/path_to_dirt.json` — "Back to Dirt" advancement (note: singular `advancement` folder since 1.21).
- `src/main/resources/assets/pathtodirt/lang/en_us.json` — all user-facing strings.

## Adding New Transformations or Features

- **New block transformation:** Add an entry to the `Transformations List` config in `BaseFeature` (format: `source_block>target_block`, optional `[prop=val]` block state properties on both sides).
- **New feature:** Create a class extending InsaneLib's `Feature` annotated with `@LoadFeature`; it will be discovered automatically.
- **New config option:** Add a static field annotated with `@Config` in the relevant `Feature` subclass.

## Releasing

- `.github/workflows/build.yml` builds on push/PR.
- `.github/workflows/publish.yaml` (manual dispatch) builds and publishes to CurseForge + Modrinth via mc-publish, using the full contents of `changelog.md` as the changelog.
- Bump `mod_version` in `gradle.properties` (4-part scheme, e.g. `2.0.0.0`) and prepend a section to `changelog.md`.
