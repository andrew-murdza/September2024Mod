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
    public static final Map<TagKey<Biome>, Float> BIOME_TO_LLAMA_SPEED = new LinkedHashMap<>();
    public static final Map<TagKey<Biome>, Float> BIOME_TO_HORSE_SPEED = new LinkedHashMap<>();
    public static final Map<TagKey<Biome>, Float> BIOME_TO_DONKEY_SPEED = new LinkedHashMap<>();
    public static final Map<TagKey<Biome>, Float> BIOME_TO_HORSE_JUMP_STRENGTH = new LinkedHashMap<>();
    public static final Map<TagKey<Biome>, Float> BIOME_TO_DONKEY_JUMP_STRENGTH = new LinkedHashMap<>();

    static {
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
}
