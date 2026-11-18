package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.RetainedMobSpawns;
import net.amurdza.examplemod.util.RetainedContent;
import net.amurdza.examplemod.worldgen.DocumentedFauna;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RetainedMobTests {
    private static final String TEMPLATE = "plant_gameplay_empty";

    private RetainedMobTests() { }

    @GameTest(template = TEMPLATE)
    public static void unusedDependencyMobsCannotEnterWorldsOrCreative(GameTestHelper helper) {
        for (String id : List.of("alexsmobs:tarantula_hawk", "alexsmobs:farseer", "quark:wraith",
                "quark:forgotten", "quark:stoneling", "twilightforest:naga", "twilightforest:lich",
                "twilightforest:lower_goblin_knight", "wildernature:flamingo",
                "alexsmobs:anaconda", "alexsmobs:anaconda_part", "alexsmobs:centipede_head",
                "alexsmobs:centipede_body", "alexsmobs:centipede_tail",
                "ambientadditions:red_river_hog", "alexsmobs:warped_toad", "orcz:orcshieldiron", "orcz:savageorc")) {
            Entity entity = create(helper, id);
            helper.assertFalse(RetainedContent.isKeptEntity(entity.getType()), "Unused species remains selected: " + id);
            for (boolean loadedFromDisk : List.of(false, true)) {
                var event = new EntityJoinLevelEvent(entity, helper.getLevel(), loadedFromDisk);
                RetainedMobSpawns.joined(event);
                helper.assertTrue(event.isCanceled(), "Unused species bypassed spawn/load filtering: " + id);
            }
            for (var item : ForgeRegistries.ITEMS.getValues()) {
                if (item instanceof SpawnEggItem egg && egg.getType(null) == entity.getType()) {
                    helper.assertFalse(RetainedContent.isKeptItem(new ItemStack(item)),
                            "Unused species still has a creative spawn egg: " + id);
                }
            }
            entity.discard();
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void retainedCreaturesBossesAndTheirHelpersStillEnterWorlds(GameTestHelper helper) {
        for (String id : List.of("minecraft:cow", "twilightforest:slime_beetle",
                "alexsmobs:moose", "outer_end:stalker",
                "twilightforest:alpha_yeti", "twilightforest:upper_goblin_knight",
                "twilightforest:thrown_ice", "twilightforest:falling_ice",
                "orcz:orcshield", "orcz:orcsavagemace", "orcz:bigorcboss")) {
            Entity entity = create(helper, id);
            var event = new EntityJoinLevelEvent(entity, helper.getLevel());
            MinecraftForge.EVENT_BUS.post(event);
            helper.assertFalse(event.isCanceled(), "Required creature or helper was removed: " + id);
            entity.discard();
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void everyEffectiveDimensionSpawnEntryRemainsSelected(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        Set<ResourceLocation> used = new HashSet<>();
        for (var entry : biomes.entrySet()) {
            var id = entry.getKey().location();
            if (!id.getNamespace().equals(AOEMod.MOD_ID) || !DocumentedFauna.hasBiome(id.getPath())) continue;
            for (MobCategory category : MobCategory.values()) {
                for (var spawn : entry.getValue().getMobSettings().getMobs(category).unwrap()) {
                    used.add(ForgeRegistries.ENTITY_TYPES.getKey(spawn.type));
                    helper.assertTrue(RetainedContent.isKeptEntity(spawn.type),
                            "Dimension roster references a removed species: " + id + ": "
                                    + ForgeRegistries.ENTITY_TYPES.getKey(spawn.type));
                }
            }
        }
        // Fixed island/mound groups and structure bosses do not use biome spawn
        // tables.
        for (String id : List.of("alexsmobs:gorilla", "alexsmobs:elephant", "biodiverse:cassowary",
                "biodiverse:capybara", "alexsmobs:moose", "alexsmobs:gazelle", "twilightforest:alpha_yeti", "orcz:bigorcboss")) {
            used.add(new ResourceLocation(id));
        }
        for (var id : ForgeRegistries.ENTITY_TYPES.getKeys()) {
            if (id.getNamespace().equals("minecraft") || id.getNamespace().equals(AOEMod.MOD_ID)) continue;
            if (RetainedContent.isKeptEntity(id)) helper.assertTrue(used.contains(id),
                    "Retained dependency species has no biome, structure, island or multipart source: " + id);
        }
        helper.succeed();
    }

    private static Entity create(GameTestHelper helper, String id) {
        var key = new ResourceLocation(id);
        helper.assertTrue(ForgeRegistries.ENTITY_TYPES.containsKey(key), "Missing regression fixture: " + id);
        Entity entity = ForgeRegistries.ENTITY_TYPES.getValue(key).create(helper.getLevel());
        helper.assertTrue(entity != null, "Cannot construct regression fixture: " + id);
        return entity;
    }
}
