package net.amurdza.examplemod.entity.ai;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

public interface DolphinBreedingAccess {
    TagKey<Item> BREEDING_FOOD = TagKey.create(
            Registries.ITEM,
            new ResourceLocation("aoemod", "fish")
    );
    int BABY_START_AGE = -24000;
    int BREEDING_COOLDOWN = 6000;

    int aoe$getAge();

    void aoe$setAge(int age);

    boolean aoe$isBaby();

    boolean aoe$isInLove();

    void aoe$setInLove(@Nullable Player player);

    void aoe$resetLove();

    boolean aoe$canMate(Dolphin other);

    @Nullable
    ServerPlayer aoe$getLoveCause();
}
