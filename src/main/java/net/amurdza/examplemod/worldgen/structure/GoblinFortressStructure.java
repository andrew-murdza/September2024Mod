package net.amurdza.examplemod.worldgen.structure;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.registry.ModStructures;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/** The existing yeti-cave structure ID now generates an inhabited mountainside fortress in new terrain. */
public final class GoblinFortressStructure extends Structure {
    public static final Codec<GoblinFortressStructure> CODEC = simpleCodec(GoblinFortressStructure::new);

    public GoblinFortressStructure(StructureSettings settings) { super(settings); }

    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        var site = GoblinFortressPlacement.site(context.randomState(), context.chunkPos());
        if (site == null) return Optional.empty();
        var inward = site.uphill() > 0 ? Direction.SOUTH : Direction.NORTH;
        var origin = site.origin();
        if (origin.getY() + GoblinFortressLayout.MIN_Y < context.heightAccessor().getMinBuildHeight()
                || origin.getY() + GoblinFortressLayout.MAX_Y >= context.heightAccessor().getMaxBuildHeight()) return Optional.empty();
        long seed = context.random().nextLong();
        return Optional.of(new GenerationStub(origin, builder -> builder.addPiece(new GoblinFortressPiece(origin, inward, seed))));
    }

    @Override public StructureType<?> type() { return ModStructures.GOBLIN_FORTRESS.get(); }
}
