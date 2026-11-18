package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.animal.horse.ZombieHorse;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Undead horses, including the horseman mounts, share ordinary undead sunlight burning. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public final class UndeadHorseSunlight {
    private UndeadHorseSunlight() { }

    public static boolean applies(Entity entity) {
        return entity instanceof SkeletonHorse || entity instanceof ZombieHorse;
    }

    public static boolean exposed(LivingEntity horse) {
        BlockPos eyes = BlockPos.containing(horse.getX(), horse.getEyeY(), horse.getZ());
        return applies(horse) && horse.isAlive() && horse.level().isDay()
                && !horse.isInWaterRainOrBubble() && !horse.level().getBlockState(horse.blockPosition()).is(Blocks.POWDER_SNOW)
                && horse.getLightLevelDependentMagicValue() > .5F && horse.level().canSeeSky(eyes);
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        LivingEntity horse = event.getEntity();
        if (!horse.level().isClientSide() && exposed(horse)) horse.setSecondsOnFire(8);
    }
}
