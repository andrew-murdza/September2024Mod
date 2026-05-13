package net.amurdza.examplemod.mixins.othermods.faunify;

import com.pepper.faunify.entity.FennecEntity;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = FennecEntity.class,remap = false)
public abstract class FennecSpawnOnMossGrassRegularSand {

    @Redirect(
            method = "canSpawn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z"
            )
    )
    private static boolean aoemod$spawnOnGrassMossOrSand(BlockState state, TagKey<Block> tagKey) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.SAND);
    }
}