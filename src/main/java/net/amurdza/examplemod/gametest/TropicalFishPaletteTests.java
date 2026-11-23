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
            helper.assertTrue(fish.getVariant() == TropicalFish.Pattern.CLAYFISH && fish.getBaseColor() != fish.getPatternColor(),
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
        helper.assertTrue(variants.size() == 182, "Clayfish must support exactly 14 * 13 distinct color pairs");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void savedAndBucketVariantsEnforceExactlyTheSamePalette(GameTestHelper helper) {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var fish = EntityType.TROPICAL_FISH.create(tropical);
        var control = EntityType.TROPICAL_FISH.create(helper.getLevel());
        int retained = 0;
        for (var pattern : TropicalFish.Pattern.values()) for (var base : DyeColor.values()) for (var marking : DyeColor.values()) {
            int packed = new TropicalFish.Variant(pattern, base, marking).getPackedId();
            var saved = new net.minecraft.nbt.CompoundTag();
            saved.putInt("Variant", packed);
            fish.readAdditionalSaveData(saved);
            control.readAdditionalSaveData(saved);
            helper.assertTrue(fish.getVariant() == TropicalFish.Pattern.CLAYFISH && fish.getBaseColor() != fish.getPatternColor()
                            && fish.getBaseColor() != DyeColor.BLACK && fish.getBaseColor() != DyeColor.GRAY
                            && fish.getPatternColor() != DyeColor.BLACK && fish.getPatternColor() != DyeColor.GRAY,
                    "Saved fish escaped the tropical palette");
            helper.assertTrue(control.getVariant() == pattern && control.getBaseColor() == base && control.getPatternColor() == marking,
                    "The tropical palette must not change fish in other dimensions");
            boolean allowed = pattern == TropicalFish.Pattern.CLAYFISH && base != marking
                    && base != DyeColor.BLACK && base != DyeColor.GRAY && marking != DyeColor.BLACK && marking != DyeColor.GRAY;
            if (allowed) {
                retained++;
                helper.assertTrue(fish.getBaseColor() == base && fish.getPatternColor() == marking,
                        "A valid saved clayfish must keep its exact colors");
            }
            var bucket = new net.minecraft.nbt.CompoundTag();
            bucket.putInt(TropicalFish.BUCKET_VARIANT_TAG, packed);
            fish.finalizeSpawn(tropical, tropical.getCurrentDifficultyAt(tropical.getSharedSpawnPos()), MobSpawnType.BUCKET, null, bucket);
            helper.assertTrue(fish.getVariant() == TropicalFish.Pattern.CLAYFISH && fish.getBaseColor() != fish.getPatternColor()
                            && fish.getBaseColor() != DyeColor.BLACK && fish.getBaseColor() != DyeColor.GRAY
                            && fish.getPatternColor() != DyeColor.BLACK && fish.getPatternColor() != DyeColor.GRAY,
                    "Bucket fish escaped the tropical palette");
        }
        helper.assertTrue(retained == 182, "Exactly 182 saved fish variants must retain their identity");
        fish.discard();
        control.discard();
        helper.succeed();
    }
}
