package net.amurdza.examplemod.event_handlers;

import com.github.alexthe666.iceandfire.entity.IafEntityRegistry;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.entity.ArchlichEntity;
import net.amurdza.examplemod.entity.IllagerLordEntity;
import net.amurdza.examplemod.entity.SpiderQueenEntity;
import net.amurdza.examplemod.registry.ModEntities;
import net.amurdza.examplemod.registry.ModItems;
import net.amurdza.examplemod.util.ModTags;
import net.mcreator.borninchaosv.init.BornInChaosV1ModEntities;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MobTierBalancing {
    private static final String CHECKED_STATS_TAG = "aoe.checkedMobTierStats";
    private static final String ZOMBIE_HORSEMAN_RIDER_TAG = "aoe.zombieHorsemanRider";
    private static final String SKELETON_HORSEMAN_RIDER_TAG = "aoe.skeletonHorsemanRider";

    private static final ResourceLocation ALEX_CENTIPEDE_HEAD = new ResourceLocation("alexsmobs", "centipede_head");
    private static final ResourceLocation QUARK_WRAITH = new ResourceLocation("quark", "wraith");

    private MobTierBalancing() {}

    @SubscribeEvent
    public static void onMobJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof Mob mob)) {
            return;
        }

        tuneOneTimeStats(mob);
        tuneOneTimeEquipment(mob);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }

        if (entity instanceof Warden warden) {
            helpWardenSeeLikeARegularEliteMob(level, warden);
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        DamageSource source = event.getSource();
        Entity attackerEntity = source.getEntity();

        if (!(attackerEntity instanceof LivingEntity attacker) || source.getDirectEntity() != attacker) {
            return;
        }

        if (target.getType() == EntityType.GHAST && source.getDirectEntity() instanceof LargeFireball && source.getEntity() instanceof Player) {
            event.setAmount(event.getAmount() * 0.3F);
        }

        if (attacker instanceof Husk) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0), attacker);
        } else if (attacker instanceof Drowned) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 0), attacker);
        } else if (attacker instanceof WitherSkeleton) {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 0), attacker);
        } else if (attacker instanceof Mob attackerMob && isType(attackerMob, QUARK_WRAITH)) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 0), attacker);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 0), attacker);
        }
    }

    private static void tuneOneTimeStats(Mob mob) {
        if (mob.getTags().contains(CHECKED_STATS_TAG)) {
            return;
        }
        mob.addTag(CHECKED_STATS_TAG);

        EntityType<?> type = mob.getType();

        if (type == ModEntities.CREAKING.get()) {
            stats(mob, 18.0D, 1.0D, 0.20D, 4.0D, 0.2D);
        } else if (type == EntityType.SPIDER) {
            stats(mob, 12.0D, 2.0D, 0.30D, 0.0D, 0.0D);
        } else if (type == EntityType.CAVE_SPIDER) {
            stats(mob, 18.0D, 2.0D, 0.33D, 1.0D, 0.0D);
        } else if (type == ModEntities.BOGGED.get()) {
            stats(mob, 22.0D, 2.0D, 0.24D, 1.0D, 0.0D);
        } else if (isType(mob, ALEX_CENTIPEDE_HEAD)) {
            stats(mob, 38.0D, 4.5D, 0.18D, 7.0D, 0.35D);
        } else if (mob instanceof SpiderQueenEntity) {
            stats(mob, 150.0D, 8.5D, 0.31D, 6.0D, 0.35D);
        } else if (mob.getTags().contains(ZOMBIE_HORSEMAN_RIDER_TAG)) {
            stats(mob, 22.0D, 1.5D, 0.22D, 0.0D, 0.0D);
        } else if (mob.getTags().contains(SKELETON_HORSEMAN_RIDER_TAG)) {
            stats(mob, 24.0D, 2.0D, 0.24D, 0.0D, 0.0D);
        } else if (type == EntityType.ZOMBIE) {
            stats(mob, 18.0D, 2.5D, 0.22D, 0.0D, 0.0D);
        } else if (type == EntityType.SKELETON) {
            tuneSkeletonStats(mob);
        } else if (type == BornInChaosV1ModEntities.SENOR_PUMPKIN.get()) {
            stats(mob, 16.0D, 3.0D, 0.28D, 0.0D, 0.0D);
        } else if (type == BornInChaosV1ModEntities.SIR_PUMPKINHEAD.get()) {
            stats(mob, 32.0D, 2.0D, 0.29D, 2.0D, 0.1D);
        } else if (type == ModEntities.ZOMBIE_HORSEMAN.get() || type == ModEntities.SKELETON_HORSEMAN.get()) {
            stats(mob, type == ModEntities.SKELETON_HORSEMAN.get() ? 36.0D : 28.0D, 0.0D, 0.28D, 2.0D, 0.15D);
            if (mob instanceof AbstractHorse horse) {
                set(horse, Attributes.JUMP_STRENGTH, 0.65D);
            }
        } else if (type == BornInChaosV1ModEntities.LORD_PUMPKINHEAD.get()) {
            stats(mob, 190.0D, 11.0D, 0.28D, 8.0D, 0.45D);
        } else if (type == EntityType.PILLAGER) {
            stats(mob, 20.0D, 2.0D, 0.25D, 0.0D, 0.0D);
        } else if (mob instanceof Ravager) {
            stats(mob, 40.0D, 4.0D, 0.23D, 2.0D, 0.2D);
        } else if (type == EntityType.ZOMBIE_VILLAGER) {
            stats(mob, 20.0D, 3.0D, 0.23D, 2.0D, 0.0D);
        } else if (type == EntityType.WITCH) {
            stats(mob, 36.0D, 2.0D, 0.25D, 2.0D, 0.0D);
        } else if (type == EntityType.EVOKER) {
            stats(mob, 42.0D, 2.0D, 0.23D, 3.0D, 0.0D);
        } else if (type == EntityType.VEX) {
            stats(mob, 10.0D, 2.0D, 0.30D, 0.0D, 0.0D);
        } else if (type == EntityType.VINDICATOR) {
            stats(mob, 28.0D, 3.0D, 0.29D, 2.0D, 0.05D);
        } else if (mob instanceof IllagerLordEntity) {
            stats(mob, 240.0D, 14.0D, 0.29D, 10.0D, 0.5D);
        } else if (mob instanceof Stray) {
            stats(mob, 22.0D, 2.0D, 0.25D, 1.0D, 0.0D);
        } else if (type == ModEntities.BREEZE.get()) {
            stats(mob, 28.0D, 3.0D, 0.60D, 1.0D, 0.0D);
        } else if (mob instanceof Drowned) {
            stats(mob, 20.0D, 0.5D, 0.21D, 0.0D, 0.0D);
        } else if (type == IafEntityRegistry.DREAD_THRALL.get()) {
            stats(mob, 25.0D, 3.5D, 0.23D, 2.0D, 0.0D);
        } else if (type == IafEntityRegistry.DREAD_LICH.get()) {
            stats(mob, 38.0D, 3.0D, 0.24D, 2.0D, 0.05D);
        } else if (type == IafEntityRegistry.DREAD_KNIGHT.get()) {
            stats(mob, 50.0D, 5.0D, 0.18D, 15.0D, 0.35D);
        } else if (type == IafEntityRegistry.TROLL.get()) {
            stats(mob, 80.0D, 8.0D, 0.18D, 8.0D, 0.55D);
        } else if (mob instanceof ArchlichEntity) {
            stats(mob, 280.0D, 16.0D, 0.24D, 12.0D, 0.55D);
        } else if (type == EntityType.HUSK) {
            stats(mob, 28.0D, 3.5D, 0.23D, 3.0D, 0.0D);
        } else if (type == ModEntities.PARCHED.get()) {
            stats(mob, 24.0D, 2.0D, 0.25D, 2.0D, 0.0D);
        } else if (type == ModEntities.CAMEL_HUSK_JOCKEY.get()) {
            stats(mob, 38.0D, 0.0D, 0.25D, 2.0D, 0.2D);
        } else if (mob instanceof EnderMan) {
            stats(mob, 32.0D, 5.0D, 0.25D, 2.0D, 0.1D);
        } else if (mob instanceof Shulker) {
            stats(mob, 30.0D, 3.0D, 0.0D, 1.0D, 1.0D);
        } else if (mob instanceof Warden) {
            stats(mob, 70.0D, 8.0D, 0.17D, 6.0D, 0.4D);
        } else if (type == EntityType.GHAST) {
            stats(mob, 32.0D, 6.0D, 0.70D, 2.0D, 0.0D);
        } else if (isType(mob, QUARK_WRAITH)) {
            stats(mob, 32.0D, 4.5D, 0.30D, 2.0D, 0.1D);
        } else if (mob instanceof Zoglin) {
            stats(mob, 20.0D, 3.0D, 0.23D, 1.0D, 0.15D);
        } else if (mob instanceof Creeper) {
            if (mob.level().getBiome(mob.blockPosition()).is(ModTags.Biomes.warpedForestBiomes)) {
                stats(mob, 10.0D, 0.0D, 0.25D, 0.0D, 0.0D);
            }
        } else if (mob instanceof ZombifiedPiglin) {
            stats(mob, 20.0D, 1.0D, 0.23D, 1.0D, 0.05D);
        } else if (mob instanceof WitherSkeleton) {
            stats(mob, 70.0D, 3.5D, 0.23D, 8.0D, 0.25D);
        } else if (mob instanceof MagmaCube) {
            tuneMagmaCubeStats((MagmaCube) mob);
        } else if (type == EntityType.BLAZE) {
            stats(mob, 32.0D, 4.0D, 0.23D, 2.0D, 0.0D);
        } else if (mob instanceof WitherBoss) {
            setMaxHealth(mob, 300.0D);
        }

        tuneJungleSlimeStats(mob);
    }

    private static void tuneOneTimeEquipment(Mob mob) {
        EntityType<?> type = mob.getType();

        if (type == BornInChaosV1ModEntities.SIR_PUMPKINHEAD.get()) {
            equip(mob, EquipmentSlot.MAINHAND, Items.IRON_SWORD);
        } else if (type == BornInChaosV1ModEntities.LORD_PUMPKINHEAD.get()) {
            equip(mob, EquipmentSlot.MAINHAND, Items.IRON_SWORD);
        } else if (mob.getTags().contains(ZOMBIE_HORSEMAN_RIDER_TAG)) {
            equip(mob, EquipmentSlot.MAINHAND, ModItems.IRON_SPEAR.get());
            equipLeatherArmor(mob);
        } else if (mob.getTags().contains(SKELETON_HORSEMAN_RIDER_TAG)) {
            ItemStack bow = new ItemStack(Items.BOW);
            bow.enchant(Enchantments.POWER_ARROWS, 1);
            equip(mob, EquipmentSlot.MAINHAND, bow);
            equipLeatherArmor(mob);
        } else if (mob instanceof Drowned) {
            equip(mob, EquipmentSlot.MAINHAND, ModItems.COPPER_SWORD.get());
            equip(mob, EquipmentSlot.HEAD, ModItems.COPPER_HELMET.get());
            equip(mob, EquipmentSlot.CHEST, ModItems.COPPER_CHESTPLATE.get());
            equip(mob, EquipmentSlot.LEGS, ModItems.COPPER_LEGGINGS.get());
            equip(mob, EquipmentSlot.FEET, ModItems.COPPER_BOOTS.get());
        } else if (type == IafEntityRegistry.DREAD_THRALL.get()) {
            clearEquipment(mob);
        } else if (mob instanceof ZombifiedPiglin) {
            equip(mob, EquipmentSlot.MAINHAND, Items.GOLDEN_SWORD);
        } else if (mob instanceof WitherSkeleton) {
            equip(mob, EquipmentSlot.MAINHAND, Items.IRON_SWORD);
        }
    }

    private static void tuneSkeletonStats(Mob mob) {
        if (mob.level().getBiome(mob.blockPosition()).is(ModTags.Biomes.soulSandValleyBiomes)) {
            stats(mob, 30.0D, 2.0D, 0.25D, 2.0D, 0.05D);
        } else {
            stats(mob, 16.0D, 2.0D, 0.23D, 0.0D, 0.0D);
        }
    }

    private static void tuneJungleSlimeStats(Mob mob) {
        if (!(mob instanceof Slime slime) || mob instanceof MagmaCube || mob.getType() != EntityType.SLIME) {
            return;
        }
        if (!mob.level().getBiome(mob.blockPosition()).is(ModTags.Biomes.tropicalBiomes)) {
            return;
        }

        int size = slime.getSize();
        setMaxHealth(slime, Math.max(4.0D, size * 4.0D));
        set(slime, Attributes.ATTACK_DAMAGE, size <= 1 ? 0.0D : 0.5D + size);
        set(slime, Attributes.MOVEMENT_SPEED, 0.18D + size * 0.04D);
        set(slime, Attributes.ARMOR, 0.0D);
        set(slime, Attributes.KNOCKBACK_RESISTANCE, 0.0D);
    }

    private static void tuneMagmaCubeStats(MagmaCube magmaCube) {
        int size = magmaCube.getSize();
        int cappedSize = Math.min(size, 4);

        setMaxHealth(magmaCube, Math.max(12.0D, size * size * 2.0D));
        set(magmaCube, Attributes.ATTACK_DAMAGE, Math.max(3.0D, size * 1.5D));
        set(magmaCube, Attributes.MOVEMENT_SPEED, 0.20D + cappedSize * 0.03D);
        set(magmaCube, Attributes.ARMOR, 2.0D + cappedSize);
        set(magmaCube, Attributes.KNOCKBACK_RESISTANCE, 0.15D);
    }

    private static void helpWardenSeeLikeARegularEliteMob(ServerLevel level, Warden warden) {
        if (warden.tickCount % 20 != 0 || warden.getTarget() != null) {
            return;
        }

        Player target = level.getNearestPlayer(
                TargetingConditions.forCombat().range(32.0D),
                warden
        );
        if (target != null) {
            warden.setTarget(target);
            warden.increaseAngerAt(target, 80, false);
        }
    }

    private static boolean isType(Mob mob, ResourceLocation id) {
        return id.equals(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()));
    }

    private static void stats(
            Mob mob,
            double health,
            double attackDamage,
            double movementSpeed,
            double armor,
            double knockbackResistance
    ) {
        setMaxHealth(mob, health);
        set(mob, Attributes.ATTACK_DAMAGE, attackDamage);
        set(mob, Attributes.MOVEMENT_SPEED, movementSpeed);
        set(mob, Attributes.ARMOR, armor);
        set(mob, Attributes.KNOCKBACK_RESISTANCE, knockbackResistance);
    }

    private static void setMaxHealth(Mob mob, double value) {
        double previousMax = mob.getMaxHealth();
        set(mob, Attributes.MAX_HEALTH, value);
        if (mob.getHealth() >= previousMax || mob.getHealth() <= 0.0F) {
            mob.setHealth((float) value);
        }
    }

    private static void set(Mob mob, Attribute attribute, double value) {
        AttributeInstance instance = mob.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    private static void equip(Mob mob, EquipmentSlot slot, Item item) {
        equip(mob, slot, new ItemStack(item));
    }

    private static void equip(Mob mob, EquipmentSlot slot, ItemStack stack) {
        mob.setItemSlot(slot, stack);
        mob.setDropChance(slot, 0.0F);
    }

    private static void equipLeatherArmor(Mob mob) {
        equip(mob, EquipmentSlot.HEAD, Items.LEATHER_HELMET);
        equip(mob, EquipmentSlot.CHEST, Items.LEATHER_CHESTPLATE);
        equip(mob, EquipmentSlot.LEGS, Items.LEATHER_LEGGINGS);
        equip(mob, EquipmentSlot.FEET, Items.LEATHER_BOOTS);
    }

    private static void clearEquipment(Mob mob) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            mob.setItemSlot(slot, ItemStack.EMPTY);
            mob.setDropChance(slot, 0.0F);
        }
    }
}
