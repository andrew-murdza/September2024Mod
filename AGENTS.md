# Repository Guidelines

## Project Structure & Module Organization

This is a Java 17 Minecraft Forge 1.20.1 mod. Production code lives under `src/main/java/net/amurdza/examplemod/`; organize additions by responsibility, following the existing packages such as `block`, `entity`, `event_handlers`, `mixins`, `registry`, and `worldgen`. Mod metadata, mixin configuration, access transformers, data packs, models, language files, and textures live in `src/main/resources/`. Generated data belongs in `src/generated/resources/` and is included in the main resource set. `run/`, `run-data/`, `.gradle/`, and `build/` are local or generated output—do not commit their contents. Local dependency JARs are resolved from `libs/`.

## Build, Test, and Development Commands

Use the checked-in Gradle wrapper from PowerShell:

- `.\gradlew.bat build` — compiles Java, processes resources, and creates the mod JAR in `build/libs/`.
- `.\gradlew.bat runClient` — launches a development Minecraft client using `run/`.
- `.\gradlew.bat runServer` — starts the dedicated development server without a GUI.
- `.\gradlew.bat runData` — regenerates data resources into `src/generated/resources/`.
- `.\gradlew.bat genIntellijRuns` — creates IntelliJ run configurations after setup changes.

## Coding Style & Naming Conventions

Use UTF-8 and four-space indentation. Follow Java conventions: `PascalCase` classes, `camelCase` methods and fields, and `UPPER_SNAKE_CASE` constants. Keep packages lowercase and place new code under the existing `net.amurdza.examplemod` namespace. Resource identifiers and JSON filenames should be lowercase `snake_case` (for example, `bone_sword.json`). Keep registry declarations in `registry`, event subscribers in `event_handlers`, and compatibility mixins in the matching `mixins/othermods/<mod>` package. No formatter or linter is configured; use your IDE's standard Java formatting and avoid unrelated reformatting.

## Testing Guidelines

There is currently no JUnit suite or coverage threshold. Validate every change with `.\gradlew.bat build`, then exercise affected behavior through `runClient` or `runServer`. Forge GameTests can be added under the `aoemod` namespace and run with `.\gradlew.bat runGameTestServer`. Put future unit tests in `src/test/java` and name them `<ClassName>Test.java`. For data changes, run `runData` and review generated diffs.

## Commit & Pull Request Guidelines

History uses short, imperative summaries such as `Optimized Code` and `Edited water plants...`. Prefer a more specific equivalent, for example `Fix water plant biome placement`. Keep each commit focused. Pull requests should explain player-visible behavior, identify affected packages or resources, list validation commands, and link relevant issues. Include screenshots or short clips for rendering, model, texture, biome, or world-generation changes.
