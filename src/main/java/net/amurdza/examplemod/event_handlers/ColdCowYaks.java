package net.amurdza.examplemod.event_handlers;

import com.blackgear.vanillabackport.common.api.modules.mob_variant.VariantDataHolder;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.cow.CowVariant;
import com.blackgear.vanillabackport.common.level.entities.mob.animal.cow.CowVariants;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.entity.ai.YakAccess;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public final class ColdCowYaks {
    private ColdCowYaks() { }

    @SuppressWarnings("unchecked")
    public static void makeCold(Cow cow) {
        if (cow instanceof VariantDataHolder<?> holder) {
            ((VariantDataHolder<CowVariant>) holder).setVariantData(CowVariants.REGISTRIES.getOrThrow(CowVariants.COLD));
        }
    }

    public static boolean isColdCow(Cow cow) {
        return cow.getType() == EntityType.COW && cow instanceof VariantDataHolder<?> holder
                && holder.getVariantData().orElse(null) == CowVariants.REGISTRIES.getOrThrow(CowVariants.COLD);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interact(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof Cow cow) || !(cow instanceof YakAccess yak)
                || cow.getType() != EntityType.COW) return;
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        boolean owned = player.getUUID().equals(yak.aoe$yakOwner());
        boolean handled = false;
        if (yak.aoe$yakOwner() == null && isColdCow(cow) && !cow.isBaby() && stack.is(Items.WHEAT)) {
            handled = true;
            if (!cow.level().isClientSide && !ForgeEventFactory.onAnimalTame(cow, player)) {
                yak.aoe$setYakOwner(player.getUUID());
                cow.setPersistenceRequired();
                if (!player.getAbilities().instabuild) stack.shrink(1);
                ((ServerLevel) cow.level()).sendParticles(ParticleTypes.HEART,
                        cow.getX(), cow.getY() + 1.2, cow.getZ(), 7, 0.4, 0.3, 0.4, 0.01);
                if (player instanceof ServerPlayer serverPlayer) CriteriaTriggers.TAME_ANIMAL.trigger(serverPlayer, cow);
            }
        } else if (owned && !cow.isBaby() && stack.is(Items.CHEST) && !yak.aoe$hasYakChest()) {
            handled = true;
            if (!cow.level().isClientSide) {
                yak.aoe$setYakChest(true);
                if (!player.getAbilities().instabuild) stack.shrink(1);
            }
        } else if (owned && stack.isEmpty()) {
            handled = true;
            if (!cow.level().isClientSide) {
                if (player.isSecondaryUseActive() || !yak.aoe$hasYakChest()) {
                    yak.aoe$setYakStaying(!yak.aoe$yakStaying());
                    cow.getNavigation().stop();
                    player.displayClientMessage(Component.translatable(yak.aoe$yakStaying()
                            ? "message.aoemod.yak_staying" : "message.aoemod.yak_following"), true);
                } else {
                    player.openMenu(new SimpleMenuProvider((id, inventory, owner) ->
                            new ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x3, id, inventory,
                                    yak.aoe$yakInventory(), 3) {
                                @Override public boolean stillValid(Player viewer) {
                                    return cow.isAlive() && cow.distanceToSqr(viewer) <= 64
                                            && viewer.getUUID().equals(yak.aoe$yakOwner()) && yak.aoe$hasYakChest();
                                }
                            }, Component.translatable("container.aoemod.yak")));
                }
            }
        }
        if (handled) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.sidedSuccess(cow.level().isClientSide));
        }
    }

    @SubscribeEvent
    public static void join(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof Cow cow
                && cow.getType() == EntityType.COW && cow instanceof YakAccess yak) {
            cow.goalSelector.addGoal(1, new Goal() {
                { setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP)); }
                @Override public boolean canUse() { return yak.aoe$yakOwner() != null && yak.aoe$yakStaying(); }
                @Override public void tick() { cow.getNavigation().stop(); }
            });
            cow.goalSelector.addGoal(3, new Goal() {
                private Player owner;
                { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
                @Override public boolean canUse() {
                    owner = yak.aoe$yakOwner() == null ? null : cow.level().getPlayerByUUID(yak.aoe$yakOwner());
                    return owner != null && !owner.isSpectator() && !yak.aoe$yakStaying()
                            && !cow.isLeashed() && cow.distanceToSqr(owner) > 36;
                }
                @Override public boolean canContinueToUse() {
                    return owner != null && owner.isAlive() && !yak.aoe$yakStaying()
                            && !cow.isLeashed() && cow.distanceToSqr(owner) > 9;
                }
                @Override public void tick() {
                    cow.getLookControl().setLookAt(owner, 10, cow.getMaxHeadXRot());
                    cow.getNavigation().moveTo(owner, 1.1);
                }
                @Override public void stop() { cow.getNavigation().stop(); owner = null; }
            });
        }
    }

    @SubscribeEvent
    public static void capabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Cow cow && cow.getType() == EntityType.COW && cow instanceof YakAccess yak) {
            LazyOptional<IItemHandler> inventory = LazyOptional.of(() -> new InvWrapper(yak.aoe$yakInventory()));
            event.addCapability(new ResourceLocation(AOEMod.MOD_ID, "yak_cargo"), new ICapabilityProvider() {
                @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
                    return capability == ForgeCapabilities.ITEM_HANDLER && yak.aoe$hasYakChest()
                            ? inventory.cast() : LazyOptional.empty();
                }
            });
            event.addListener(inventory::invalidate);
        }
    }

    /** Called after configured resource-drop replacement so pack contents cannot be erased. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void drops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof YakAccess yak)) return;
        for (ItemStack stack : drainCargo(yak)) {
            Entity cow = event.getEntity();
            event.getDrops().add(new ItemEntity(cow.level(), cow.getX(), cow.getY(), cow.getZ(), stack));
        }
    }

    public static List<ItemStack> drainCargo(YakAccess yak) {
        List<ItemStack> drops = new ArrayList<>();
        if (yak.aoe$hasYakChest()) drops.add(new ItemStack(Items.CHEST));
        for (int i = 0; i < yak.aoe$yakInventory().getContainerSize(); i++) {
            ItemStack stack = yak.aoe$yakInventory().removeItemNoUpdate(i);
            if (!stack.isEmpty()) drops.add(stack);
        }
        yak.aoe$setYakChest(false);
        return drops;
    }
}
