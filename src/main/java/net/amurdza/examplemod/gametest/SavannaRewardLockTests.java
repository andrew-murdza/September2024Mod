package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.worldgen.structure.AuthoredEncounterProgress;
import net.amurdza.examplemod.worldgen.structure.SavannaCampProgress;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcCampPiece;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcVillage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SavannaRewardLockTests {
    private SavannaRewardLockTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void everyAuthoredCropSectionUnlocksWithoutReleasingGroundOrFences(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = new BlockPos(-317, 180, 213);
        var camp = new AuthoredEncounterProgress().register(UUID.randomUUID(), origin, 127, SavannaCampProgress.KIND);
        var bounds = SavannaCampProgress.bounds(camp);
        var data = level.getStructureManager().getOrCreate(SavannaOrcCampPiece.TEMPLATE).save(new CompoundTag());
        var palette = data.getList("palette", Tag.TAG_COMPOUND);
        var crops = Set.of("minecraft:wheat", "minecraft:pumpkin", "minecraft:attached_pumpkin_stem",
                "minecraft:beetroots", "farmersdelight:cabbages", "minecraft:potatoes", "minecraft:carrots",
                "hearthandharvest:corn_stalk", "minecraft:sugar_cane");
        var permanent = Set.of("minecraft:farmland", "minecraft:dirt", "minecraft:grass_block", "minecraft:mud",
                "minecraft:acacia_fence", "minecraft:acacia_fence_gate", "minecraft:black_wool");
        var observedCrops = new HashSet<String>();
        var observedPermanent = new HashSet<String>();
        var cornSections = new HashSet<String>();
        var cropQuadrants = new HashSet<String>();
        boolean submergedCane = false;
        for (var entry : data.getList("blocks", Tag.TAG_COMPOUND)) {
            var block = (CompoundTag) entry;
            var local = block.getList("pos", Tag.TAG_INT);
            var pos = origin.offset(local.getInt(0), local.getInt(1), local.getInt(2));
            var encodedState = palette.getCompound(block.getInt("state"));
            var name = encodedState.getString("Name");
            if (!crops.contains(name) && !permanent.contains(name)) continue;
            var state = NbtUtils.readBlockState(level.holderLookup(Registries.BLOCK), encodedState);
            if (crops.contains(name)) {
                helper.assertTrue(SavannaCampProgress.isRewardBlock(state, pos, bounds),
                        "Every section of an authored crop must unlock after victory: " + name + " at " + pos);
                observedCrops.add(name);
                cropQuadrants.add((local.getInt(0) < SavannaOrcVillage.CENTER ? "west" : "east")
                        + (local.getInt(2) < SavannaOrcVillage.CENTER ? "north" : "south"));
                if (name.equals("hearthandharvest:corn_stalk"))
                    cornSections.add(encodedState.getCompound("Properties").getString("section"));
                if (name.equals("minecraft:sugar_cane") && local.getInt(1) == 2) submergedCane = true;
            } else {
                helper.assertFalse(SavannaCampProgress.isRewardBlock(state, pos, bounds),
                        "Victory must never release authored ground, fencing, or roofs: " + name + " at " + pos);
                observedPermanent.add(name);
            }
        }
        helper.assertTrue(observedCrops.equals(crops) && cropQuadrants.size() == 4,
                "The real camp template must exercise every planted crop in all four fields");
        helper.assertTrue(observedPermanent.contains("minecraft:farmland")
                        && observedPermanent.contains("minecraft:acacia_fence")
                        && observedPermanent.contains("minecraft:dirt"),
                "The template probes must include the crop foundations and fence");
        helper.assertTrue(cornSections.equals(Set.of("bottom", "middle", "top")) && submergedCane,
                "The test must include all three modded corn sections and the submerged cane base");

        for (int sx : new int[]{-1, 1}) for (int sz : new int[]{-1, 1}) {
            for (int[] sample : new int[][]{{4, 40, 1}, {40, 4, 1}, {51, 51, 1},
                    {3, 40, 0}, {40, 3, 0}, {39, 39, 0}, {52, 51, 0}, {65, 65, 0}}) {
                int x = sx > 0 ? sample[0] : -sample[0] - 1;
                int z = sz > 0 ? sample[1] : -sample[1] - 1;
                var pos = origin.offset(SavannaOrcVillage.CENTER + x, 3, SavannaOrcVillage.CENTER + z);
                helper.assertTrue(SavannaCampProgress.isRewardBlock(Blocks.WHEAT.defaultBlockState(), pos, bounds)
                                == (sample[2] == 1),
                        "Harvesting must stay inside the actual fence on every mirrored edge: " + pos);
            }
        }
        helper.succeed();
    }
}
