package net.amurdza.examplemod.gametest;

import com.mojang.authlib.GameProfile;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.entity.future.NautilusEntity;
import net.amurdza.examplemod.event_handlers.RetainedMobSpawns;
import net.amurdza.examplemod.registry.ModEntities;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.RetainedContent;
import net.amurdza.examplemod.worldgen.DocumentedFauna;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LocalMobCapCalculator;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AquaticNovember22Tests {
    @GameTest(template = "plant_gameplay_empty")
    public static void retiredAquaticsAndMeatsCannotReturnThroughSpawningRecipesOrFishing(GameTestHelper helper) {
        helper.assertFalse(ModList.get().isLoaded("angling"), "Angling must be removed from the actual loaded pack");
        helper.assertTrue(ForgeRegistries.ENTITY_TYPES.getKeys().stream().noneMatch(id -> id.getNamespace().equals("angling"))
                        && ForgeRegistries.ITEMS.getKeys().stream().noneMatch(id -> id.getNamespace().equals("angling")),
                "No Angling entity or food registration may survive removal");
        for (String id : List.of("finsandtails:swamp_mucker", "alexsmobs:giant_squid")) {
            var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id));
            helper.assertTrue(type != null && !RetainedContent.isKeptEntity(type), "Retired species remains selected: " + id);
            var mob = type.create(helper.getLevel());
            var event = new EntityJoinLevelEvent(mob, helper.getLevel(), true);
            RetainedMobSpawns.joined(event);
            helper.assertTrue(event.isCanceled(), "Saved, summoned or egg-spawned retired animal can rejoin: " + id);
            mob.discard();
        }
        Set<String> retiredFoods = Set.of("aoemod:raw_anomalocaris", "aoemod:cooked_anomalocaris",
                "aoemod:cooked_swamp_mucker", "aoemod:cooked_lost_tentacle",
                "finsandtails:swamp_mucker", "alexsmobs:lost_tentacle");
        for (String id : retiredFoods) {
            var key = new ResourceLocation(id);
            helper.assertTrue(RetainedContent.isExplicitlyRemovedItem(key), "Retired food needs an explicit exclusion: " + id);
            if (key.getNamespace().equals(AOEMod.MOD_ID)) {
                helper.assertFalse(ForgeRegistries.ITEMS.containsKey(key), "Retired AoE food is still registered: " + id);
            } else {
                helper.assertFalse(RetainedContent.isKeptItem(new ItemStack(ForgeRegistries.ITEMS.getValue(key))),
                        "Retired native meat is still available in creative: " + id);
            }
        }
        helper.assertFalse(ForgeRegistries.ITEMS.getValue(new ResourceLocation("alexsmobs:lost_tentacle")).isEdible(),
                "The mod must no longer turn legacy native tentacles into giant-squid meat");
        for (var recipe : helper.getLevel().getRecipeManager().getRecipes()) {
            helper.assertFalse(retiredFoods.contains(String.valueOf(ForgeRegistries.ITEMS.getKey(
                            recipe.getResultItem(helper.getLevel().registryAccess()).getItem()))),
                    "Recipe creates retired meat: " + recipe.getId());
            for (var ingredient : recipe.getIngredients()) {
                var choices = ingredient.getItems();
                helper.assertFalse(choices.length > 0 && Arrays.stream(choices).allMatch(stack -> retiredFoods.contains(
                                String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem())))),
                        "Recipe still depends on retired meat: " + recipe.getId());
            }
        }
        var table = helper.getLevel().getServer().getLootData().getLootTable(new ResourceLocation("finsandtails:inject/fishing"));
        helper.assertTrue(table != LootTable.EMPTY, "The native fishing table must remain available");
        var params = new LootParams.Builder(helper.getLevel()).withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD)).create(LootContextParamSets.FISHING);
        for (long seed = 1; seed <= 1024; seed++) {
            helper.assertTrue(table.getRandomItems(params, seed * 0x9E3779B97F4A7C15L).stream().noneMatch(stack ->
                            retiredFoods.contains(String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem())))),
                    "Fishing must not recreate swamp-mucker meat");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void wildNautiliStayInWaterCreatureCensusWhenPlayersLeaveAndAfterReload(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "NautilusCensus"));
        var wild = ModEntities.NAUTILUS.get().create(level);
        var restored = ModEntities.NAUTILUS.get().create(level);
        var owned = ModEntities.NAUTILUS.get().create(level);
        try {
            level.players().add(player);
            wild.setPos(0, 100, 0);
            owned.tame(player);
            owned.setPos(0, 100, 0);
            CompoundTag saved = new CompoundTag();
            wild.saveWithoutId(saved);
            restored.load(saved);
            helper.assertFalse(wild.removeWhenFarAway(1000000), "Wild nautili retain their existing permanent residency");
            helper.assertTrue(owned.requiresCustomPersistence(), "Owned nautili remain exempt from the natural cap");
            for (int distance : new int[]{32, 128, 129, 512}) {
                player.setPos(distance, 100, 0);
                var chunk = level.getChunkAt(new BlockPos(0, 100, 0));
                var counts = NaturalSpawner.createState(289, new ArrayList<Entity>(List.of(wild, restored, owned)),
                        (key, consumer) -> consumer.accept(chunk), new LocalMobCapCalculator(level.getChunkSource().chunkMap))
                        .getMobCategoryCounts();
                helper.assertTrue(counts.getInt(MobCategory.WATER_CREATURE) == 2
                                && counts.getInt(MobCategory.CREATURE) == 0 && counts.getInt(MobCategory.WATER_AMBIENT) == 0,
                        "Both wild residents must keep charging WATER_CREATURE at distance " + distance + ": " + counts);
            }
            for (String biome : List.of("cold_ocean", "mountain_lake")) {
                var loaded = level.registryAccess().registryOrThrow(Registries.BIOME).get(AOEMod.makeID(biome));
                helper.assertTrue(loaded.getMobSettings().getMobs(MobCategory.WATER_CREATURE).unwrap().stream()
                                .anyMatch(entry -> entry.type == ModEntities.NAUTILUS.get())
                                && DocumentedFauna.spawnCategory(wild, MobCategory.CREATURE) == MobCategory.WATER_CREATURE,
                        "Nautilus candidates and existing residents must charge the same pool in " + biome);
            }
        } finally {
            level.players().remove(player);
            player.discard(); wild.discard(); restored.discard(); owned.discard();
        }
        helper.succeed();
    }
}
