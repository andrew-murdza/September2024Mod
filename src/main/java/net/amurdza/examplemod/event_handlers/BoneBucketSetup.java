package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.item.BoneBucketItem;
import net.amurdza.examplemod.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockSource;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class BoneBucketSetup {
    private BoneBucketSetup() { }

    @SubscribeEvent
    public static void creativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.BONE_BUCKET.get());
            event.accept(ModItems.BONE_WATER_BUCKET.get());
            event.accept(ModItems.BONE_LAVA_BUCKET.get());
        }
        if (event.getTabKey() == CreativeModeTabs.COMBAT) event.accept(ModItems.BOSS_AXE.get());
    }

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            registerDispensers();
            registerCauldrons();
        });
    }

    private static void registerDispensers() {
        DefaultDispenseItemBehavior emptyFluid = new DefaultDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource source, ItemStack stack) {
                BlockPos pos = source.getPos().relative(source.getBlockState().getValue(DispenserBlock.FACING));
                BoneBucketItem bucket = (BoneBucketItem) stack.getItem();
                if (bucket.emptyContents(null, source.getLevel(), pos, null, stack)) {
                    bucket.checkExtraContent(null, source.getLevel(), stack, pos);
                    return BoneBucketItem.bucketFor(Fluids.EMPTY);
                }
                return super.execute(source, stack);
            }
        };
        DispenserBlock.registerBehavior(ModItems.BONE_WATER_BUCKET.get(), emptyFluid);
        DispenserBlock.registerBehavior(ModItems.BONE_LAVA_BUCKET.get(), emptyFluid);
        DispenserBlock.registerBehavior(ModItems.BONE_BUCKET.get(), new DefaultDispenseItemBehavior() {
            @Override
            protected ItemStack execute(BlockSource source, ItemStack stack) {
                BlockPos pos = source.getPos().relative(source.getBlockState().getValue(DispenserBlock.FACING));
                var state = source.getLevel().getBlockState(pos);
                var fluid = state.getFluidState().getType();
                if ((fluid == Fluids.WATER || fluid == Fluids.LAVA) && state.getBlock() instanceof BucketPickup pickup
                        && !pickup.pickupBlock(source.getLevel(), pos, state).isEmpty()) {
                    ItemStack filled = BoneBucketItem.bucketFor(fluid);
                    source.getLevel().gameEvent(null, GameEvent.FLUID_PICKUP, pos);
                    stack.shrink(1);
                    if (stack.isEmpty()) return filled;
                    DispenserBlockEntity dispenser = source.getEntity();
                    if (dispenser.addItem(filled) < 0) super.execute(source, filled);
                    return stack;
                }
                return super.execute(source, stack);
            }
        });
    }

    private static void registerCauldrons() {
        CauldronInteraction.WATER.put(ModItems.BONE_BUCKET.get(), (state, level, pos, player, hand, stack) ->
                CauldronInteraction.fillBucket(state, level, pos, player, hand, stack,
                        BoneBucketItem.bucketFor(Fluids.WATER), value -> value.getValue(LayeredCauldronBlock.LEVEL) == 3,
                        SoundEvents.BUCKET_FILL));
        CauldronInteraction.LAVA.put(ModItems.BONE_BUCKET.get(), (state, level, pos, player, hand, stack) ->
                CauldronInteraction.fillBucket(state, level, pos, player, hand, stack,
                        BoneBucketItem.bucketFor(Fluids.LAVA), value -> true, SoundEvents.BUCKET_FILL_LAVA));
        for (var interactions : java.util.List.of(CauldronInteraction.EMPTY, CauldronInteraction.WATER,
                CauldronInteraction.LAVA, CauldronInteraction.POWDER_SNOW)) {
            for (var item : java.util.List.of(ModItems.BONE_WATER_BUCKET.get(), ModItems.BONE_LAVA_BUCKET.get())) {
                interactions.put(item, (state, level, pos, player, hand, stack) -> {
                    boolean water = ((BoneBucketItem) item).getFluid() == Fluids.WATER;
                    var filledState = water ? Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3)
                            : Blocks.LAVA_CAULDRON.defaultBlockState();
                    if (!level.isClientSide) {
                        if (!level.setBlockAndUpdate(pos, filledState)) return InteractionResult.FAIL;
                        player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, BoneBucketItem.bucketFor(Fluids.EMPTY)));
                        player.awardStat(Stats.FILL_CAULDRON);
                        player.awardStat(Stats.ITEM_USED.get(item));
                        level.playSound(null, pos, water ? SoundEvents.BUCKET_EMPTY : SoundEvents.BUCKET_EMPTY_LAVA,
                                SoundSource.BLOCKS, 1.0F, 1.0F);
                        level.gameEvent(player, GameEvent.FLUID_PLACE, pos);
                    }
                    return InteractionResult.sidedSuccess(level.isClientSide);
                });
            }
        }
    }
}
