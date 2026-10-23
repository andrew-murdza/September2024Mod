package net.amurdza.examplemod.entity.ai;

import net.minecraft.world.SimpleContainer;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/** Persistent pack-animal state added only when a cold cow is tamed. */
public interface YakAccess {
    @Nullable UUID aoe$yakOwner();
    void aoe$setYakOwner(@Nullable UUID owner);
    boolean aoe$hasYakChest();
    void aoe$setYakChest(boolean value);
    boolean aoe$yakStaying();
    void aoe$setYakStaying(boolean value);
    SimpleContainer aoe$yakInventory();
}
