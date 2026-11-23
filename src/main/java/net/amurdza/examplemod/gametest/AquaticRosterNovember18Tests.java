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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AquaticRosterNovember18Tests {
    @GameTest(template = "plant_gameplay_empty")
    public static void removedSpeciesCannotJoinAndTheirFoodsHaveNoRecipes(GameTestHelper helper) {
        Set<String> removedAnimalItems = new HashSet<>();
        for (String id : List.of("finsandtails:rubber_belly_glider",
                "upgrade_aquatic:lionfish", "hybrid_aquatic:carp", "hybrid_aquatic:lightfoot_crab",
                "hybrid_aquatic:flower_crab", "ecologics:coconut_crab")) {
            var key = new ResourceLocation(id);
            var type = ForgeRegistries.ENTITY_TYPES.getValue(key);
            helper.assertTrue(type != null && !RetainedContent.isKeptEntity(type), "Removed dependency species remains selected: " + id);
            var mob = type.create(helper.getLevel());
            var joined = new EntityJoinLevelEvent(mob, helper.getLevel(), true);
            RetainedMobSpawns.joined(joined);
            helper.assertTrue(joined.isCanceled(), "Removed species can return from eggs, summons or saves: " + id);
            mob.discard();
            for (var entry : ForgeRegistries.ITEMS.getEntries()) {
                var itemId = entry.getKey().location();
                if (itemId.getNamespace().equals(key.getNamespace()) && itemId.getPath().contains(key.getPath())) {
                    removedAnimalItems.add(itemId.toString());
                    var stack = new ItemStack(entry.getValue());
                    helper.assertTrue(RetainedContent.isExplicitlyRemovedItem(itemId)
                                    && !RetainedContent.isKeptItem(stack) && !RetainedContent.isSelectedItem(stack),
                            "Removed animal item can return through recipes or creative: " + itemId);
                }
            }
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
        Set<String> removedFoods = new HashSet<>(Set.of("aoemod:raw_mahi_mahi", "aoemod:cooked_mahi_mahi",
                "aoemod:raw_rubber_belly_glider", "aoemod:cooked_rubber_belly_glider", "aoemod:cooked_carp",
                "hybrid_aquatic:mahi", "hybrid_aquatic:cooked_mahi", "hybrid_aquatic:carp", "hybrid_aquatic:cooked_carp",
                "finsandtails:rubber_belly_glider", "upgrade_aquatic:lionfish", "upgrade_aquatic:cooked_lionfish"));
        removedFoods.addAll(removedAnimalItems);
        for (var recipe : helper.getLevel().getRecipeManager().getRecipes()) {
            var result = recipe.getResultItem(helper.getLevel().registryAccess());
            helper.assertFalse(removedFoods.contains(String.valueOf(ForgeRegistries.ITEMS.getKey(result.getItem()))),
                    "Recipe recreates a removed aquatic food: " + recipe.getId());
            for (var ingredient : recipe.getIngredients()) {
                var choices = ingredient.getItems();
                helper.assertFalse(choices.length > 0 && Arrays.stream(choices).allMatch(stack ->
                                removedFoods.contains(String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem())))),
                        "Recipe still requires a removed aquatic food: " + recipe.getId());
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void lakeAndTropicalLoadedRostersMatchTheirNewAnimals(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        for (String biome : List.of("plains_lake", "savanna_lake")) {
            Set<String> expected = Set.of("minecraft:cod", "minecraft:salmon",
                    "hybrid_aquatic:octopus", "alexsmobs:lobster");
            var configured = DocumentedFauna.forBiome(biome).stream().map(s -> s.id().toString()).collect(Collectors.toSet());
            helper.assertTrue(configured.equals(expected), "Lake roster differs: " + biome + "/" + configured);
            helper.assertTrue(DocumentedFauna.forBiome(biome).stream().anyMatch(s -> s.id().toString().equals("alexsmobs:lobster")
                            && s.habitat() == DocumentedFauna.Habitat.BOTTOM_WATER && s.depth() == DocumentedFauna.Depth.ANY),
                    "Plains and savanna lobsters need unrestricted lake-floor spawning: " + biome);
            var loaded = registry.get(new ResourceLocation(AOEMod.MOD_ID, biome));
            var aquatic = Arrays.stream(MobCategory.values()).flatMap(c -> loaded.getMobSettings().getMobs(c).unwrap().stream())
                    .filter(s -> DocumentedFauna.isWaterSpecies(s.type))
                    .map(s -> ForgeRegistries.ENTITY_TYPES.getKey(s.type).toString()).collect(Collectors.toSet());
            helper.assertTrue(aquatic.equals(expected), "Loaded lake roster differs: " + biome + "/" + aquatic);
        }
        for (String biome : List.of("coral_reef", "mangrove_swamp")) {
            var roster = DocumentedFauna.forBiome(biome);
            helper.assertTrue(roster.stream().anyMatch(s -> s.id().toString().equals("hybrid_aquatic:starfish")
                            && s.habitat() == DocumentedFauna.Habitat.BOTTOM_WATER
                            && DocumentedFauna.spawnCategory(s) == MobCategory.WATER_AMBIENT),
                    "Starfish must remain ambient seabed animals");
            var loaded = registry.get(new ResourceLocation(AOEMod.MOD_ID, biome));
            helper.assertTrue(loaded.getMobSettings().getMobs(MobCategory.WATER_AMBIENT).unwrap().stream()
                            .anyMatch(s -> ForgeRegistries.ENTITY_TYPES.getKey(s.type).toString().equals("hybrid_aquatic:starfish")),
                    "Loaded tropical biomes must keep starfish in the ambient pool");
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
