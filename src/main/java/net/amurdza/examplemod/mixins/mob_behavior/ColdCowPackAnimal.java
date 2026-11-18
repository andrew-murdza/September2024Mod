package net.amurdza.examplemod.mixins.mob_behavior;

import net.amurdza.examplemod.entity.ai.YakAccess;
import net.amurdza.examplemod.event_handlers.ColdCowYaks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.UUID;

/** Only reads the old pack NBT so LegacyYakMigration can transfer it to the dedicated entity. */
@Mixin(Cow.class)
public abstract class ColdCowPackAnimal extends Animal implements YakAccess {
    @Unique private static final EntityDataAccessor<Optional<UUID>> AOE_YAK_OWNER =
            SynchedEntityData.defineId(Cow.class, EntityDataSerializers.OPTIONAL_UUID);
    @Unique private static final EntityDataAccessor<Boolean> AOE_YAK_CHEST =
            SynchedEntityData.defineId(Cow.class, EntityDataSerializers.BOOLEAN);
    @Unique private static final EntityDataAccessor<Boolean> AOE_YAK_STAY =
            SynchedEntityData.defineId(Cow.class, EntityDataSerializers.BOOLEAN);
    @Unique private final SimpleContainer aoe$cargo = new SimpleContainer(30);
    @Unique private Cow aoe$caravanHead;
    @Unique private Cow aoe$caravanTail;


    protected ColdCowPackAnimal(EntityType<? extends Animal> type, Level level) { super(type, level); }

    @Override
    protected Component getTypeName() {
        return ColdCowYaks.isColdCow((Cow) (Object) this) ? Component.translatable("entity.aoemod.yak") : super.getTypeName();
    }

    // Cow inherits defineSynchedData, so initialize at its own constructor rather than
    // injecting an inherited method and accidentally changing every Animal subclass.
    @Inject(method = "<init>", at = @At("TAIL"))
    private void aoe$definePackData(EntityType<? extends Cow> type, Level level, CallbackInfo callback) {
        entityData.define(AOE_YAK_OWNER, Optional.empty());
        entityData.define(AOE_YAK_CHEST, false);
        entityData.define(AOE_YAK_STAY, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        UUID owner = aoe$yakOwner();
        if (owner != null) {
            tag.putUUID("AoeYakOwner", owner);
            tag.putBoolean("AoeYakChest", aoe$hasYakChest());
            tag.putBoolean("AoeYakStay", aoe$yakStaying());
            ListTag cargo = new ListTag();
            for (int slot = 0; slot < aoe$cargo.getContainerSize(); slot++) {
                ItemStack stack = aoe$cargo.getItem(slot);
                if (!stack.isEmpty()) {
                    CompoundTag entry = stack.save(new CompoundTag());
                    entry.putByte("Slot", (byte) slot);
                    cargo.add(entry);
                }
            }
            tag.put("AoeYakCargo", cargo);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        aoe$setYakOwner(tag.hasUUID("AoeYakOwner") ? tag.getUUID("AoeYakOwner") : null);
        aoe$setYakChest(aoe$yakOwner() != null && tag.getBoolean("AoeYakChest"));
        aoe$setYakStaying(tag.getBoolean("AoeYakStay"));
        aoe$cargo.clearContent();
        ListTag cargo = tag.getList("AoeYakCargo", Tag.TAG_COMPOUND);
        for (int i = 0; i < cargo.size(); i++) {
            CompoundTag entry = cargo.getCompound(i);
            int slot = entry.getByte("Slot") & 255;
            if (slot < aoe$cargo.getContainerSize()) aoe$cargo.setItem(slot, ItemStack.of(entry));
        }
    }

    @Override public Cow aoe$caravanHead() { return aoe$caravanHead; }
    @Override public Cow aoe$caravanTail() { return aoe$caravanTail; }
    @Override public void aoe$setCaravanHead(Cow head) { aoe$caravanHead = head; }
    @Override public void aoe$setCaravanTail(Cow tail) { aoe$caravanTail = tail; }

    @Override @Nullable public UUID aoe$yakOwner() { return entityData.get(AOE_YAK_OWNER).orElse(null); }
    @Override public void aoe$setYakOwner(@Nullable UUID owner) { entityData.set(AOE_YAK_OWNER, Optional.ofNullable(owner)); }
    @Override public boolean aoe$hasYakChest() { return entityData.get(AOE_YAK_CHEST); }
    @Override public void aoe$setYakChest(boolean value) { entityData.set(AOE_YAK_CHEST, value); }
    @Override public boolean aoe$yakStaying() { return entityData.get(AOE_YAK_STAY); }
    @Override public void aoe$setYakStaying(boolean value) { entityData.set(AOE_YAK_STAY, value); }
    @Override public SimpleContainer aoe$yakInventory() { return aoe$cargo; }
}
