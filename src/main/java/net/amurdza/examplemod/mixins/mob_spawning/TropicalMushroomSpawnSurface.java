package net.amurdza.examplemod.mixins.mob_spawning;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.TropicalSpawnSurfaces;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public final class TropicalMushroomSpawnSurface {
    @Inject(method = "isValidSpawn", at = @At("HEAD"), cancellable = true)
    private void aoemod$mushroomsAreCanopy(BlockGetter getter, BlockPos pos, EntityType<?> type,
                                          CallbackInfoReturnable<Boolean> result) {
        if (getter instanceof LevelReader level && GameplayDimensions.is(level, GameplayDimensions.TROPICAL)
                && TropicalSpawnSurfaces.giantMushroom((BlockState) (Object) this)) result.setReturnValue(false);
    }
}
