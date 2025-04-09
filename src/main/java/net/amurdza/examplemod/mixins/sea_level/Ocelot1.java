package net.amurdza.examplemod.mixins.sea_level;

import net.amurdza.examplemod.worldgen.WorldGenUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Ocelot.class)
public abstract class Ocelot1 extends Entity {
    public Ocelot1(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Redirect(method = "checkSpawnObstruction",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelReader;getSeaLevel()I"))
    private int hi2(LevelReader instance){
        return WorldGenUtils.getSeaLevelWorldGen(blockPosition(),level());
    }

}
