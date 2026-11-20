package dev.rinchan.killconfirmed;

import dev.rinchan.killconfirmed.portable.DogTagCaseModel;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.Structure;

/** Resolves the most descriptive landmark (structure, falling back to biome) at a death position (1.20.1). */
public final class DogTagLandmarks {
    private DogTagLandmarks() {}

    public record Landmark(ResourceLocation id, Component displayName) {}

    public static Landmark resolve(ServerLevel level, BlockPos pos) {
        var structureRegistry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var structureManager = level.structureManager();
        Map<Structure, LongSet> structures = structureManager.getAllStructuresAt(pos);
        for (Structure structure : structures.keySet()) {
            if (structureManager.getStructureAt(pos, structure).isValid()) {
                ResourceLocation key = structureRegistry.getKey(structure);
                if (key != null) {
                    return new Landmark(key, format("structure", key));
                }
            }
        }
        var biomeKey = level.getBiome(pos).unwrapKey();
        if (biomeKey.isPresent()) {
            ResourceLocation id = biomeKey.get().location();
            return new Landmark(id, format("biome", id));
        }
        ResourceLocation dim = level.dimension().location();
        return new Landmark(dim, Component.translatableWithFallback(
                "kill_confirmed.dimension." + dim.getNamespace() + "." + dim.getPath(),
                DogTagCaseModel.humanizePath(dim.getPath())));
    }

    private static Component format(String prefix, ResourceLocation id) {
        String key = prefix + "." + id.getNamespace() + "." + id.getPath().replace('/', '.');
        String fallback = DogTagCaseModel.humanizePath(id.getPath());
        return Component.translatableWithFallback(key, fallback);
    }
}
