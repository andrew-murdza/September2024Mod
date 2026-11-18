package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.entity.YakEntity;
import net.amurdza.examplemod.entity.ai.YakAccess;
import net.amurdza.examplemod.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Upgrade the previously saved cow-variant yaks without losing ownership, cargo or identity. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public final class LegacyYakMigration {
    private LegacyYakMigration() { }

    public static YakEntity convert(Cow cow, ServerLevel level) {
        var yak = ModEntities.YAK.get().create(level);
        CompoundTag saved = cow.saveWithoutId(new CompoundTag());
        // Cow attributes would otherwise overwrite the mount's slower horse attributes.
        saved.remove("Attributes");
        yak.load(saved);
        if (cow instanceof YakAccess old && old.aoe$yakOwner() != null) {
            yak.setOwnerUUID(old.aoe$yakOwner());
            yak.setTamed(true);
            yak.setPersistenceRequired();
            if (old.aoe$hasYakChest()) yak.equipPack();
            // Previously these mounts needed no saddle. Preserve that ability after the upgrade.
            yak.packInventory().setItem(0, new ItemStack(Items.SADDLE));
            if (old.aoe$hasYakChest()) for (int slot = 0; slot < old.aoe$yakInventory().getContainerSize(); slot++) {
                if (slot < YakEntity.CARGO_SLOTS) yak.packInventory().setItem(slot + 2, old.aoe$yakInventory().getItem(slot).copy());
                else yak.preserveLegacyCargo(old.aoe$yakInventory().getItem(slot));
            }
        }
        return yak;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void load(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Cow cow)
                || cow.getType() != EntityType.COW || !(ColdCowYaks.isColdCow(cow)
                || cow instanceof YakAccess old && old.aoe$yakOwner() != null)) return;
        var yak = convert(cow, level);
        event.setCanceled(true);
        level.addFreshEntity(yak);
    }
}
