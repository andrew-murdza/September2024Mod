package net.amurdza.examplemod.config;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.fml.common.Mod;

import java.util.LinkedHashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MobStatsConfig
{
    public static final Map<TagKey<Biome>, Integer> BIOME_TO_LLAMA_HEALTH = new LinkedHashMap<>();
    public static final Map<TagKey<Biome>, Integer> BIOME_TO_HORSE_HEALTH = new LinkedHashMap<>();
    public static final Map<TagKey<Biome>, Integer> BIOME_TO_DONKEY_HEALTH = new LinkedHashMap<>();
    public static final Map<TagKey<Biome>, Integer> BIOME_TO_MULE_HEALTH = new LinkedHashMap<>();
    public static final Map<TagKey<Biome>, Float> BIOME_TO_LLAMA_SPEED = new LinkedHashMap<>();
    public static final Map<TagKey<Biome>, Float> BIOME_TO_HORSE_SPEED = new LinkedHashMap<>();
    public static final Map<TagKey<Biome>, Float> BIOME_TO_DONKEY_SPEED = new LinkedHashMap<>();
    public static final Map<TagKey<Biome>, Float> BIOME_TO_MULE_SPEED = new LinkedHashMap<>();
    public static final Map<TagKey<Biome>, Float> BIOME_TO_HORSE_JUMP_STRENGTH = new LinkedHashMap<>();
    public static final Map<TagKey<Biome>, Float> BIOME_TO_DONKEY_JUMP_STRENGTH = new LinkedHashMap<>();
    public static final Map<TagKey<Biome>, Float> BIOME_TO_MULE_JUMP_STRENGTH = new LinkedHashMap<>();

    private static final int MIN_LLAMA_HEALTH = 15;
    private static final int MIN_HORSE_HEALTH = 15;
    private static final int MIN_DONKEY_HEALTH = 15;
    private static final int MIN_MULE_HEALTH = 15;

    private static final float MIN_LLAMA_SPEED = 0.175F;
    private static final float MIN_HORSE_SPEED = 0.1125F;
    private static final float MIN_DONKEY_SPEED = 0.175F;
    private static final float MIN_MULE_SPEED = 0.175F;

    private static final float MIN_HORSE_JUMP_STRENGTH = 0.4F;
    private static final float MIN_DONKEY_JUMP_STRENGTH = 0.5F;
    private static final float MIN_MULE_JUMP_STRENGTH = 0.5F;

    static {
        fillAllBiomes(BIOME_TO_LLAMA_HEALTH, MIN_LLAMA_HEALTH);
        fillAllBiomes(BIOME_TO_HORSE_HEALTH, MIN_HORSE_HEALTH);
        fillAllBiomes(BIOME_TO_DONKEY_HEALTH, MIN_DONKEY_HEALTH);
        fillAllBiomes(BIOME_TO_MULE_HEALTH, MIN_MULE_HEALTH);

        fillAllBiomes(BIOME_TO_LLAMA_SPEED, MIN_LLAMA_SPEED);
        fillAllBiomes(BIOME_TO_HORSE_SPEED, MIN_HORSE_SPEED);
        fillAllBiomes(BIOME_TO_DONKEY_SPEED, MIN_DONKEY_SPEED);
        fillAllBiomes(BIOME_TO_MULE_SPEED, MIN_MULE_SPEED);

        fillAllBiomes(BIOME_TO_HORSE_JUMP_STRENGTH, MIN_HORSE_JUMP_STRENGTH);
        fillAllBiomes(BIOME_TO_DONKEY_JUMP_STRENGTH, MIN_DONKEY_JUMP_STRENGTH);
        fillAllBiomes(BIOME_TO_MULE_JUMP_STRENGTH, MIN_MULE_JUMP_STRENGTH);

        BIOME_TO_LLAMA_HEALTH.put(ModTags.Biomes.tropicalBiomes,30);
        BIOME_TO_LLAMA_HEALTH.put(ModTags.Biomes.savannaBiomes,60);
        BIOME_TO_HORSE_HEALTH.put(ModTags.Biomes.tropicalBiomes,30);
        BIOME_TO_HORSE_HEALTH.put(ModTags.Biomes.savannaBiomes,60);
        BIOME_TO_DONKEY_HEALTH.put(ModTags.Biomes.tropicalBiomes,30);
        BIOME_TO_DONKEY_HEALTH.put(ModTags.Biomes.savannaBiomes,60);
        BIOME_TO_DONKEY_HEALTH.put(ModTags.Biomes.plainsBiomes,24);
        BIOME_TO_LLAMA_SPEED.put(ModTags.Biomes.tropicalBiomes,0.2f);
        BIOME_TO_LLAMA_SPEED.put(ModTags.Biomes.savannaBiomes,0.3f);
        BIOME_TO_HORSE_SPEED.put(ModTags.Biomes.tropicalBiomes,0.24f);
        BIOME_TO_HORSE_SPEED.put(ModTags.Biomes.savannaBiomes,0.4f);
        BIOME_TO_HORSE_SPEED.put(ModTags.Biomes.plainsBiomes,0.225f);
        BIOME_TO_DONKEY_SPEED.put(ModTags.Biomes.tropicalBiomes,0.2f);
        BIOME_TO_DONKEY_SPEED.put(ModTags.Biomes.savannaBiomes,0.3f);
        BIOME_TO_DONKEY_SPEED.put(ModTags.Biomes.plainsBiomes,0.175f);
        BIOME_TO_HORSE_JUMP_STRENGTH.put(ModTags.Biomes.tropicalBiomes,0.8f);
        BIOME_TO_HORSE_JUMP_STRENGTH.put(ModTags.Biomes.savannaBiomes,1.1f);
        BIOME_TO_HORSE_JUMP_STRENGTH.put(ModTags.Biomes.plainsBiomes,0.7f);
        BIOME_TO_DONKEY_JUMP_STRENGTH.put(ModTags.Biomes.tropicalBiomes,0.7f);
        BIOME_TO_DONKEY_JUMP_STRENGTH.put(ModTags.Biomes.savannaBiomes,0.7f);
        BIOME_TO_DONKEY_JUMP_STRENGTH.put(ModTags.Biomes.plainsBiomes,0.6f);
    }

    private static <T> void fillAllBiomes(Map<TagKey<Biome>, T> map, T value) {
        map.put(ModTags.Biomes.tropicalBiomes, value);
        map.put(ModTags.Biomes.savannaBiomes, value);
        map.put(ModTags.Biomes.plainsBiomes, value);
        map.put(ModTags.Biomes.mountainBiomes, value);
        map.put(ModTags.Biomes.desertBiomes, value);
        map.put(ModTags.Biomes.deepDarkBiomes, value);
        map.put(ModTags.Biomes.soulSandValleyBiomes, value);
        map.put(ModTags.Biomes.warpedForestBiomes, value);
        map.put(ModTags.Biomes.crimsonForestBiomes, value);
        map.put(ModTags.Biomes.basaltDeltasBiomes, value);
    }
}
