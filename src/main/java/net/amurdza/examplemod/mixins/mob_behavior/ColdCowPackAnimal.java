package net.amurdza.examplemod.mixins.mob_behavior;

import net.amurdza.examplemod.entity.ai.YakAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.OwnableEntity;
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

@Mixin(Cow.class)
public abstract class ColdCowPackAnimal extends Animal implements YakAccess, OwnableEntity {
    @Unique private static final EntityDataAccessor<Optional<UUID>> AOE_YAK_OWNER =
            SynchedEntityData.defineId(Cow.class, EntityDataSerializers.OPTIONAL_UUID);
    @Unique private static final EntityDataAccessor<Boolean> AOE_YAK_CHEST =
            SynchedEntityData.defineId(Cow.class, EntityDataSerializers.BOOLEAN);
    @Unique private static final EntityDataAccessor<Boolean> AOE_YAK_STAY =
            SynchedEntityData.defineId(Cow.class, EntityDataSerializers.BOOLEAN);
    @Unique private final SimpleContainer aoe$cargo = new SimpleContainer(27);

    protected ColdCowPackAnimal(EntityType<? extends Animal> type, Level level) { super(type, level); }

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

    @Override @Nullable public UUID aoe$yakOwner() { return entityData.get(AOE_YAK_OWNER).orElse(null); }
    @Override public void aoe$setYakOwner(@Nullable UUID owner) { entityData.set(AOE_YAK_OWNER, Optional.ofNullable(owner)); }
    @Override @Nullable public UUID getOwnerUUID() { return aoe$yakOwner(); }
    @Override public boolean aoe$hasYakChest() { return entityData.get(AOE_YAK_CHEST); }
    @Override public void aoe$setYakChest(boolean value) { entityData.set(AOE_YAK_CHEST, value); }
    @Override public boolean aoe$yakStaying() { return entityData.get(AOE_YAK_STAY); }
    @Override public void aoe$setYakStaying(boolean value) { entityData.set(AOE_YAK_STAY, value); }
    @Override public SimpleContainer aoe$yakInventory() { return aoe$cargo; }
}
