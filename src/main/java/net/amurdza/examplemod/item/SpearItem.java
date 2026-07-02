package net.amurdza.examplemod.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraftforge.common.ForgeMod;

import java.util.Arrays;
import java.util.UUID;

/** A 1.20.1 implementation of the post-1.20 kinetic spear. */
public class SpearItem extends TieredItem {
    private static final UUID ATTACK_DAMAGE_UUID = UUID.fromString("cb3f55d3-645c-4f38-a497-9c13a33db5cf");
    private static final UUID ATTACK_SPEED_UUID = UUID.fromString("fa233e1c-4180-4865-b01b-bcce9785aca3");
    private static final UUID REACH_UUID = UUID.fromString("0bc4ba47-4829-46aa-94ef-595cb58c0f8a");
    private static final String HIT_IDS = "AOESpearHitIds";

    private final Tier tier;
    private final int attackDelayTicks;
    private final float damageMultiplier;
    private final float dismountThreshold;
    private final float knockbackThreshold;
    private final float damageThreshold;
    private final Multimap<Attribute, AttributeModifier> attributes;

    public SpearItem(Tier tier, float attackDuration, float damageMultiplier, float delay,
                     float dismountThreshold, float knockbackThreshold, float damageThreshold,
                     Properties properties) {
        super(tier, properties);
        this.tier = tier;
        this.attackDelayTicks = Math.round(delay * 20.0F);
        this.damageMultiplier = damageMultiplier;
        this.dismountThreshold = dismountThreshold;
        this.knockbackThreshold = knockbackThreshold;
        this.damageThreshold = damageThreshold;
        this.attributes = ImmutableMultimap.<Attribute, AttributeModifier>builder()
                .put(Attributes.ATTACK_DAMAGE, new AttributeModifier(ATTACK_DAMAGE_UUID, "Spear damage",
                        tier.getAttackDamageBonus(), AttributeModifier.Operation.ADDITION))
                .put(Attributes.ATTACK_SPEED, new AttributeModifier(ATTACK_SPEED_UUID, "Spear speed",
                        1.0D / attackDuration - 4.0D, AttributeModifier.Operation.ADDITION))
                .put(ForgeMod.ENTITY_REACH.get(), new AttributeModifier(REACH_UUID, "Spear reach",
                        1.5D, AttributeModifier.Operation.ADDITION))
                .build();
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.attributes : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getDamageValue() >= stack.getMaxDamage() - 1) {
            return InteractionResultHolder.fail(stack);
        }
        stack.getOrCreateTag().remove(HIT_IDS);
        player.startUsingItem(hand);
        level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.7F, 1.25F);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseDuration) {
        int usedTicks = getUseDuration(stack) - remainingUseDuration;
        if (level.isClientSide || usedTicks < this.attackDelayTicks) {
            return;
        }

        Vec3 start = user.getEyePosition();
        Vec3 end = start.add(user.getViewVector(1.0F).scale(4.5D));
        AABB search = user.getBoundingBox().expandTowards(user.getViewVector(1.0F).scale(4.5D)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, user, start, end, search,
                entity -> entity instanceof LivingEntity && entity != user && entity.isPickable() && !entity.isSpectator());
        if (hit == null || !(hit.getEntity() instanceof LivingEntity target) || wasHit(stack, target.getId())) {
            return;
        }

        double relativeSpeed = user.getDeltaMovement().subtract(target.getDeltaMovement()).length() * 20.0D;
        if (relativeSpeed < this.damageThreshold) {
            return;
        }

        float damage = 1.0F + this.tier.getAttackDamageBonus()
                + (float) ((relativeSpeed - this.damageThreshold) * this.damageMultiplier);
        boolean hurt = target.hurt(user instanceof Player player
                        ? level.damageSources().playerAttack(player)
                        : level.damageSources().mobAttack(user),
                damage);
        if (!hurt) {
            return;
        }

        rememberHit(stack, target.getId());
        if (relativeSpeed >= this.knockbackThreshold) {
            Vec3 look = user.getViewVector(1.0F);
            target.knockback(Math.min(2.0D, relativeSpeed / 10.0D), -look.x, -look.z);
        }
        if (relativeSpeed >= this.dismountThreshold && target.isPassenger()) {
            target.stopRiding();
        }

        level.playSound(null, target.blockPosition(), SoundEvents.TRIDENT_HIT, SoundSource.PLAYERS, 1.0F, 1.0F);
        stack.hurtAndBreak(1, user, entity -> entity.broadcastBreakEvent(user.getUsedItemHand()));
        if (user instanceof Player player) {
            player.awardStat(Stats.ITEM_USED.get(this));
        }
    }

    private static boolean wasHit(ItemStack stack, int id) {
        return Arrays.stream(stack.getOrCreateTag().getIntArray(HIT_IDS)).anyMatch(value -> value == id);
    }

    private static void rememberHit(ItemStack stack, int id) {
        CompoundTag tag = stack.getOrCreateTag();
        int[] oldIds = tag.getIntArray(HIT_IDS);
        int[] newIds = Arrays.copyOf(oldIds, oldIds.length + 1);
        newIds[oldIds.length] = id;
        tag.putIntArray(HIT_IDS, newIds);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        stack.getOrCreateTag().remove(HIT_IDS);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, entity -> entity.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return true;
    }

    @Override
    public boolean canAttackBlock(net.minecraft.world.level.block.state.BlockState state,
                                  Level level, net.minecraft.core.BlockPos pos, Player player) {
        return !player.isCreative();
    }
}
