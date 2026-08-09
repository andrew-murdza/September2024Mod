package net.amurdza.examplemod.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class MaceItem extends Item {
    private static final UUID ATTACK_DAMAGE_UUID = UUID.fromString("4c0a8038-9f89-4f5f-8df0-5c37f0f6f26a");
    private static final UUID ATTACK_SPEED_UUID = UUID.fromString("a6dfddc9-73e6-4c87-b183-dcdfd2409ca6");

    private final Multimap<Attribute, AttributeModifier> attributes;

    public MaceItem(Properties properties) {
        super(properties);
        this.attributes = ImmutableMultimap.<Attribute, AttributeModifier>builder()
                .put(Attributes.ATTACK_DAMAGE, new AttributeModifier(ATTACK_DAMAGE_UUID, "Mace damage",
                        5.0D, AttributeModifier.Operation.ADDITION))
                .put(Attributes.ATTACK_SPEED, new AttributeModifier(ATTACK_SPEED_UUID, "Mace speed",
                        -3.4D, AttributeModifier.Operation.ADDITION))
                .build();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.attributes : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, entity -> entity.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return true;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    public static float fallBonusDamage(float fallDistance) {
        float effectiveFall = Math.max(0.0F, fallDistance - 1.5F);
        if (effectiveFall <= 3.0F) {
            return effectiveFall * 4.0F;
        }
        if (effectiveFall <= 8.0F) {
            return 12.0F + (effectiveFall - 3.0F) * 2.0F;
        }
        return 22.0F + (effectiveFall - 8.0F);
    }
}
