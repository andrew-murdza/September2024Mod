package net.amurdza.examplemod.mixins.access;

import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SaplingBlock.class)
public interface SaplingTreeGrower {
    @Accessor
    AbstractTreeGrower getTreeGrower();

}
