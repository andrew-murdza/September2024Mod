package net.amurdza.examplemod.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;

public final class WitherTargets {
    private WitherTargets() { }

    public static boolean allowed(Entity entity) {
        return entity instanceof Player || entity instanceof AbstractVillager || entity instanceof AbstractPiglin;
    }
}
