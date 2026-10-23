package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.item.BoneBucketItem;
import net.amurdza.examplemod.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BoneBucketTests {
    private BoneBucketTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void fluidCapabilityKeepsBoneContainer(GameTestHelper helper) {
        for (var fluid : new net.minecraft.world.level.material.Fluid[]{Fluids.WATER, Fluids.LAVA}) {
            ItemStack empty = new ItemStack(ModItems.BONE_BUCKET.get());
            var handler = empty.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElseThrow(
                    () -> new IllegalStateException("Bone bucket has no fluid capability"));
            helper.assertTrue(handler.fill(new FluidStack(fluid, 1000), IFluidHandler.FluidAction.EXECUTE) == 1000,
                    "Bone bucket must accept a full bucket of water/lava");
            helper.assertTrue(handler.getContainer().is(BoneBucketItem.bucketFor(fluid).getItem()),
                    "Filling must produce the matching bone bucket");
            helper.assertTrue(handler.drain(1000, IFluidHandler.FluidAction.EXECUTE).getFluid() == fluid,
                    "Draining must return the original fluid");
            helper.assertTrue(handler.getContainer().is(ModItems.BONE_BUCKET.get()),
                    "Draining must return bone, not an iron bucket");
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void waterloggingAndCauldronsKeepBoneContainer(GameTestHelper helper) {
        BlockPos fence = new BlockPos(1, 1, 1);
        helper.setBlock(fence, Blocks.OAK_FENCE);
        ItemStack water = new ItemStack(ModItems.BONE_WATER_BUCKET.get());
        helper.assertTrue(((BoneBucketItem) water.getItem()).emptyContents(null, helper.getLevel(), helper.absolutePos(fence), null, water),
                "Bone water bucket should fill waterloggable blocks");
        helper.assertTrue(helper.getBlockState(fence).getValue(BlockStateProperties.WATERLOGGED),
                "Fence must become waterlogged");

        BlockPos cauldron = helper.absolutePos(new BlockPos(3, 1, 1));
        var player = helper.makeMockSurvivalPlayer();
        player.setItemInHand(InteractionHand.MAIN_HAND, water);
        helper.getLevel().setBlockAndUpdate(cauldron, Blocks.CAULDRON.defaultBlockState());
        CauldronInteraction.EMPTY.get(ModItems.BONE_WATER_BUCKET.get()).interact(Blocks.CAULDRON.defaultBlockState(),
                helper.getLevel(), cauldron, player, InteractionHand.MAIN_HAND, water);
        helper.assertTrue(player.getMainHandItem().is(ModItems.BONE_BUCKET.get()),
                "Emptying into a cauldron must retain the bone container");
        var filled = helper.getLevel().getBlockState(cauldron);
        helper.assertTrue(filled.is(Blocks.WATER_CAULDRON) && filled.getValue(LayeredCauldronBlock.LEVEL) == 3,
                "Bone water bucket should fill the cauldron");
        CauldronInteraction.WATER.get(ModItems.BONE_BUCKET.get()).interact(filled, helper.getLevel(), cauldron,
                player, InteractionHand.MAIN_HAND, player.getMainHandItem());
        helper.assertTrue(player.getMainHandItem().is(ModItems.BONE_WATER_BUCKET.get()),
                "Picking up cauldron water must produce a bone water bucket");
        helper.assertTrue(helper.getLevel().getBlockState(cauldron).is(Blocks.CAULDRON),
                "Picking up a full cauldron should empty it");
        helper.succeed();
    }
}
