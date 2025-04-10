package net.amurdza.examplemod.worldgen.biome;

import net.amurdza.examplemod.block.ModBlocks;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;

public class ModSurfaceRules {

    public static SurfaceRules.RuleSource makeRules() {
        SurfaceRules.RuleSource MOSS_BLOCK = makeStateRule(Blocks.MOSS_BLOCK);
        SurfaceRules.RuleSource BEDROCK = makeStateRule(Blocks.BEDROCK);
        SurfaceRules.RuleSource mossInRainForest=SurfaceRules.ifTrue(SurfaceRules.isBiome(ModBiomes.RAINFOREST_KEY),MOSS_BLOCK);
        SurfaceRules.RuleSource bedRockRule=SurfaceRules.ifTrue(SurfaceRules.verticalGradient
                ("bedrock_floor",VerticalAnchor.bottom(),VerticalAnchor.bottom()),BEDROCK);
        return SurfaceRules.sequence(bedRockRule,mossInRainForest);
    }

    private static SurfaceRules.RuleSource makeStateRule(Block block) {
        return SurfaceRules.state(block.defaultBlockState());
    }
}
