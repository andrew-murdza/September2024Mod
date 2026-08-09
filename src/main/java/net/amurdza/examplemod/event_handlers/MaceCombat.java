package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.item.MaceItem;
import net.amurdza.examplemod.registry.ModEnchantments;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MaceCombat {
    private MaceCombat() {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        Entity attackerEntity = event.getSource().getEntity();
        if (!(attackerEntity instanceof LivingEntity attacker)) {
            return;
        }

        ItemStack stack = attacker.getMainHandItem();
        if (!(stack.getItem() instanceof MaceItem) || attacker.fallDistance <= 1.5F || attacker.onGround()) {
            return;
        }

        int density = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.DENSITY.get(), stack);
        int breach = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.BREACH.get(), stack);
        int windBurst = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.WIND_BURST.get(), stack);

        float bonusDamage = MaceItem.fallBonusDamage(attacker.fallDistance)
                + attacker.fallDistance * 0.5F * density;

        if (breach > 0) {
            bonusDamage += (float) event.getEntity().getAttributeValue(Attributes.ARMOR) * 0.15F * breach;
        }

        event.setAmount(event.getAmount() + bonusDamage);

        if (windBurst > 0) {
            Vec3 motion = attacker.getDeltaMovement();
            attacker.setDeltaMovement(motion.x, 0.55D + 0.18D * windBurst, motion.z);
            attacker.hasImpulse = true;
        }

        attacker.resetFallDistance();
    }
}
