package net.amurdza.examplemod.worldgen.surface_rule;

import com.mojang.serialization.Codec;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.biome.ModBiomes;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

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

    public static final DeferredRegister<Codec<? extends SurfaceRules.ConditionSource>> MATERIAL_CONDITIONS =
            DeferredRegister.create(Registries.MATERIAL_CONDITION, AOEMod.MOD_ID);

    public static final RegistryObject<Codec<? extends SurfaceRules.ConditionSource>> NOISE_THRESHOLD_3D =
            MATERIAL_CONDITIONS.register("noise_threshold_3d", NoiseThreshold3DConditionSource.CODEC::codec);


    public static void register(IEventBus bus) {
        MATERIAL_CONDITIONS.register(bus);
    }
}
