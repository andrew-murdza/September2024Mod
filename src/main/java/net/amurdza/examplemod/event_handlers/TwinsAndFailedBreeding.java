package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.config.MobConfig;
import net.amurdza.examplemod.util.Helper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TwinsAndFailedBreeding {
    @SubscribeEvent
    public static void twinsOrInfertile(BabyEntitySpawnEvent event) {
        if (!(event.getParentA() instanceof AgeableMob parentA)) {
            return;
        }

        if (!(event.getParentB() instanceof AgeableMob parentB)) {
            return;
        }

        if (!(parentA.level() instanceof ServerLevel world)) {
            return;
        }

        if (event.getChild() == null) {
            return;
        }

        float childMultiplier = MobConfig.mobTwinsChance(event.getChild());

        int children = Helper.computeIncrements(parentA.getRandom(), childMultiplier);

        if(children>0&&(parentA instanceof Turtle|| parentA instanceof Frog)){
            return;
        }

        // Vanilla already created one child.
        // So only manually add children after the first one.
        for (int i = 0; i < children; i++) {
            AgeableMob extraChild = parentA.getBreedOffspring(world, parentB);

            if (extraChild == null) {
                continue;
            }
            extraChild.moveTo(parentA.getX(), parentA.getY(), parentA.getZ(), 0.0F, 0.0F);
            extraChild.setBaby(true);
            world.addFreshEntity(extraChild);
        }
        event.setCanceled(true);
    }
}