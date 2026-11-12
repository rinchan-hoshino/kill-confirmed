# Changelog

## 1.2.0

- Use a single commented `config/kill_confirmed.toml` for delivery, player-only kills, an optional function and lore. Old JSON is ignored with a startup warning; no migration is performed.
- Replace the datapack function-tag/scratch-storage protocol and `when` conditions with an optional function and vanilla score, NBT and selector text components resolved as the deceased player.
- Give new tags a gold, non-italic localized name and three restrained lore lines: vanilla death message, coordinates/dimension, and level/day.
- Add `day` and `weapon` placeholders, plus short built-in names and individual coordinates. Hide a whole line when a value is absent or parsing fails; warn once per invalid line.
- Bundle RinLib 2.0; retain existing snapshots, pending-player saves, client protocol and right-click behavior.

## 1.1.0

- Fix dog tag registration on Minecraft 26.x, which requires an item registry ID before construction.
- Fix datapack placeholder functions not completing before dog tag creation, and restore the command context after callbacks, including failures.
- Share gameplay and snapshot serialization across the existing Minecraft versions; use RinLib 1.1.0 for Minecraft integration.
- Remove unused delivery state-machine code and unnecessary pending-data copies.
- Return copies of text components from public death snapshots.
- Preserve the schema-one snapshot layout, pending-player save key, item ID and configuration format.

## 1.0.1

- Publish the corrected mod icon with the existing dog-tag behavior unchanged.
- Include the proof-gated drop-strategy and provider contract tests in source.

## 1.0.0

- Create one structured dog tag for every real server-player death.
- Record localized death information, optional killer, coordinates, dimension and vanilla experience level.
- Support death-position delivery by default or persistent respawn-inventory delivery by configuration.
- Add data-driven native-component lore templates, Java placeholder providers and datapack function providers.
- Release for Fabric and NeoForge with RinLib 1.0.0 as a required dependency.
