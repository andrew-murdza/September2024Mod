package net.amurdza.examplemod.mixins.othermods.netherdepthsupgrade;

import com.scouter.netherdepthsupgrade.entity.LavaAnimal;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LavaAnimal.class,remap = false)
public class LavaFishSpawning {
    @Inject(method = "checkSurfaceLavaAnimalSpawnRules", at = @At("HEAD"), cancellable = true, remap = false)
    private static void aoemod$allowAnyYLevel(
            EntityType<? extends LavaAnimal> entityType,
            LevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random,
            CallbackInfoReturnable<Boolean> cir
    ) {
        cir.setReturnValue(spawnType == MobSpawnType.SPAWNER
                || (level.getFluidState(pos.below()).is(FluidTags.LAVA)
                && level.getBlockState(pos.above()).is(Blocks.LAVA)));
    }
}
