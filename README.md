# AstralCore ↔ Typewriter bridge

A [Typewriter](https://github.com/gabber235/TypeWriter) extension that connects Typewriter to the
**AstralCore Paper module**, in both directions.

* **From Typewriter** — run AstralCore action lists, gate audiences and criteria on AstralCore
  requirements, and read AstralCore placeholders, inline functions and expressions as Typewriter
  variables and facts.
* **From AstralCore** — trigger Typewriter entries, read Typewriter facts, and use
  `%typewriter_…%` placeholders inside any AstralCore menu, dialog or item configuration.

Nothing has to be re-modelled on either side: a reward written once as an AstralCore action list
works in a quest, and a quest fact gates a menu icon.

---

## Requirements

| | |
| --- | --- |
| Typewriter engine | `0.9.0` |
| AstralCore (Paper) | `core-paper` 1.5.1-SNAPSHOT or newer |
| Java | 21+ to build, 25+ at runtime (AstralCore targets Java 25) |

## Building

```bash
./gradlew build          # -> build/libs/AstralCoreTWExtension-<version>.jar
```

`core-paper` is resolved from the private AstralRealms repository, falling back to your local Maven
repository. If you do not have repository credentials, install AstralCore locally first:

```bash
cd ../AstralCore && mvn -pl commons,paper -am install -DskipTests
```

Credentials, when you have them, go in `~/.gradle/gradle.properties` as `astralRepoUsername` /
`astralRepoPassword`, or in the `ASTRAL_REPO_USERNAME` / `ASTRAL_REPO_PASSWORD` environment
variables.

## Installing

Drop the jar into `plugins/Typewriter/extensions/` and run `/typewriter reload`, or let the build
do it:

```bash
./gradlew buildAndMove -PtypewriterServerDir=/path/to/server
```

---

## Typewriter → AstralCore

### `astral_action` — run AstralCore actions

An `ActionEntry` that runs a list of AstralCore action lines for the interacting player. Each line
is written exactly as in an AstralCore configuration, action properties included.

```
[message] <gradient:#ffb347:#ffcc33>Welcome back, %player_name%!</gradient>
[sound] entity.player.levelup
[console-command] <delay=20> give %player_name% diamond 1
[open-menu] <if=[permission] astral.vip> vip_shop
```

### `astral_requirement_audience` — filter an audience

An invertible `AudienceFilterEntry`. Its children are only shown to players who meet every
AstralCore requirement listed. `refreshInterval` (ticks, default `20`) controls how often the
requirements are re-checked; `0` checks only when a player enters the audience.

```
[permission] astral.vip
[placeholder] %astral_level% >= 10
```

### `astral_requirement_fact` — requirements as a fact

A read-only fact that is `1` when the player meets every listed AstralCore requirement, `0`
otherwise. Facts are what Typewriter criteria read, so this makes an AstralCore requirement usable
to gate dialogue, quests and objectives: `= 1` to require it, `= 0` to exclude it.

### `astral_number_fact` — a value as a fact

A read-only fact whose value is an AstralCore placeholder, function or expression, rounded to a
whole number. Anything that does not resolve to a number reads as `0`.

```
%astral_level%
$e(%vault_eco_balance% / 100)
$round(%astral_progress%)
```

### `astral_placeholder` — a value in any field

A `VariableEntry` (and a placeholder entry) backed by AstralCore's placeholder engine. It
understands `%placeholders%` — PlaceholderAPI included — inline `$function(...)` calls and `$e(...)`
expressions, and converts the result to whatever the field expects: text, a number or a boolean.

Each usage may override the expression, so one entry can serve many fields. It is also readable as
`%typewriter_<entry id>%`.

### `astral_function` — call one function by name

A `VariableEntry` that calls a registered AstralCore function directly — `round`, `min`, `max`,
`format-number`, `uppercase`, `apply-transformer`, or one of your own. Arguments are written as they
would be in a configuration, so they may contain placeholders and nested calls.

---

## AstralCore → Typewriter

Registered globally when the extension loads, so **every** AstralCore plugin on the server can use
them, not only AstralCore itself.

### `[typewriter]` action

```yaml
actions:
  - "[typewriter] village_elder_greeting"   # entry id, or entry name
```

Triggers the entry for the clicking player, always on the main thread — an action marked
`<async=true>` stays safe.

### `[typewriter-fact]` requirement

```yaml
requirements:
  - "[typewriter-fact] talked_to_elder"       # non-zero
  - "[typewriter-fact] quest_stage >= 3"      # =, ==, !=, <>, >, >=, <, <=
```

An unknown fact fails the requirement rather than passing it.

### `$tw-fact(...)` function

```yaml
lore:
  - "Stage: $tw-fact(quest_stage) of 5"
  - "Progress: $e($tw-fact(kills) / 10 * 100)%"
```

Reads `0` when the fact does not exist, so it stays usable inside `$e(...)`.

### `%typewriter_…%` placeholders

```yaml
display-name: "Elder — %typewriter_talked_to_elder%"
lore:
  - "Remaining: %typewriter_kills:remaining:10%"
```

Any Typewriter placeholder entry, addressed by id (or name) with `:`-separated arguments. Typewriter
exposes the same placeholders through PlaceholderAPI; registering them here means they resolve
without it, and without the round trip.

---

## Notes

* The extension does nothing, and says so in the log, when AstralCore is not enabled.
* Parsed actions, requirements and expressions are cached — Typewriter entries must be stateless, so
  the cache lives in the bridge and is cleared on shutdown.
* The Typewriter engine pulls in EntityLib and Geyser from repositories that are currently
  unreachable, and neither is used here; `build.gradle.kts` excludes them so resolution stays
  offline-safe. Drop the exclusions if you start using entity entries.
