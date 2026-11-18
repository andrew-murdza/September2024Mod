package net.amurdza.examplemod.gametest;

import com.mojang.serialization.JsonOps;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.RemovedOnionAcquisition;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.BasicItemListing;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import vectorwing.farmersdelight.common.event.VillagerEvents;
import vectorwing.farmersdelight.common.registry.ModItems;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OnionAcquisitionTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void loadedChestsAndForagingCannotSupplyOnions(GameTestHelper helper) {
        var level = helper.getLevel();
        var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .create(LootContextParamSets.CHEST);
        var observed = new HashSet<String>();
        for (String path : List.of("farmersdelight:chests/fd_pillager_outpost",
                "farmersdelight:chests/fd_shipwreck_supply", "farmersdelight:chests/fd_village_plains_house",
                "farmersdelight:chests/fd_village_snowy_house", "hearthandharvest:chests/lilliput_lane/larder",
                "hearthandharvest:chests/lilliput_lane/pantry")) {
            var table = level.getServer().getLootData().getLootTable(new ResourceLocation(path));
            for (int seed = 1; seed <= 64; seed++) {
                for (ItemStack stack : table.getRandomItems(params, seed)) {
                    String item = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
                    helper.assertFalse(item.contains("onion"), "Native chest still supplies onions: " + path);
                    observed.add(item);
                }
            }
        }
        helper.assertTrue(observed.containsAll(List.of("farmersdelight:tomato_seeds", "farmersdelight:cabbage_seeds",
                        "farmersdelight:rice", "farmersdelight:rope", "minecraft:carrot", "hearthandharvest:moonshine")),
                "Unrelated native chest drops must survive the onion removal: " + observed);
        var onion = new ItemStack(ModItems.ONION.get());
        for (String tag : List.of("minecraft:villager_plantable_seeds", "wildernature:can_be_truffled")) {
            helper.assertFalse(onion.is(TagKey.create(Registries.ITEM, new ResourceLocation(tag))),
                    "Onions still enter farming or foraging through " + tag);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void nativeVillageFarmsAndTradesExcludeOnlyOnions(GameTestHelper helper) {
        var trades = new ArrayList<VillagerTrades.ItemListing>();
        trades.add(VillagerEvents.itemForEmeraldTrade(ModItems.ONION.get(), 1, 12));
        trades.add(VillagerEvents.emeraldForItemsTrade(ModItems.ONION.get(), 26, 16, 2));
        var tomatoSale = VillagerEvents.itemForEmeraldTrade(ModItems.TOMATO_SEEDS.get(), 1, 12);
        var cabbagePurchase = VillagerEvents.emeraldForItemsTrade(ModItems.CABBAGE.get(), 16, 16, 5);
        trades.add(tomatoSale);
        trades.add(cabbagePurchase);
        RemovedOnionAcquisition.removeOnionTrades(trades);
        helper.assertTrue(trades.equals(List.of(tomatoSale, cabbagePurchase)),
                "Native onion trades must be removed while the other crop offers retain their identity");
        for (var listing : trades) {
            helper.assertTrue(listing.getClass() == BasicItemListing.class
                            && listing.getOffer(null, RandomSource.create(0)) != null,
                    "Remaining native crop trades must still produce offers");
        }
        var access = helper.getLevel().registryAccess();
        var processors = access.registryOrThrow(Registries.PROCESSOR_LIST);
        var ops = RegistryOps.create(JsonOps.INSTANCE, access);
        for (String biome : List.of("plains", "snowy", "taiga", "savanna", "desert")) {
            var farm = processors.get(new ResourceLocation("minecraft", "farm_" + biome));
            helper.assertTrue(farm != null, "Village farm processors remain registered: " + biome);
            boolean hasOtherCrop = false;
            for (var processor : farm.list()) {
                if (processor instanceof RuleProcessor rules) {
                    String json = RuleProcessor.CODEC.encodeStart(ops, rules).result().orElseThrow().toString();
                    helper.assertFalse(json.contains("farmersdelight:onions"), "Native village farm regrows onions: " + biome);
                    hasOtherCrop |= json.contains("farmersdelight:tomatoes") || json.contains("farmersdelight:cabbages");
                }
            }
            if (vectorwing.farmersdelight.common.Configuration.GENERATE_VILLAGE_FARM_FD_CROPS.get()) {
                helper.assertTrue(hasOtherCrop, "Other Farmer's Delight crops must remain in village farms: " + biome);
            }
        }
        helper.succeed();
    }
}
