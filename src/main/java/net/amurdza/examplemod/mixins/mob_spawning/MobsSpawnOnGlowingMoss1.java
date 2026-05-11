package net.amurdza.examplemod.mixins.mob_spawning;

import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockBehaviour.class)
public interface MobsSpawnOnGlowingMoss1 {

    @Accessor("properties")
    BlockBehaviour.Properties getProperties();
}