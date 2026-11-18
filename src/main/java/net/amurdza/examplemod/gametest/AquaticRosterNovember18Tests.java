package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.RetainedMobSpawns;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.RetainedContent;
import net.amurdza.examplemod.worldgen.DocumentedFauna;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AquaticRosterNovember18Tests {
    @GameTest(template = "plant_gameplay_empty")
    public static void removedSpeciesCannotJoinAndTheirFoodsHaveNoRecipes(GameTestHelper helper) {
        for (String id : List.of("angling:mahi_mahi", "finsandtails:rubber_belly_glider",
                "upgrade_aquatic:lionfish", "hybrid_aquatic:carp")) {
            var key = new ResourceLocation(id);
            var type = ForgeRegistries.ENTITY_TYPES.getValue(key);
            helper.assertTrue(type != null && !RetainedContent.isKeptEntity(type), "Removed dependency species remains selected: " + id);
            var mob = type.create(helper.getLevel());
            var joined = new EntityJoinLevelEvent(mob, helper.getLevel(), true);
            RetainedMobSpawns.joined(joined);
            helper.assertTrue(joined.isCanceled(), "Removed species can return from eggs, summons or saves: " + id);
            mob.discard();
        }
        for (String name : List.of("raw_mahi_mahi", "cooked_mahi_mahi", "raw_rubber_belly_glider",
                "cooked_rubber_belly_glider", "cooked_carp")) {
            helper.assertFalse(ForgeRegistries.ITEMS.containsKey(new ResourceLocation(AOEMod.MOD_ID, name)),
                    "Retired AoE food must no longer be registered: " + name);
        }
        for (String id : List.of("hybrid_aquatic:mahi", "hybrid_aquatic:carp", "upgrade_aquatic:lionfish",
                "upgrade_aquatic:cooked_lionfish")) {
            var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
            helper.assertFalse(RetainedContent.isKeptItem(new ItemStack(item)), "Removed food leaked into catalog: " + id);
        }
        for (var recipe : helper.getLevel().getRecipeManager().getRecipes()) {
            var result = recipe.getResultItem(helper.getLevel().registryAccess());
            helper.assertFalse(RetainedContent.isExplicitlyRemovedItem(ForgeRegistries.ITEMS.getKey(result.getItem())),
                    "Recipe recreates a removed item: " + recipe.getId());
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void lakeAndTropicalLoadedRostersMatchTheirNewAnimals(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        for (String biome : List.of("plains_lake", "savanna_lake")) {
            Set<String> expected = Set.of("minecraft:cod", "minecraft:salmon", "finsandtails:swamp_mucker",
                    "hybrid_aquatic:octopus", "hybrid_aquatic:flower_crab");
            var configured = DocumentedFauna.forBiome(biome).stream().map(s -> s.id().toString()).collect(Collectors.toSet());
            helper.assertTrue(configured.equals(expected), "Lake roster differs: " + biome + "/" + configured);
            var loaded = registry.get(new ResourceLocation(AOEMod.MOD_ID, biome));
            var aquatic = Arrays.stream(MobCategory.values()).flatMap(c -> loaded.getMobSettings().getMobs(c).unwrap().stream())
                    .filter(s -> DocumentedFauna.isWaterSpecies(s.type))
                    .map(s -> ForgeRegistries.ENTITY_TYPES.getKey(s.type).toString()).collect(Collectors.toSet());
            helper.assertTrue(aquatic.equals(expected), "Loaded lake roster differs: " + biome + "/" + aquatic);
        }
        for (String biome : List.of("coral_reef", "mangrove_swamp")) {
            var roster = DocumentedFauna.forBiome(biome);
            helper.assertTrue(roster.stream().anyMatch(s -> s.id().toString().equals("hybrid_aquatic:coconut_crab")
                            && s.habitat() == DocumentedFauna.Habitat.BOTTOM_WATER), "Coconut crab needs a seabed spawn entry");
            helper.assertTrue(roster.stream().anyMatch(s -> s.id().toString().equals("hybrid_aquatic:umbrella_octopus")),
                    "Umbrella octopus must remain tropical");
            helper.assertTrue(roster.stream().noneMatch(s -> s.id().toString().equals("hybrid_aquatic:octopus")),
                    "Ordinary octopus must leave Tropical");
        }
        helper.assertTrue(RetainedContent.isKeptItem(new ItemStack(ForgeRegistries.ITEMS.getValue(
                new ResourceLocation("biodiverse:cassowary_egg")))), "Laid cassowary eggs must remain obtainable and visible");
        helper.succeed();
    }
}
