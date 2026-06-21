package net.amurdza.examplemod.mixins.accessor;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockBehaviour.Properties.class)
public interface MobsSpawnOnGlowingMoss {

    @Accessor("isValidSpawn")
    void setIsValidSpawn(BlockBehaviour.StateArgumentPredicate<EntityType<?>> predicate);
}