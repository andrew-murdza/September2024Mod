package net.amurdza.examplemod.worldgen.structure;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/** A single continuous fortress through the four vertical Nether habitats. */
public final class NetherCitadelStructure extends Structure {
    public static final Codec<NetherCitadelStructure> CODEC = simpleCodec(NetherCitadelStructure::new);

    public NetherCitadelStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x = context.chunkPos().getMiddleBlockX();
        int z = context.chunkPos().getMiddleBlockZ();
        if (context.heightAccessor().getMinBuildHeight() > NetherCitadelLayout.MIN_Y
                || context.heightAccessor().getMaxBuildHeight() <= NetherCitadelLayout.MAX_Y
                || !context.biomeSource().getNoiseBiome(x >> 2, 26, z >> 2, context.randomState().sampler())
                        .is(new ResourceLocation("aoemod", "soul_sand_valley"))) return Optional.empty();
        // Keep the center in its owner chunk: every wing stays within vanilla's eight-chunk reference radius.
        var origin = new BlockPos(x, NetherCitadelLayout.MIN_Y, z);
        if (!NetherCitadelApproach.sample(origin, context.chunkGenerator(), context.randomState(),
                context.heightAccessor()).hasUpperEntrance()) return Optional.empty();
        long seed = context.random().nextLong();
        return Optional.of(new GenerationStub(origin.atY(104),
                builder -> builder.addPiece(new NetherCitadelPiece(origin, seed))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.NETHER_CITADEL.get();
    }
}
