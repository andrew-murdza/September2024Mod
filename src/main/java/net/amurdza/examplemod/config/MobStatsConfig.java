package net.amurdza.examplemod.config;

import net.amurdza.examplemod.AOEMod;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MobStatsConfig
{
    public static final double LLAMA_HEALTH = 40;
    public static final double HORSE_HEALTH = 40;
    public static final double HORSE_SPEED = 0.4;
    public static final double HORSE_JUMP_STRENGTH = 1.1;
    public static final double DONKEY_HEALTH = 40;
    public static final double DONKEY_SPEED = 0.31;
    public static final double DONKEY_JUMP_STRENGTH = 1.0;
}
