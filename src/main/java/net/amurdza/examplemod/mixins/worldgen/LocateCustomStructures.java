package net.amurdza.examplemod.mixins.worldgen;

import com.mojang.datafixers.util.Pair;
import net.amurdza.examplemod.worldgen.structure.LocatableStructurePlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** Vanilla searches only random-spread and concentric-ring placements. */
@Mixin(ChunkGenerator.class)
public abstract class LocateCustomStructures {
    @Shadow
    private static Pair<BlockPos, Holder<Structure>> getStructureGeneratingAt(Set<Holder<Structure>> structures,
            LevelReader level, StructureManager manager, boolean skipKnownStructures,
            StructurePlacement placement, ChunkPos chunk) {
        throw new AssertionError();
    }

    @Inject(method = "findNearestMapStructure", at = @At("RETURN"), cancellable = true)
    private void aoemod$locateCustomPlacements(ServerLevel level, HolderSet<Structure> structures, BlockPos origin,
            int radius, boolean skipKnownStructures, CallbackInfoReturnable<Pair<BlockPos, Holder<Structure>>> cir) {
        var state = level.getChunkSource().getGeneratorState();
        Map<StructurePlacement, Set<Holder<Structure>>> placements = new LinkedHashMap<>();
        for (var structure : structures) {
            // This also excludes structures whose biomes cannot occur in this dimension.
            for (var placement : state.getPlacementsForStructure(structure)) {
                if (placement instanceof LocatableStructurePlacement) {
                    placements.computeIfAbsent(placement, ignored -> new LinkedHashSet<>()).add(structure);
                }
            }
        }
        if (placements.isEmpty()) return;

        var candidates = new ArrayList<Pair<StructurePlacement, ChunkPos>>();
        placements.forEach((placement, holders) -> {
            for (var chunk : ((LocatableStructurePlacement) placement).locateCandidates(state, origin, radius)) {
                candidates.add(Pair.of(placement, chunk));
            }
        });
        candidates.sort(Comparator.comparingDouble(candidate -> candidate.getFirst().getLocatePos(candidate.getSecond()).distSqr(origin)));
        var vanilla = cir.getReturnValue();
        double bestDistance = vanilla == null ? Double.POSITIVE_INFINITY : vanilla.getFirst().distSqr(origin);
        for (var candidate : candidates) {
            var placement = candidate.getFirst();
            var chunk = candidate.getSecond();
            if (placement.getLocatePos(chunk).distSqr(origin) >= bestDistance) break;
            // Reuse vanilla's saved-start checks, STRUCTURE_STARTS generation and reference handling.
            var found = getStructureGeneratingAt(placements.get(placement), level, level.structureManager(),
                    skipKnownStructures, placement, chunk);
            if (found != null) {
                cir.setReturnValue(found);
                return;
            }
        }
    }
}
