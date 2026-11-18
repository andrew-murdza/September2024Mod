package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.ItemsPlaceModBlocks;
import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.worldgen.SnowyPlantSnowlogging;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import snownee.snow.block.entity.SnowBlockEntity;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BlueberryPlacementFollowupTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void blueberryPlacementMatchesNativeBerryTargetsAndPreservesOneThroughEightLayers(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockSurvivalPlayer();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var blueberry = ForgeRegistries.ITEMS.getValue(new ResourceLocation("fruitsdelight:blueberry"));
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        for (int layers = 1; layers <= 8; layers++) {
            var snow = Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, layers);
            level.setBlock(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 18);
            level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 18);
            level.setBlock(pos, snow, 18);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SWEET_BERRIES, 2));
            var nativeContext = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, player.getMainHandItem(), hit);
            BlockPos nativeTarget = nativeContext.getClickedPos();
            boolean nativePlaced = ((BlockItem) Items.SWEET_BERRIES).place(nativeContext).consumesAction();
            helper.assertTrue(nativePlaced == contains(level, nativeTarget, Blocks.SWEET_BERRY_BUSH),
                    "Native berry success must reflect an actual planted bush at layer count " + layers);

            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
            level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 18);
            level.setBlock(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 18);
            level.setBlock(pos, snow, 18);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(blueberry, 2));
            var blueberryContext = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, player.getMainHandItem(), hit);
            helper.assertTrue(blueberryContext.getClickedPos().equals(nativeTarget),
                    "Food-item blueberry context must target the same block as native berries for " + layers + " layers");
            var event = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit);
            ItemsPlaceModBlocks.sunflowerPlace1(event);
            boolean bluePlaced = event.isCanceled() && event.getCancellationResult().consumesAction();
            helper.assertTrue(bluePlaced == nativePlaced,
                    "Blueberries and native sweet berries must agree on whether this snow depth can be planted: " + layers);
            helper.assertTrue(level.getBlockState(pos).getValue(SnowLayerBlock.LAYERS) == layers,
                    "Player planting must preserve the exact pre-existing snow amount, including full eight-layer snow");
            if (bluePlaced) {
                helper.assertTrue(level.getBlockEntity(pos) instanceof SnowBlockEntity
                                && contains(level, pos, ModBlocks.BLUE_BERRY_BUSH.get()) && level.getBlockState(pos.above()).isAir(),
                        "Successful planting must embed the bush in the original snow block");
            } else helper.assertTrue(level.getBlockState(pos).is(Blocks.SNOW) && level.getBlockState(pos.above()).isAir(),
                    "A rejected full-snow placement must leave the snow and the block above untouched");
            helper.assertTrue(player.getMainHandItem().getCount() == (bluePlaced ? 1 : 2),
                    "Exactly one fruit is consumed only for a successful placement");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void blueberryPlacementMatchesNativeReplacementRulesForSnowloggedPlants(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockSurvivalPlayer();
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        var blueberry = ForgeRegistries.ITEMS.getValue(new ResourceLocation("fruitsdelight:blueberry"));
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        for (var plant : new net.minecraft.world.level.block.Block[]{Blocks.FERN, Blocks.GRASS, Blocks.SWEET_BERRY_BUSH}) {
            for (int layers = 1; layers <= 8; layers++) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
                level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 18);
                level.setBlock(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 18);
                level.setBlock(pos, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, layers), 18);
                helper.assertTrue(SnowyPlantSnowlogging.place(level, pos, plant.defaultBlockState()),
                        "Occupied snow fixture must contain the source plant");
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SWEET_BERRIES, 2));
                var nativeContext = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, player.getMainHandItem(), hit);
                BlockPos nativeTarget = nativeContext.getClickedPos();
                boolean nativePlaced = ((BlockItem) Items.SWEET_BERRIES).place(nativeContext).consumesAction();
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
                level.setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 18);
                level.setBlock(pos.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 18);
                level.setBlock(pos, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, layers), 18);
                helper.assertTrue(SnowyPlantSnowlogging.place(level, pos, plant.defaultBlockState()),
                        "Blueberry fixture must match the native source plant and snow depth");
                var original = level.getBlockState(pos);
                var originalContents = ((SnowBlockEntity) level.getBlockEntity(pos)).getContainedState();
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(blueberry, 2));
                var blueberryContext = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, player.getMainHandItem(), hit);
                helper.assertTrue(blueberryContext.getClickedPos().equals(nativeTarget),
                        "Both berry items must respect the same snow-container replacement target");
                var event = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos, hit);
                ItemsPlaceModBlocks.sunflowerPlace1(event);
                boolean bluePlaced = event.isCanceled() && event.getCancellationResult().consumesAction();
                helper.assertTrue(bluePlaced == nativePlaced,
                        "Replacing a snowlogged plant must follow native sweet-berry rules: " + plant + " / " + layers);
                helper.assertTrue(level.getBlockState(pos).equals(original)
                                && level.getBlockEntity(pos) instanceof SnowBlockEntity container
                                && container.getContainedState().equals(bluePlaced
                                ? ModBlocks.BLUE_BERRY_BUSH.get().defaultBlockState() : originalContents)
                                && level.getBlockState(pos.above()).isAir()
                                && player.getMainHandItem().getCount() == (bluePlaced ? 1 : 2),
                        "Replacing a permitted plant must preserve snow; rejected plants and item counts stay intact: " + plant + " / " + layers);
            }
        }
        helper.succeed();
    }

    private static boolean contains(net.minecraft.world.level.Level level, BlockPos pos,
            net.minecraft.world.level.block.Block plant) {
        return level.getBlockState(pos).is(plant) || level.getBlockEntity(pos) instanceof SnowBlockEntity snow
                && snow.getContainedState().is(plant);
    }
}
