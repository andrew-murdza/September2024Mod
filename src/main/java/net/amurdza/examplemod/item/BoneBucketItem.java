package net.amurdza.examplemod.item;

import net.amurdza.examplemod.registry.ModItems;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.wrappers.FluidBucketWrapper;
import org.jetbrains.annotations.Nullable;

/** Water/lava bucket whose container stays bone through use and fluid capabilities. */
public class BoneBucketItem extends BucketItem {
    public BoneBucketItem(Fluid fluid, Properties properties) {
        super(fluid, properties);
    }

    public static ItemStack bucketFor(Fluid fluid) {
        if (fluid == Fluids.WATER) return new ItemStack(ModItems.BONE_WATER_BUCKET.get());
        if (fluid == Fluids.LAVA) return new ItemStack(ModItems.BONE_LAVA_BUCKET.get());
        return new ItemStack(ModItems.BONE_BUCKET.get());
    }

    @Override
    public int getBurnTime(ItemStack stack, @Nullable RecipeType<?> recipeType) {
        return getFluid() == Fluids.LAVA ? ForgeHooks.getBurnTime(new ItemStack(Items.LAVA_BUCKET), recipeType) : 0;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (getFluid() != Fluids.EMPTY) {
            var result = super.use(level, player, hand);
            return result.getObject().is(Items.BUCKET)
                    ? new InteractionResultHolder<>(result.getResult(), bucketFor(Fluids.EMPTY)) : result;
        }
        ItemStack held = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        var eventResult = ForgeEventFactory.onBucketUse(player, level, held, hit);
        if (eventResult != null) return eventResult;
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(held);
        BlockPos pos = hit.getBlockPos();
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos.relative(hit.getDirection()), hit.getDirection(), held)) {
            return InteractionResultHolder.fail(held);
        }
        BlockState state = level.getBlockState(pos);
        Fluid fluid = state.getFluidState().getType();
        if ((fluid == Fluids.WATER || fluid == Fluids.LAVA) && state.getBlock() instanceof BucketPickup pickup) {
            ItemStack pickedUp = pickup.pickupBlock(level, pos, state);
            if (!pickedUp.isEmpty()) {
                ItemStack filled = bucketFor(fluid);
                player.awardStat(Stats.ITEM_USED.get(this));
                pickup.getPickupSound(state).ifPresent(sound -> player.playSound(sound, 1.0F, 1.0F));
                level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
                if (player instanceof ServerPlayer serverPlayer) CriteriaTriggers.FILLED_BUCKET.trigger(serverPlayer, filled);
                return InteractionResultHolder.sidedSuccess(ItemUtils.createFilledResult(held, player, filled), level.isClientSide());
            }
        }
        return InteractionResultHolder.fail(held);
    }

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new FluidBucketWrapper(stack) {
            @Override
            public boolean canFillFluidType(FluidStack fluid) {
                return fluid.getFluid() == Fluids.WATER || fluid.getFluid() == Fluids.LAVA;
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack fluid) {
                return canFillFluidType(fluid);
            }

            @Override
            protected void setFluid(FluidStack fluid) {
                container = bucketFor(fluid.isEmpty() ? Fluids.EMPTY : fluid.getFluid());
            }
        };
    }
}
