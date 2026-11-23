package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.config.MobConfig;
import net.amurdza.examplemod.event_handlers.CustomMobDrops;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.RetainedContent;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AquaticLootRevisionTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void lavaFishReplaceNativeLootWithExactRequestedResources(GameTestHelper helper) {
        var nether = helper.getLevel().getServer().getLevel(GameplayDimensions.NETHER);
        var expected = Map.of(
                "netherdepthsupgrade:blazefish", Set.of(Items.BLAZE_ROD),
                "netherdepthsupgrade:glowdine", Set.of(Items.GLOWSTONE_DUST),
                "netherdepthsupgrade:magmacubefish", Set.of(Items.MAGMA_CREAM),
                "aoemod:cubozoa", Set.of(Items.SLIME_BALL),
                "netherdepthsupgrade:wither_bonefish", Set.of(Items.BONE, Items.COAL));
        for (var entry : expected.entrySet()) {
            var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(entry.getKey()));
            helper.assertTrue(type != null && RetainedContent.isKeptEntity(type), "Lava fauna must survive filtering: " + entry.getKey());
            Mob mob = (Mob) type.create(nether);
            try {
                var rows = MobConfig.mobDropEntries(mob);
                helper.assertTrue(rows.size() == entry.getValue().size(), "Only requested resources should be configured");
                for (var row : rows) {
                    helper.assertTrue(entry.getValue().contains(row.item()) && row.soulSandValley() == 1
                                    && row.warpedForest() == 1 && row.crimsonForest() == 1 && row.basaltDeltas() == 1
                                    && !row.affectedByLooting(),
                            "Each Nether biome must yield exactly one resource, including with Looting: " + entry.getKey());
                }
                for (int looting : new int[]{0, 3}) {
                    var drops = new LivingDropsEvent(mob, nether.damageSources().generic(), new ArrayList<>(), looting, true);
                    CustomMobDrops.addConfiguredDrops(drops);
                    helper.assertTrue(drops.getDrops().stream().allMatch(drop -> drop.getItem().getCount() == 1)
                                    && drops.getDrops().stream().map(drop -> drop.getItem().getItem()).collect(Collectors.toSet()).equals(entry.getValue()),
                            "Actual death handler must return exactly the requested stack(s): " + entry.getKey());
                    drops.getDrops().forEach(drop -> drop.discard());
                }
            } finally { mob.discard(); }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void retainedCrabsUseEcologicsMeatAndStarfishHaveCookedReward(GameTestHelper helper) {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var meat = ForgeRegistries.ITEMS.getValue(new ResourceLocation("ecologics:crab_meat"));
        var cookedMeat = ForgeRegistries.ITEMS.getValue(new ResourceLocation("aoemod:cooked_crab_meat"));
        for (String name : new String[]{"fiddler_crab", "spider_crab"}) {
            Mob crab = (Mob) ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("hybrid_aquatic", name)).create(tropical);
            var rows = MobConfig.mobDropEntries(crab);
            helper.assertTrue(rows.size() == 1 && rows.get(0).item() == meat && rows.get(0).cookedItem() == cookedMeat,
                    "Retired claws must be replaced by Ecologics meat and its cooked counterpart: " + name);
            crab.discard();
        }
        Mob starfish = (Mob) ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("hybrid_aquatic:starfish")).create(tropical);
        var row = MobConfig.mobDropEntries(starfish).get(0);
        helper.assertTrue(row.tropical() == 1 && row.item() == ForgeRegistries.ITEMS.getValue(new ResourceLocation("hybrid_aquatic:starfish"))
                        && row.cookedItem() == ForgeRegistries.ITEMS.getValue(new ResourceLocation("aoemod:cooked_starfish")),
                "Starfish deaths need the raw food and its cooked fire-kill counterpart");
        starfish.discard();
        helper.succeed();
    }
}
