package net.amurdza.examplemod;

import com.github.alexthe666.alexsmobs.entity.*;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.iceandfire.entity.EntityPixie;
import com.github.alexthe666.iceandfire.entity.EntitySeaSerpent;
import com.github.alexthe666.iceandfire.entity.EntitySiren;
import com.github.alexthe666.iceandfire.item.IafItemRegistry;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.potionstudios.biomeswevegone.world.level.block.wood.BWGWood;

import java.util.*;
import java.util.function.Function;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    /**
     * Each biome tag has two multipliers:
     * - plant: general plants (grass, flowers, crops, etc.)
     * - mushroom: anything we treat as "mushroom category" (e.g., MYCELIUM blocks and MushroomBlock instances)
     */
    private static final Map<TagKey<Biome>, BiomeBonemealMultipliers> DEFAULTS = new HashMap<>();

    public static final Map<TagKey<Biome>, BiomeBonemealMultipliers> BIOME_TAG_TO_BONEMEAL_MULTIPLIERS = new LinkedHashMap<>();




    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        // Tropical / Jungle: *4
        defineBiomeTagMultipliers(builder,
                ModTags.Biomes.tropicalBiomes,
                4.0,  // plant
                4.0   // mushroom
        );

        // Savanna: *2
        defineBiomeTagMultipliers(builder,
                ModTags.Biomes.savannaBiomes,
                2.0,  // plant
                2.0   // mushroom
        );

        defineBiomeTagMultipliers(builder,
                ModTags.Biomes.deepDarkBiomes,
                0.5,  // plant
                1   // mushroom
        );

        // Mountains: plant *0.5, but "mushroom category" (MYCELIUM + MushroomBlock) *1.5
        defineBiomeTagMultipliers(builder,
                ModTags.Biomes.mountainBiomes,
                0.5,  // plant
                1.5   // mushroom
        );

        // Desert: plant *0.5, but mushroom category *1
        defineBiomeTagMultipliers(builder,
                ModTags.Biomes.desertBiomes,
                0.5,  // plant
                1.0   // mushroom
        );

        // Nether: plant *0.5, but mushroom category *1
        defineBiomeTagMultipliers(builder,
                ModTags.Biomes.netherBiomes,
                0.5,  // plant
                1.0   // mushroom
        );

        builder.pop();

        SPEC = builder.build();
    }

    /**
     * Call this after config reload if you support live reload.
     * (E.g., from ModConfigEvent.Reloading)
     */
    public static void rebuildBiomeMultiplierCache() {
        BIOME_TAG_TO_BONEMEAL_MULTIPLIERS.clear();

        for (TagKey<Biome> tag : PLANT_MULT_VALUES.keySet()) {
            BiomeBonemealMultipliers def = DEFAULTS.get(tag);

            float plantFallback = (def != null) ? def.plant() : 1.0f;
            float mushroomFallback = (def != null) ? def.mushroom() : 1.0f;

            double plant = safeGet(PLANT_MULT_VALUES.get(tag), plantFallback);
            double mushroom = safeGet(MUSHROOM_MULT_VALUES.get(tag), mushroomFallback);

            BIOME_TAG_TO_BONEMEAL_MULTIPLIERS.put(
                    tag,
                    new BiomeBonemealMultipliers((float) plant, (float) mushroom)
            );
        }
    }
    private static float safeGet(ForgeConfigSpec.DoubleValue v, float fallback) {
        try {
            return (float)(double)v.get();
        } catch (IllegalStateException e) {
            return fallback;
        }
    }

    public record BiomeBonemealMultipliers(float plant, float mushroom) {}
}
