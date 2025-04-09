package net.amurdza.examplemod.mixins.othermods.blueskies;

import com.legacy.blue_skies.registries.SkiesBlocks;
import com.legacy.blue_skies.registries.SkiesEntityTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SkiesEntityTypes.SpawnConditions.class)
public class MobsSpawnOnMossAndGrass {
    @Redirect(method = "animalSpawnConditions",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;"))
    private static Block hi(BlockState instance){
        return instance.is(BlockTags.ANIMALS_SPAWNABLE_ON)? SkiesBlocks.turquoise_grass_block:instance.getBlock();
    }
}
