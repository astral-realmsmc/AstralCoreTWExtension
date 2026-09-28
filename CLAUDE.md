# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A single-module [Typewriter](https://github.com/gabber235/TypeWriter) extension (Kotlin, Gradle)
that bridges Typewriter and the **AstralCore Paper module**, in both directions. It is not a Bukkit
plugin: the jar is loaded by Typewriter from `plugins/Typewriter/extensions/`, and its entry classes
are discovered at build time by a KSP processor that emits `extension.json` into the jar.

See `README.md` for the user-facing reference of every entry and every AstralCore-side addition.

## Commands

```bash
./gradlew build                                      # -> build/libs/AstralCoreTWExtension-<version>.jar
./gradlew buildAndMove -PtypewriterServerDir=/path   # build + copy to <path>/plugins/Typewriter/extensions/
```

There are no tests and no linter configured; `build` is the full check. Most mistakes surface as KSP
validator failures during `kspKotlin`, not as compile errors — read that task's output first.

`core-paper` comes from the private AstralRealms repository, falling back to the local Maven
repository. Without repository credentials (`astralRepoUsername`/`astralRepoPassword`, or
`ASTRAL_REPO_USERNAME`/`ASTRAL_REPO_PASSWORD`), install AstralCore locally first:

```bash
cd ../AstralCore && mvn -pl commons,paper -am install -DskipTests
```

Note the local Maven repository for this machine is `/Volumes/Untitled/.m2/repository`, not `~/.m2`.

## Architecture

Two independent halves that share `internal/`:

**Typewriter → AstralCore** (`entries/`) — Typewriter entry classes annotated `@Entry`. Each one
delegates straight to `internal/AstralCoreAccess`, which owns the only contact with AstralCore:

| AstralCore concept | Reached through |
| --- | --- |
| Actions | `PaperActionFactory.instance().createList(plugin, lines)` → `run(DefaultPaperActionContext)` |
| Requirements | `PaperRequirementFactory.instance().createList(...)` → `run(DefaultPaperRequirementContext)` |
| Placeholders / `$fn(...)` / `$e(...)` | `PlaceholderWrappers.wrap(raw, String::class.java)` → `get(parser)` |
| Functions by name | `PaperFunctionFactory.instance().prepare(name, args)` → `compute(parser)` |

The "parser" every context takes is a `PlaceholderContainer` from
`AstralPaperAPI.createPlaceholderContainer(player)`.

**AstralCore → Typewriter** (`astral/`) — implementations of AstralCore's `PaperAction`,
`PaperRequirement`, `PaperFunction` and `ComplexPlaceholder`, registered *globally* (not against one
plugin) by `AstralCoreBridgeInitializer`, a `@Singleton Initializable` that Typewriter calls on
load/unload. They delegate to `internal/TypewriterAccess`, which owns the only contact with the
Typewriter side (`Query`, `Ref.triggerFor`, `ReadableFactEntry.readForPlayersGroup`,
`PlaceholderEntry.parser().parse`).

`internal/Coercion.kt` converts AstralCore's stringly values into whatever a Typewriter field wants.
Typewriter's own `ultraSafeCast` covers strings and numbers but not booleans — that gap is why the
file exists.

## Constraints that are enforced elsewhere

These are checked by KSP validators or by AstralCore's reflection, so violating them fails the build
or fails silently at runtime rather than at the line you wrote.

**Typewriter entries**

- Must be stateless: every constructor parameter is a `val` *and* has a default value. Mutable state
  belongs in a singleton (see `AstralCoreAccess`'s caches) or in an `AudienceFilter`, which is a
  display rather than an entry and may hold state.
- `@Entry` name must match `^[a-z0-9_]+$`.
- `@Entry` icon is validated **over the network against Iconify at build time**. Reuse an icon
  already used in this repo or in Typewriter itself rather than inventing one; a wrong icon fails
  `kspKotlin` with no local way to check it.
- `typewriter { extension { ... } }` in `build.gradle.kts` has length rules: namespace 5–20
  lowercase alphanumerics, name 5–25 alphanumerics and may not contain "Extension" or "Adapter",
  `shortDescription` 10–80 chars, `description` 100–2000 chars.

**AstralCore-side classes** (`astral/`)

- Built reflectively by `PlatformAdapterDeserializer` from a configuration string, so each class
  needs **exactly one** constructor, and its `PlaceholderWrapper<String>` parameters must be
  **nullable** — the deserializer passes nothing for an argument the config line omits, and a
  non-null Kotlin parameter would throw `checkNotNullParameter` on e.g.
  `[typewriter-fact] some_fact`.
- Anything touching the Bukkit API from a trigger must go through the main thread; AstralCore
  actions can be declared `<async=true>`. `TypewriterAccess.onMain` handles this.

## Version coupling

- **Do not bump Kotlin independently.** The Kotlin version is pinned by the KSP version that
  `com.typewritermc.module-plugin` depends on: module-plugin `2.0.0` → KSP `2.2.10-2.0.2` → Kotlin
  `2.2.10`. Check the module-plugin POM before changing either.
- AstralCore compiles to **Java 25** bytecode (class file v69). Kotlin must be new enough to read
  it — 2.2.10 can; older versions fail with "unsupported class file major version". `jvmToolchain`
  stays at 21 (Typewriter's requirement); only the *reading* of the dependency needs the newer
  compiler.
- `engineVersion` in `build.gradle.kts` must match the Typewriter engine running on the server.

## Gotchas

- `configurations.configureEach { exclude(...) }` drops EntityLib and Geyser: the engine pulls them
  from repositories that are currently unreachable (`maven.evokegames.gg` no longer resolves) and
  neither is used here. Remove the exclusions only if entity entries are added, and expect to fix
  the repositories then.
- To look up Typewriter API shapes, fetch the sources jars rather than guessing — the published docs
  lag the engine:
  `https://maven.typewritermc.com/releases/com/typewritermc/engine-{core,paper}/<version>/engine-{core,paper}-<version>-sources.jar`
- Parsed actions, requirements and expressions are cached in `AstralCoreAccess` keyed by the config
  they were written as. The cache holds the `AstralPaperPlugin` instance they were built against, so
  `clearCaches()` on shutdown is not optional — a reload must not keep a stale plugin.
