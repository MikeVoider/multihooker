# Contributing to MultiHooker

Thanks for wanting to help. Bug reports are the most useful thing you can send.

## Reporting a bug

Open an issue and use the **Bug report** template. Please include:

- MultiHooker version (shown in the mods list) and NeoForge version,
- Minecraft version (this branch is 1.21.1 only),
- your full `logs/latest.log` or the crash report — a link to a paste site is fine,
- your mod list, or the name of the modpack,
- what you did, what you expected, and what happened instead,
- a screenshot or short clip if it is a visual problem.

Reports that say only "it doesn't work" usually cannot be acted on, so please fill in
the fields.

### Mod-compatibility bugs

If another mod's items do **not** show up in crates, that is usually a missing item
tag. Tell me the mod name and, if you can, the item's registry name
(`modid:item_name`) and I will add it.

## Suggesting a feature

Open an issue with the **Feature request** template. Keep it concrete: what should
happen, and why it fits the mod.

## Pull requests

- Keep changes focused; one fix or feature per PR.
- Match the surrounding code style.
- Make sure `./gradlew build` passes.
- Do **not** commit build output, decompiled Minecraft sources, or any game files.
  The `.gitignore` already excludes `build/`, `.gradle/` and the run folders.

## Development

```bash
./gradlew build          # build the jar (build/libs/)
./gradlew runClient      # launch a dev client
./gradlew runServer      # launch a dev server
```

The mod targets **Minecraft 1.21.1 / NeoForge 21.1.x**.

## License

By contributing you agree that your contributions are licensed under the MIT License
(see [LICENSE](LICENSE)).
