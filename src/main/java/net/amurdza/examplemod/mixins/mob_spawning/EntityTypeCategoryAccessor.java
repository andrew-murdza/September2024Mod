package net.amurdza.examplemod.mixins.mob_spawning;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityType.class)
public interface EntityTypeCategoryAccessor {
    @Mutable
    @Accessor("category")
    void aoemod$setCategory(MobCategory category);
}