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

The `kill_confirmed` namespace is reserved. Duplicate keys are rejected. Provider exceptions are logged and isolated. Reference a provider with `{"placeholder":"example:rank"}` in any lore line. A missing value hides the entire line; no `when` condition is needed. Styles on the placeholder object override the provider's outer style without flattening its component.

Built-in names are `player`, `killer`, `weapon`, `death_message`, `x`, `y`, `z`, `dimension`, `level` and `day`. Namespaced Java providers remain available; the old function-tag/scratch-storage protocol has been removed. Use the TOML `function` option and vanilla `score`/`nbt` components instead. See the [configuration guide](customization.md) for a complete example.

The schema-one snapshot layout, item ID, pending-player save key, client protocol and right-click behavior are unchanged. Legacy translation keys remain available for saved tags. `weapon` and `day` are captured into lore at creation; the persisted snapshot keeps its existing fields.
