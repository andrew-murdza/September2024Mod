package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TropicalFishPaletteTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void naturalColorsAndBucketRoundTrip(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var fish = EntityType.TROPICAL_FISH.create(level);
        var difficulty = level.getCurrentDifficultyAt(level.getSharedSpawnPos());
        var variants = new HashSet<Integer>();
        fish.getRandom().setSeed(271828L);
        for (int i = 0; i < 6000; i++) {
            fish.finalizeSpawn(level, difficulty, MobSpawnType.NATURAL, null, null);
            helper.assertTrue(fish.getVariant() == TropicalFish.Pattern.CLAYFISH || fish.getVariant() == TropicalFish.Pattern.BETTY,
                    "Tropical natural spawn used another pattern");
            for (var color : new DyeColor[]{fish.getBaseColor(), fish.getPatternColor()}) {
                helper.assertTrue(color != DyeColor.BLACK && color != DyeColor.GRAY, "Disallowed tropical color");
            }
            var bucket = fish.getBucketItemStack();
            fish.saveToBucketTag(bucket);
            variants.add(bucket.getTag().getInt(TropicalFish.BUCKET_VARIANT_TAG));
            if (i < 50) {
                var released = EntityType.TROPICAL_FISH.create(level);
                released.finalizeSpawn(level, difficulty, MobSpawnType.BUCKET, null, bucket.getTag());
                helper.assertTrue(released.getVariant() == fish.getVariant() && released.getBaseColor() == fish.getBaseColor()
                        && released.getPatternColor() == fish.getPatternColor(), "Bucket changed a captured fish's colors");
            }
        }
        helper.assertTrue(variants.size() == 392, "Both patterns must support every pair of 14 colors");
        helper.succeed();
    }
}
