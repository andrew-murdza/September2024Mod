package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.config.MobConfig;
import net.amurdza.examplemod.config.MobStatsConfig;
import net.amurdza.examplemod.event_handlers.CustomMobDrops;
import net.amurdza.examplemod.event_handlers.TwinsAndFailedBreeding;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.Helper;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SnowyMobRateTests {
    private static final List<String> BIOMES = List.of("snowy_forest", "dappled_forest", "cold_ocean",
            "mountain_lake", "mushroom_caves", "dripstone_caves");

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 800)
    public static void allSnowyBiomesShareEachMobRateWithoutBorrowingOtherDimensions(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        var animals = new ArrayList<Entity>();
        var probe = EntityType.MOOSHROOM.create(level);
        try {
            for (var entry : MobConfig.MOB_INFO_BY_MOB.entrySet()) {
                if (entry.getValue().aging() == null && entry.getValue().breeding() == null) continue;
                var entity = entry.getKey().create(level);
                helper.assertTrue(entity != null, "Configured animal needs a usable entity: " + entry.getKey());
                animals.add(entity);
            }
            forEachSnowyBiome(helper, level, new BlockPos(2388, -24, 2388), (name, position) -> {
                move(probe, position);
                for (var animal : animals) {
                    move(animal, position);
                    var info = MobConfig.MOB_INFO_BY_MOB.get(animal.getType());
                    float growth = info.aging() == null ? 1 : info.aging().mountains();
                    float breeding = info.breeding() == null ? -1 : info.breeding().mountains();
                    helper.assertTrue(MobConfig.mobGrowthChance(animal) == growth
                                    && MobConfig.mobTwinsChance(animal) == breeding
                                    && MobConfig.mobTwinsChance(animal, probe) == breeding,
                            "Aging and offspring must use the Snowy entry in " + name + " for " + animal.getType());
                }
                for (var info : MobConfig.MOB_INFO_BY_MOB.values()) {
                    for (var row : info.drops()) {
                        helper.assertTrue(MobConfig.mobAmount(probe, row) == row.mountains(),
                                "A resource yield must retain its own Snowy maximum in " + name + ": " + row.item());
                    }
                }
                // Rabbit growth is faster in Savanna (3) than Snowy (0.8).
                // Sharing Snowy rates must not take a maximum across dimensions.
                var rabbit = animals.stream().filter(entity -> entity.getType() == EntityType.RABBIT).findFirst().orElseThrow();
                var pig = animals.stream().filter(entity -> entity.getType() == EntityType.PIG).findFirst().orElseThrow();
                helper.assertTrue(MobConfig.mobGrowthChance(rabbit) == 0.8F && MobConfig.mobTwinsChance(rabbit) == 1,
                        "Snowy rabbits must retain their Snowy rates, not another dimension's higher scores");
                helper.assertTrue(MobConfig.mobGrowthChance(pig) == 0 && MobConfig.mobTwinsChance(pig) == 0,
                        "An animal unsupported in every Snowy biome must remain unsupported");
                for (var rates : numericMobRates()) {
                    Number expected = rates.get(ModTags.Biomes.mountainBiomes);
                    Number actual = Helper.getBiomeValue(level, position, rates);
                    helper.assertTrue(expected != null && actual != null && actual.doubleValue() == expected.doubleValue(),
                            "Equipment yields and mount stats must share their Snowy value in " + name);
                }
            });
        } finally {
            animals.forEach(Entity::discard);
            probe.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 800)
    public static void snowyBreedingFoodAgingAndDropsWorkInEveryHabitat(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SNOWY);
        forEachSnowyBiome(helper, level, new BlockPos(2389, -24, 2389), (name, position) -> {
            var parent = EntityType.MOOSHROOM.create(level);
            var mate = EntityType.MOOSHROOM.create(level);
            var pig = EntityType.PIG.create(level);
            var pigMate = EntityType.PIG.create(level);
            move(parent, position); move(mate, position); move(pig, position); move(pigMate, position);
            var child = parent.getBreedOffspring(level, mate);
            var pigChild = pig.getBreedOffspring(level, pigMate);
            helper.assertTrue(child != null && pigChild != null, "The breeding fixture needs real offspring");
            try {
                move(child, position); move(pigChild, position);
                parent.setNoAi(true);
                parent.setAge(100);
                parent.aiStep();
                helper.assertTrue(parent.getAge() == 99, "Breeding cooldown must use the shared Snowy aging score in " + name);
                parent.setAge(0);
                child.setNoAi(true);
                child.setAge(-100);
                child.aiStep();
                helper.assertTrue(child.getAge() == -99, "Mooshrooms must age naturally in " + name);
                child.ageUp(2, false);
                helper.assertTrue(child.getAge() == -59, "Food must apply the shared Snowy growth score in " + name);
                var birth = new BabyEntitySpawnEvent(parent, mate, child);
                TwinsAndFailedBreeding.twinsOrInfertile(birth);
                helper.assertTrue(!birth.isCanceled() && birth.getChild().getType() == EntityType.MOOSHROOM,
                        "The breeding handler must allow Snowy mooshroom offspring in " + name);

                pigChild.setNoAi(true);
                pigChild.setAge(-100);
                pigChild.aiStep();
                pigChild.ageUp(2, false);
                var blockedBirth = new BabyEntitySpawnEvent(pig, pigMate, pigChild);
                TwinsAndFailedBreeding.twinsOrInfertile(blockedBirth);
                helper.assertTrue(pigChild.getAge() == -100 && blockedBirth.isCanceled(),
                        "Sharing Snowy habitats must preserve zero growth and breeding for pigs in " + name);

                var drops = new LivingDropsEvent(parent, level.damageSources().generic(), new ArrayList<>(), 0, true);
                try {
                    CustomMobDrops.addConfiguredDrops(drops);
                    var counts = new HashMap<Item, Integer>();
                    drops.getDrops().forEach(drop -> counts.merge(drop.getItem().getItem(), drop.getItem().getCount(), Integer::sum));
                    helper.assertTrue(counts.equals(Map.of(Items.BEEF, 2, Items.LEATHER, 1)),
                            "Actual death drops must keep the same Snowy yields in " + name + ": " + counts);
                } finally { drops.getDrops().forEach(Entity::discard); }
            } finally {
                parent.discard(); mate.discard(); child.discard();
                pig.discard(); pigMate.discard(); pigChild.discard();
            }
        });
        helper.succeed();
    }

    private static List<Map<TagKey<Biome>, ? extends Number>> numericMobRates() {
        return List.of(MobConfig.BIOME_TO_NUGGETS_FROM_ARMOR,
                MobStatsConfig.BIOME_TO_LLAMA_HEALTH, MobStatsConfig.BIOME_TO_HORSE_HEALTH,
                MobStatsConfig.BIOME_TO_DONKEY_HEALTH, MobStatsConfig.BIOME_TO_MULE_HEALTH,
                MobStatsConfig.BIOME_TO_LLAMA_SPEED, MobStatsConfig.BIOME_TO_HORSE_SPEED,
                MobStatsConfig.BIOME_TO_DONKEY_SPEED, MobStatsConfig.BIOME_TO_MULE_SPEED,
                MobStatsConfig.BIOME_TO_HORSE_JUMP_STRENGTH, MobStatsConfig.BIOME_TO_DONKEY_JUMP_STRENGTH,
                MobStatsConfig.BIOME_TO_MULE_JUMP_STRENGTH);
    }

    private static void move(Entity entity, BlockPos position) {
        entity.moveTo(position.getX() + 0.5, position.getY(), position.getZ() + 0.5);
    }

    private static void forEachSnowyBiome(GameTestHelper helper, ServerLevel level, BlockPos position,
                                         BiConsumer<String, BlockPos> check) {
        var chunk = level.getChunkAt(position);
        var saved = new HashMap<BlockPos, Holder<Biome>>();
        int minX = QuartPos.fromBlock(chunk.getPos().getMinBlockX());
        int minZ = QuartPos.fromBlock(chunk.getPos().getMinBlockZ());
        for (int x = minX; x < minX + 4; x++) for (int z = minZ; z < minZ + 4; z++)
            for (int y = QuartPos.fromBlock(level.getMinBuildHeight()); y < QuartPos.fromBlock(level.getMaxBuildHeight()); y++)
                saved.put(new BlockPos(x, y, z), chunk.getNoiseBiome(x, y, z));
        try {
            for (var name : BIOMES) {
                var key = ResourceKey.create(Registries.BIOME, AOEMod.makeID(name));
                var biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(key);
                helper.assertTrue(biome.is(ModTags.Biomes.mountainBiomes), "Every registered Snowy biome needs the shared mob-rate tag: " + name);
                // Below y=0 the exact runtime biome lookup reads the chunk palette,
                // allowing legacy habitats too without changing surface-column data.
                chunk.fillBiomesFromNoise((x, y, z, sampler) -> biome, level.getChunkSource().randomState().sampler());
                helper.assertTrue(level.getBiome(position).is(key), "The real mob lookup must see its fixture biome: " + name);
                check.accept(name, position);
            }
        } finally {
            chunk.fillBiomesFromNoise((x, y, z, sampler) -> saved.get(new BlockPos(x, y, z)),
                    level.getChunkSource().randomState().sampler());
        }
    }
}
