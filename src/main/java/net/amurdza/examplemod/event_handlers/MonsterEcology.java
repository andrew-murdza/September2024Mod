package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.SpawnCollisionSafety;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import twilightforest.entity.ai.goal.RedcapLightTNTGoal;
import twilightforest.entity.ai.goal.RedcapPlantTNTGoal;
import twilightforest.entity.monster.Redcap;


@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public final class MonsterEcology {
    private MonsterEcology() { }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void joined(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof Redcap redcap) {
            redcap.goalSelector.getAvailableGoals().removeIf(goal -> goal.getGoal() instanceof RedcapLightTNTGoal
                    || goal.getGoal() instanceof RedcapPlantTNTGoal);
        }
        if (event.getEntity() instanceof Drowned drowned
                && GameplayDimensions.is(event.getLevel(), GameplayDimensions.TROPICAL)) {
            for (EquipmentSlot slot : EquipmentSlot.values()) drowned.setItemSlot(slot, ItemStack.EMPTY);
            drowned.setCanPickUpLoot(false);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void safeSpawn(MobSpawnEvent.PositionCheck event) {
        if (event.getSpawnType() == net.minecraft.world.entity.MobSpawnType.SPAWN_EGG
                || event.getSpawnType() == net.minecraft.world.entity.MobSpawnType.COMMAND) return;
        if (!SpawnCollisionSafety.hasNoPowderCane(event.getLevel(), event.getEntity().getBoundingBox())) {
            event.setResult(Event.Result.DENY);
        }
    }
}
