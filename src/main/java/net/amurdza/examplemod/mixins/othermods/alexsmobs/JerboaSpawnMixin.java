package net.amurdza.examplemod.mixins.othermods.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.EntityJerboa;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityJerboa.class,remap = false)
public abstract class JerboaSpawnMixin {

    @Inject(method = "canJerboaSpawn", at = @At("HEAD"), cancellable = true)
    private static void aoemod$customSpawnRules(EntityType<EntityJerboa> entityType,
                                                ServerLevelAccessor level,
                                                MobSpawnType reason,
                                                BlockPos pos,
                                                RandomSource random,
                                                CallbackInfoReturnable<Boolean> cir) {

        BlockState below = level.getBlockState(pos.below());

        boolean validBlock =
                below.is(Blocks.GRASS_BLOCK)
                        || below.is(Blocks.MOSS_BLOCK)
                        || below.is(Blocks.SAND);

        cir.setReturnValue(
                reason == MobSpawnType.SPAWNER ||
                        (
                                validBlock &&
                                        level.getRawBrightness(pos, 0) > 8
                        )
        );
    }
}