package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.ObtainableContent;
import net.amurdza.examplemod.util.RetainedContent;
import net.amurdza.examplemod.worldgen.DocumentedFloraFeature;
import net.amurdza.examplemod.worldgen.ShoreSugarCane;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DesertNovemberRevisionTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void bullKelpUsesDepthSpecificMatureHeights(GameTestHelper helper) {
        var desert = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        BlockPos root = new BlockPos(8, 101, 8);
        for (int depth = 0; depth <= 4; depth++) for (int seed = 0; seed < 16; seed++) {
            var blocks = new HashMap<BlockPos, BlockState>();
            var world = AquaticPlantGenerationTests.fixture(helper, "desert", blocks, depth, desert,
                    Blocks.SAND.defaultBlockState(), Blocks.WATER.defaultBlockState());
            boolean placed = DocumentedFloraFeature.placePlant(world, root, "hybrid_aquatic:bull_kelp", RandomSource.create(seed));
            int expected = depth < 3 ? 0 : depth - 1;
            helper.assertTrue(placed == (expected > 0) && blocks.size() == expected, "Wrong bull kelp height in depth " + depth);
            if (expected == 0) continue;
            var head = world.getBlockState(root.above(expected - 1));
            helper.assertTrue(head.getValue(BlockStateProperties.AGE_25) == 25 && !head.isRandomlyTicking(),
                    "All generated bull kelp must have terminal age 25");
            for (int y = 0; y < expected; y++) helper.assertTrue(world.getBlockState(root.above(y)).canSurvive(world, root.above(y))
                            && world.getFluidState(root.above(y)).is(FluidTags.WATER), "Every kelp block must retain support and original water");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void desertCaneConfiguredFeatureCreatesConnectedWaterRootedPatches(GameTestHelper helper) {
        var desert = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        var feature = desert.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).get(new ResourceLocation("aoemod:sugar_cane_patch"));
        for (String biome : new String[]{"desert", "mesa"}) for (int depth = 0; depth <= 2; depth++) {
            var blocks = new HashMap<BlockPos, BlockState>();
            var world = AquaticPlantGenerationTests.fixture(helper, biome, blocks, depth, desert,
                    Blocks.SAND.defaultBlockState(), Blocks.WATER.defaultBlockState());
            // A real dry shoreline borders every eligible root; open lake interiors are rejected.
            for (int x = 5; x <= 12; x++) blocks.put(new BlockPos(x, 101, 5), Blocks.SAND.defaultBlockState());
            int bankBlocks = blocks.size();
            boolean placed = feature.place(world, desert.getChunkSource().getGenerator(), RandomSource.create(19), new BlockPos(8, 102, 8));
            helper.assertTrue(placed == (depth == 1), "Desert cane must root only in one-deep water");
            if (!placed) { helper.assertTrue(blocks.size() == bankBlocks, "Rejected patches must not leave partial cane"); continue; }
            Set<BlockPos> roots = new HashSet<>();
            blocks.forEach((pos, state) -> { if (pos.getY() == 101 && state.is(Blocks.SUGAR_CANE)) roots.add(pos); });
            helper.assertTrue(roots.size() == 5 && blocks.size() == bankBlocks + 15, "A complete patch needs five three-block stalks");
            Set<BlockPos> connected = new HashSet<>();
            connected.add(roots.iterator().next());
            for (int pass = 0; pass < 5; pass++) for (BlockPos root : roots) {
                for (Direction direction : Direction.Plane.HORIZONTAL) if (connected.contains(root.relative(direction))) connected.add(root);
            }
            helper.assertTrue(connected.equals(roots), "All five roots must form one continuous region without gaps");
            for (BlockPos root : roots) helper.assertTrue(ShoreSugarCane.touchesDryShore(world, root), "Every cane root must touch the bank");
            for (BlockPos root : roots) for (int y = 0; y < 3; y++) {
                var state = world.getBlockState(root.above(y));
                helper.assertTrue(state.is(Blocks.SUGAR_CANE) && state.canSurvive(world, root.above(y))
                                && state.getFluidState().is(FluidTags.WATER) == (y == 0),
                        "Only the bottom section of each surviving stalk must preserve source water");
            }
            helper.assertTrue(ShoreSugarCane.placeConfiguredPatch(world, new BlockPos(8, 102, 8)) == 0,
                    "Repeated decoration must preserve the existing patch");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void sandyFarmlandSurvivesAcquisitionRebuildAndRealHoeUse(GameTestHelper helper) {
        var desert = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        ObtainableContent.rebuild(desert.getRecipeManager(), desert.registryAccess());
        var farmland = ForgeRegistries.ITEMS.getValue(new ResourceLocation("biomeswevegone:sandy_farmland"));
        helper.assertTrue(farmland != null && farmland != Items.AIR && ObtainableContent.contains(farmland),
                "Restored sandy farmland must remain obtainable after the acquisition rebuild");
        BlockPos pos = helper.absolutePos(BlockPos.ZERO).atY(230);
        desert.setBlock(pos, Blocks.SAND.defaultBlockState(), 18);
        desert.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 18);
        var player = FakePlayerFactory.getMinecraft(desert);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_HOE));
        Items.IRON_HOE.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
        helper.assertTrue(desert.getBlockState(pos).getBlock().asItem() == farmland, "A real hoe must till desert sand into restored sandy farmland");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void naturalWitherSkeletonEquipmentIsStoneAfterActualJoin(GameTestHelper helper) {
        var desert = helper.getLevel().getServer().getLevel(GameplayDimensions.DESERT);
        BlockPos pos = helper.absolutePos(BlockPos.ZERO).atY(235);
        var skeleton = EntityType.WITHER_SKELETON.create(desert);
        skeleton.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
        skeleton.finalizeSpawn(desert, desert.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null, null);
        skeleton.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
        try {
            helper.assertTrue(desert.addFreshEntity(skeleton), "Wither skeleton must enter the real server level");
            helper.assertTrue(skeleton.getMainHandItem().is(Items.STONE_SWORD), "Actual join must replace old netherite with the requested stone sword");
        } finally { skeleton.discard(); }
        helper.succeed();
    }
}
