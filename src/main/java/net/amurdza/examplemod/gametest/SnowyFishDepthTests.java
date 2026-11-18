package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.DocumentedNaturalSpawns;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.DocumentedFauna;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SnowyFishDepthTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void deepFishBeginImmediatelyBelowTheLastRegularFishBlock(GameTestHelper helper) {
        var snowy = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        Set<String> deep = Set.of("alexscaves:tripodfish", "alexscaves:gossamer_worm",
                "minecraft:glow_squid", "hybrid_aquatic:vampire_squid", "alexsmobs:giant_squid");
        for (String biome : List.of("snowy_forest", "dappled_forest", "mountain_lake", "cold_ocean", "mushroom_caves")) {
            var water = AquaticPlantGenerationTests.fixture(helper, biome, new HashMap<>(), 40, snowy);
            // Surface water occupies Y140. Y117 has 23 cells overhead; Y116 has 24.
            for (var species : DocumentedFauna.forBiome(biome)) {
                if (!DocumentedFauna.isWaterSpecies(species.type())
                        || species.habitat() == DocumentedFauna.Habitat.BOTTOM_WATER) continue;
                boolean isDeep = deep.contains(species.id().toString());
                helper.assertTrue(species.depth() == (isDeep ? DocumentedFauna.Depth.DEEP : DocumentedFauna.Depth.SHALLOW),
                        "Snowy fish must belong to exactly one depth range: " + species.id());
                for (int y : new int[]{139, 117, 116, 102}) {
                    BlockPos pos = new BlockPos(0, y, 0);
                    boolean expected = isDeep == (y <= 116);
                    helper.assertTrue(DocumentedFauna.hasSpace(water, pos, species) == expected,
                            "Incorrect consecutive-depth eligibility for " + species.id() + " at Y" + y);
                    for (var reason : new MobSpawnType[]{MobSpawnType.NATURAL, MobSpawnType.CHUNK_GENERATION}) {
                        var event = new MobSpawnEvent.SpawnPlacementCheck(species.type(), water, reason,
                                pos, RandomSource.create(51), true);
                        DocumentedNaturalSpawns.placement(event);
                        helper.assertTrue(event.getResult() == (expected ? Event.Result.ALLOW : Event.Result.DENY),
                                "Native spawn events must enforce the same depth split: " + species.id());
                    }
                }
            }
        }
        helper.succeed();
    }
}
