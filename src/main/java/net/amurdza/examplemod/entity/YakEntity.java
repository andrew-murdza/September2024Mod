package net.amurdza.examplemod.entity;

import net.amurdza.examplemod.entity.ai.YakFollowCaravanGoal;
import net.amurdza.examplemod.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.Donkey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/** A real chested mount. The horse base owns taming, saddles, riding, jumping, inventory and persistence. */
public final class YakEntity extends Donkey {
    public static final int CARGO_SLOTS = 15;
    private final List<ItemStack> legacyCargo = new ArrayList<>();
    private YakEntity caravanHead;
    private YakEntity caravanTail;

    public YakEntity(EntityType<? extends YakEntity> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return createBaseChestedHorseAttributes().add(Attributes.MAX_HEALTH, 30)
                .add(Attributes.MOVEMENT_SPEED, .14).add(Attributes.JUMP_STRENGTH, .5);
    }

    @Override protected void randomizeAttributes(RandomSource random) {
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(30);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.14);
        getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(.5);
    }

    @Override protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(2, new YakFollowCaravanGoal(this));
    }

    // Inherit the exact donkey inventory, native horse-screen packet, menu and screen.
    public SimpleContainer packInventory() { return inventory; }
    public void equipPack() { setChest(true); createInventory(); }
    @Override public double getPassengersRidingOffset() { return getBbHeight() * .82; }

    /** Preserve items from the retired thirty-slot pack until they can be returned safely. */
    public void preserveLegacyCargo(ItemStack stack) {
        if (!stack.isEmpty()) legacyCargo.add(stack.copy());
        compactLegacyCargo();
    }

    private void compactLegacyCargo() {
        if (!hasChest() || legacyCargo.isEmpty()) return;
        SimpleContainer cargo = new SimpleContainer(CARGO_SLOTS);
        for (int slot = 0; slot < CARGO_SLOTS; slot++) cargo.setItem(slot, inventory.getItem(slot + 2).copy());
        var remaining = new ArrayList<ItemStack>();
        for (ItemStack stack : legacyCargo) {
            ItemStack rest = cargo.addItem(stack.copy());
            if (!rest.isEmpty()) remaining.add(rest);
        }
        for (int slot = 0; slot < CARGO_SLOTS; slot++) inventory.setItem(slot + 2, cargo.getItem(slot));
        legacyCargo.clear();
        legacyCargo.addAll(remaining);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        legacyCargo.clear();
        ListTag oldItems = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int index = 0; index < oldItems.size(); index++) {
            CompoundTag entry = oldItems.getCompound(index);
            if ((entry.getByte("Slot") & 255) >= 2 + CARGO_SLOTS) {
                legacyCargo.add(ItemStack.of(entry));
            }
        }
        ListTag pending = tag.getList("AoeYakCargoOverflow", Tag.TAG_COMPOUND);
        for (int index = 0; index < pending.size(); index++) legacyCargo.add(ItemStack.of(pending.getCompound(index)));
        compactLegacyCargo();
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (!legacyCargo.isEmpty()) {
            ListTag pending = new ListTag();
            for (ItemStack stack : legacyCargo) pending.add(stack.save(new CompoundTag()));
            tag.put("AoeYakCargoOverflow", pending);
        }
    }

    @Override public void aiStep() {
        super.aiStep();
        if (!level().isClientSide && !legacyCargo.isEmpty()) {
            compactLegacyCargo();
            // A loaded entity now has a real world position. Return excess beside
            // it instead of deleting old cargo or retaining hidden storage slots.
            legacyCargo.removeIf(stack -> stack.isEmpty() || spawnAtLocation(stack.copy()) != null);
        }
    }

    @Override protected void dropEquipment() {
        super.dropEquipment();
        for (ItemStack stack : legacyCargo) spawnAtLocation(stack.copy());
        legacyCargo.clear();
    }

    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!isBaby() && held.is(Items.BUCKET)) {
            player.playSound(SoundEvents.COW_MILK, 1, 1);
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, new ItemStack(Items.MILK_BUCKET)));
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override public boolean canMate(Animal other) {
        return other instanceof YakEntity yak && other != this && canParent() && yak.canParent();
    }

    @Override public YakEntity getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.YAK.get().create(level);
    }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.COW_AMBIENT; }
    @Override protected SoundEvent getAngrySound() { return SoundEvents.COW_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.COW_DEATH; }
    @Override protected SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) { return SoundEvents.COW_HURT; }
    @Override protected SoundEvent getEatingSound() { return SoundEvents.HORSE_EAT; }

    public YakEntity caravanHead() { return caravanHead; }
    public YakEntity caravanTail() { return caravanTail; }
    public void joinCaravan(YakEntity head) { caravanHead = head; head.caravanTail = this; }
    public void leaveCaravan() {
        if (caravanHead != null && caravanHead.caravanTail == this) caravanHead.caravanTail = null;
        caravanHead = null;
    }
}
