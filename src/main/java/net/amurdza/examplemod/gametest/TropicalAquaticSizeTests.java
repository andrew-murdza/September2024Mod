package net.amurdza.examplemod.gametest;

import dev.hybridlabs.aquatic.entity.critter.StarfishEntity;
import dev.hybridlabs.aquatic.entity.critter.StarfishEntity.Companion.Type;
import dev.hybridlabs.hapi.entity.base.aquatic.BaseWaterAnimal;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.DocumentedNaturalSpawns;
import net.amurdza.examplemod.util.AquaticMobSizes;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TropicalAquaticSizeTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 200)
    public static void everyHybridAquaticSpeciesKeepsItsMaximumAcrossOldDataAndRandomWrites(GameTestHelper helper) {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        int checked = 0;
        for (var type : ForgeRegistries.ENTITY_TYPES.getValues()) {
            if (!ForgeRegistries.ENTITY_TYPES.getKey(type).getNamespace().equals("hybrid_aquatic")
                    || type.getCategory() == MobCategory.MISC) continue;
            if (!(type.create(tropical) instanceof BaseWaterAnimal animal)) continue;
            if (AquaticMobSizes.usesNativeSize(animal)) continue;
            checked++;
            int maximum = AquaticMobSizes.maximum(animal);
            helper.assertTrue(animal.getSize() == maximum, "Newly constructed animal must already render at maximum: " + type);
            for (int input : new int[]{-100, -5, 0, 3, 20}) {
                animal.setSize(input);
                helper.assertTrue(animal.getSize() == maximum, "Native size writes cannot shrink or oversize " + type);
            }
            // Metadata packets and old world NBT bypass the usual spawn event.
            animal.getEntityData().set(BaseWaterAnimal.Companion.getSIZE(), -5);
            helper.assertTrue(animal.getSize() == maximum, "Stale metadata cannot render a small " + type);
            CompoundTag saved = new CompoundTag();
            animal.addAdditionalSaveData(saved);
            saved.putInt("Size", -5);
            animal.readAdditionalSaveData(saved);
            DocumentedNaturalSpawns.finishedSpawn(new EntityJoinLevelEvent(animal, tropical, true));
            animal.addAdditionalSaveData(saved);
            helper.assertTrue(saved.getInt("Size") == maximum
                    && animal.getEntityData().get(BaseWaterAnimal.Companion.getSIZE()) == maximum,
                    "Old-world and creature-net data must be upgraded and persisted: " + type);
        }
        helper.assertTrue(checked >= 100, "The all-species audit must cover the installed Hybrid Aquatic roster; checked=" + checked);
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void nativeHybridSpawnAndBreedingCannotRestoreRandomSizesOrChangeStarfishTypes(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        for (String id : new String[]{"octopus", "umbrella_octopus", "trout", "tuna", "spider_crab",
                "manta_ray", "whale_shark", "moon_jellyfish"}) {
            var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("hybrid_aquatic", id));
            for (int seed = 0; seed < 12; seed++) {
                var animal = (BaseWaterAnimal) type.create(level);
                animal.getRandom().setSeed(seed);
                animal.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.ZERO), MobSpawnType.NATURAL, null, null);
                helper.assertTrue(animal.getSize() == AquaticMobSizes.maximum(animal), "Actual native spawn randomized size: " + id);
            }
        }
        var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("hybrid_aquatic:starfish"));
        var starfish = (StarfishEntity) type.create(level);
        var mate = (StarfishEntity) type.create(level);
        starfish.finalizeSpawnChildFromBreeding(level, mate);
        helper.assertTrue(starfish.getSize() >= -5 && starfish.getSize() <= 5 && mate.getSize() == 0,
                "Starfish breeding keeps the native random scalar and does not maximize the other parent");
        for (Type variant : Type.values()) {
            starfish.setVariant(variant);
            starfish.setSize(-5);
            helper.assertTrue(starfish.getVariant() == variant && starfish.getSize() == -5,
                    "Native scalar must preserve all artistic starfish types");
            helper.assertTrue(Math.abs(BaseWaterAnimal.Companion.getScaleAdjustment(starfish, .05F) - .75F) < .0001F,
                    "The real renderer must preserve the native starfish scale");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void otherVariableAquaticsUseNativeMaximumOnlyInTropical(GameTestHelper helper) {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var outside = helper.getLevel();
        for (String id : new String[]{"aoemod:end_fish", "aoemod:cubozoa"}) {
            var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(id));
            for (var level : new net.minecraft.server.level.ServerLevel[]{tropical, outside}) {
                Mob fish = (Mob) type.create(level);
                CompoundTag state = new CompoundTag();
                fish.addAdditionalSaveData(state);
                state.putByte("Scale", (byte) 0);
                fish.readAdditionalSaveData(state);
                fish.addAdditionalSaveData(state);
                helper.assertTrue(state.getByte("Scale") == (level == tropical ? 15 : 0), "Wrong scoped End-fish scalar for " + id);
                if (level == tropical) {
                    DocumentedNaturalSpawns.finishedSpawn(new EntityJoinLevelEvent(fish, level, true));
                    var expected = fish.getDimensions(fish.getPose());
                    helper.assertTrue(Math.abs(fish.getBbWidth() - expected.width) < .0001F
                                    && Math.abs(fish.getBbHeight() - expected.height) < .0001F,
                            "Loaded End-fish collision bounds must match its maximum displayed size: " + id);
                    var bucket = (fish instanceof net.amurdza.examplemod.entity.CubozoaEntity lava
                            ? lava.getBucketItemStack() : ((net.minecraft.world.entity.animal.Bucketable) fish).getBucketItemStack()).getOrCreateTag();
                    helper.assertTrue(bucket.getByte(bucket.contains("scale") ? "scale" : "Scale") == 15, "Bucket lost maximum scalar: " + id);
                }
            }
        }
        for (var level : new net.minecraft.server.level.ServerLevel[]{tropical, outside}) {
            var triops = (com.github.alexthe666.alexsmobs.entity.EntityTriops) ForgeRegistries.ENTITY_TYPES
                    .getValue(new ResourceLocation("alexsmobs:triops")).create(level);
            triops.setTriopsScale(.9F);
            helper.assertTrue(Math.abs(triops.getTriopsScale() - (level == tropical ? 1.1F : .9F)) < .0001F,
                    "Triops scalar must be maximum only in tropical");
            for (String id : new String[]{"box_jellyfish", "cassiopea_jellyfish"}) {
                var jellyfish = (com.teamabnormals.upgrade_aquatic.common.entity.animal.jellyfish.ColoredSizableJellyfish)
                        ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("upgrade_aquatic", id)).create(level);
                jellyfish.setSize(.5F, true);
                helper.assertTrue(jellyfish.getSize() == (level == tropical ? jellyfish.getNaturalSizeMap().lastKey() : .5F),
                        "Upgrade Aquatic jellyfish scalar must be maximum only in tropical");
            }
            var starfish = (BaseWaterAnimal) ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("hybrid_aquatic:starfish")).create(level);
            starfish.setSize(-5);
            helper.assertTrue(starfish.getSize() == -5, "Starfish native size setters must work in every dimension");
        }
        helper.succeed();
    }
}
