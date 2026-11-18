package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Keep the selected orcs' native combat, without their morale/flee machinery. */
@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID)
public final class OrcCombatRules {
    private OrcCombatRules() { }

    public static boolean isSelected(Entity entity) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return id != null && id.getNamespace().equals("orcz")
                && (id.getPath().equals("orcsavagemace") || id.getPath().equals("orcshield")
                || id.getPath().equals("bigorcboss"));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void joined(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !isSelected(event.getEntity())
                || !(event.getEntity() instanceof Mob orc)) return;
        orc.goalSelector.getAvailableGoals().stream().map(goal -> goal.getGoal())
                .filter(AvoidEntityGoal.class::isInstance).toList().forEach(orc.goalSelector::removeGoal);
        // Old saves can carry morale effects even after their producers stop running.
        for (var effect : java.util.List.copyOf(orc.getActiveEffects())) {
            var id = ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect());
            if (id != null && id.getNamespace().equals("orcz")) orc.removeEffect(effect.getEffect());
        }
        orc.getPersistentData().remove("hitrun_flee");
        orc.getPersistentData().remove("block_ready");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void preventPsychology(MobEffectEvent.Applicable event) {
        var id = ForgeRegistries.MOB_EFFECTS.getKey(event.getEffectInstance().getEffect());
        if (isSelected(event.getEntity()) && id != null && id.getNamespace().equals("orcz")) {
            event.setResult(Event.Result.DENY);
        }
    }
}
