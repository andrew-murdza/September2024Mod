package net.amurdza.examplemod.mixins.block_behavior;

import net.amurdza.examplemod.block.RotateableSunflower;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(net.minecraft.world.level.block.Blocks.class)
public class SunflowersAreRotateable {
    @ModifyArg(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/Blocks;register(Ljava/lang/String;Lnet/minecraft/world/level/block/Block;)Lnet/minecraft/world/level/block/Block;"
            ),
            slice = @Slice(
                    from = @At(value = "CONSTANT", args = "stringValue=sunflower"),
                    to = @At(value = "CONSTANT", args = "stringValue=lilac")
            ),
            index = 1
    )
    private static Block aoe$replaceSunflowerBlock(Block original) {
        return new RotateableSunflower(
                BlockBehaviour.Properties
                        .of()
                        .mapColor(MapColor.PLANT)
                        .noCollission()
                        .instabreak()
                        .sound(SoundType.GRASS)
                        .offsetType(BlockBehaviour.OffsetType.XZ)
                        .ignitedByLava()
                        .pushReaction(PushReaction.DESTROY)
        );
    }
}
