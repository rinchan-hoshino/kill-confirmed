# Integration reference

[Dog tag customization](customization.md)

## Snapshot and Java API

`dev.rinchan.killconfirmed.api.DogTagSnapshot` captures the owner UUID/name/display component, optional killer UUID/type/display component, native death-message component, integer death coordinates, dimension ID/display component, and experience level at death. The complete structured snapshot is stored in the dog tag's custom data. Add-ons can read it with `KillConfirmedApi.readSnapshot(stack, level.registryAccess())`.

Add-ons register a namespaced component provider without internal access:

```java
KillConfirmedApi.registerPlaceholder(
    ResourceLocation.fromNamespaceAndPath("example", "rank"),
    context -> Optional.of(Component.literal("S").withStyle(ChatFormatting.GOLD))
);
```

The `kill_confirmed` namespace is reserved. Duplicate keys are rejected. Provider exceptions are logged and isolated. Optional values should be paired with a template line's `when` key.

Built-in placeholders are:

- `kill_confirmed:owner`
- `kill_confirmed:killer` (only when present)
- `kill_confirmed:death_message`
- `kill_confirmed:coordinates`
- `kill_confirmed:dimension`
- `kill_confirmed:dimension_id`
- `kill_confirmed:level`

Placeholder values are native JSON text components, so translatable content remains client-language localized.

## Lore configuration

On first initialization, `config/kill_confirmed.json` is created with the built-in template. `drop_strategy` accepts `AT_DEATH_POSITION` or `ON_RESPAWN_INVENTORY`. `lore_template` is an array of lines:

```json
{
  "when": "example:rank",
  "component": {
    "translate": "tooltip.example.rank",
    "with": [{"placeholder": "example:rank"}]
  }
}
```

A node containing only `placeholder` is replaced by that provider's component without flattening its formatting. A missing unconditional placeholder is an explicit rendering error.

## Datapack function protocol

Add functions to `#kill_confirmed:placeholder_providers`. Callbacks are invoked as the deceased player after input has been written to storage `kill_confirmed:scratch`:

- `input`: the versioned structured snapshot shown below
- `output`: an initially empty list

```snbt
input: {
  schema: 1,
  owner: {uuid: [I; ...], name: "Player", component: '{"text":"Player"}'},
  killer: {uuid: [I; ...], type: "minecraft:zombie", component: '{"translate":"entity.minecraft.zombie"}'},
  death_message: '{"translate":"death.attack.mob",...}',
  position: {x: 0, y: 64, z: 0},
  dimension: {id: "minecraft:overworld", component: '{"translate":"dimension.minecraft.overworld"}'},
  experience_level: 12
}
```

`killer` is absent when the snapshot has no killer. Every `component` and `death_message` value is a JSON text-component string. A function appends one result like this:

```mcfunction
data modify storage kill_confirmed:scratch output append value {id:"example:rank",component:'{"text":"S","color":"gold"}'}
```

The output is all-or-nothing validated: at most 32 entries, 8 KiB per component, 32 KiB total, valid non-reserved namespaced IDs, no duplicate IDs, and valid native component JSON. Java providers have deterministic precedence over datapack output on a cross-surface collision. Scratch storage is cleared before and after every invocation, including failures.
