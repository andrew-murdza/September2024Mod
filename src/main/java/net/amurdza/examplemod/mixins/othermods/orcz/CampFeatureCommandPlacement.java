package net.amurdza.examplemod.mixins.othermods.orcz;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.amurdza.examplemod.worldgen.OrczCampPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.server.commands.PlaceCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlaceCommand.class)
public abstract class CampFeatureCommandPlacement {
    @WrapOperation(method = "placeFeature", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/feature/ConfiguredFeature;place(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z"))
    private static boolean aoemod$placeCampAtRequestedPosition(ConfiguredFeature<?, ?> feature, WorldGenLevel level,
            ChunkGenerator generator, RandomSource random, BlockPos pos, Operation<Boolean> original) {
        if (level instanceof ServerLevel server && OrczCampPlacement.handles(server, feature)) {
            return OrczCampPlacement.place(server, pos, random);
        }
        return original.call(feature, level, generator, random, pos);
    }
}
