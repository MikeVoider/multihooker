# MultiHooker Agent Instructions

Minecraft 1.21.1 NeoForge mod (fishing + crates). Build with Gradle (Java 21 toolchain).

## Build / release workflow

1. `./gradlew clean build` — compiles Java, runs the JSON minifier in `processResources`, writes `build/libs/multihooker-<version>.jar`
2. `python tools/crategen/gen_textures.py` — regenerates all 12 tiers × side/front/top PNGs into `tools/crategen/out/`
3. `python tools/crategen/gen_resources.py` — regenerates all block models, blockstates, loot tables, tags, and copies textures into `src/main/resources/`
4. `python tools/crategen/validate_jar.py <jar-path>` — release gate; must exit 0 before shipping. Run after every build.
5. Copy the validated jar to exactly one folder: `C:\Users\Mikeg\Desktop\Modding Project\Done For Testing` (delete old jar first, never create new folders).
6. Changelog: long plain-text version shown to user first; nothing published until "yes go ahead". Short version goes on the desktop changelog file.
7. Publishing: `cf-publish.ps1` for CurseForge. Last commit `2f8044c`, last tag `v3.10.5` — 3.10.6 work is uncommitted.

## Codegen rules

- `tools/crategen/tiers.py` defines all 12 crate tiers (8 normal + 4 special). Never reuse a `seed` value when adding/renumbering tiers — it reshuffles existing art.
- `gen_textures.py` draws crate art: TEXT_SCALE=2, FONT_3X5. Text is flat engraved into textures, never 3D geometry.
- `gen_resources.py` writes block/item models, blockstates, loot tables, and copies textures. Item models must be bare parents (no `display` block, no `elements`).
- Block models: exactly 2 elements (body + lid). No 3D letter elements allowed (validator fails with "letter boxes").
- All crate block display names read "Trove" (registry IDs and Java field names untouched).

## Critical gotchas

- `minecraft:luck_bonus` does NOT exist in 1.21.1. Any loot table using it makes crates drop nothing. `validate_jar.py` checks for this.
- Item models for crates: bare parent only. Any `display` block resizes the held crate.
- Jar minification: Gradle `processResources` minifies all JSON at build time (verified semantically identical). Source JSONs stay pretty-printed.
- Java version 21 required (Minecraft 1.20.5+ ships Java 21 to end users).
- Do not report bugs to third-party GitHub trackers.

## Commands

- `./gradlew build` — build jar
- `./gradlew runClient` — dev client
- `./gradlew runServer` — dev server
- `python tools/crategen/validate_jar.py <jar>` — validate (exit code = failure count)