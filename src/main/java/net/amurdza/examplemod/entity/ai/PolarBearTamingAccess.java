package net.amurdza.examplemod.entity.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface PolarBearTamingAccess {
    boolean aoe$isTame();

    @Nullable
    UUID aoe$getOwnerUUID();

    void aoe$setOwnerUUID(@Nullable UUID ownerUUID);

    boolean aoe$isOwnedBy(Player player);

    boolean aoe$canAttackOwnerTarget(@Nullable LivingEntity target);
}
