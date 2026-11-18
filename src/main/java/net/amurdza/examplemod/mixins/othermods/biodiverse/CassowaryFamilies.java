package net.amurdza.examplemod.mixins.othermods.biodiverse;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.config.MobConfig;
import net.amurdza.examplemod.util.Helper;
import net.mcreator.biodiverse.entity.CassowaryEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.EnumSet;

/** Keep egg laying, while restoring vanilla spawn-egg offspring and family movement. */
@Mixin(CassowaryEntity.class)
public abstract class CassowaryFamilies {
    private static final TagKey<Item> AOEMOD_FRUIT = TagKey.create(Registries.ITEM, AOEMod.makeID("cassowary_food"));
    @Unique private int aoemod$eggTime = -1;
    @Unique private int aoemod$featherTime = -1;

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void aoemod$passiveEggsAndFeathers(CallbackInfo ci) {
        var bird = (CassowaryEntity) (Object) this;
        if (bird.level().isClientSide || !bird.isAlive() || bird.isBaby()) return;
        if (aoemod$eggTime < 0) aoemod$eggTime = 6000 + bird.getRandom().nextInt(6000);
        if (aoemod$featherTime < 0) aoemod$featherTime = 6000 + bird.getRandom().nextInt(6000);
        int increments = Helper.computeIncrements(bird.getRandom(), MobConfig.mobGrowthChance(bird));
        // Chicken eggs retain their native one-tick minimum; feather shedding uses the configured rate.
        aoemod$eggTime -= Math.max(1, increments);
        aoemod$featherTime -= increments;
        if (aoemod$eggTime <= 0) {
            bird.spawnAtLocation(net.mcreator.biodiverse.init.BiodiverseModItems.CASSOWARY_EGG.get());
            bird.playSound(SoundEvents.CHICKEN_EGG, 1.0F, (bird.getRandom().nextFloat() - bird.getRandom().nextFloat()) * .2F + 1.0F);
            bird.gameEvent(GameEvent.ENTITY_PLACE);
            aoemod$eggTime = 6000 + bird.getRandom().nextInt(6000);
        }
        if (aoemod$featherTime <= 0) {
            bird.spawnAtLocation(Items.FEATHER);
            bird.gameEvent(GameEvent.ENTITY_PLACE);
            aoemod$featherTime = 6000 + bird.getRandom().nextInt(6000);
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void aoemod$saveYields(CompoundTag tag, CallbackInfo ci) {
        tag.putInt("AOEModCassowaryEggTime", aoemod$eggTime);
        tag.putInt("AOEModFeatherDropTime", aoemod$featherTime);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void aoemod$loadYields(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains("AOEModCassowaryEggTime")) aoemod$eggTime = tag.getInt("AOEModCassowaryEggTime");
        if (tag.contains("AOEModFeatherDropTime")) aoemod$featherTime = tag.getInt("AOEModFeatherDropTime");
    }

    @Inject(method = "getBreedOffspring", at = @At("HEAD"), cancellable = true)
    private void aoemod$createBaby(ServerLevel level, AgeableMob partner, CallbackInfoReturnable<AgeableMob> cir) {
        var parent = (CassowaryEntity) (Object) this;
        var child = (CassowaryEntity) parent.getType().create(level);
        if (child != null) child.setTexture("cassowarybaby");
        cir.setReturnValue(child);
    }

    @Inject(method = "isFood", at = @At("RETURN"), cancellable = true)
    private void aoemod$eatFruit(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.is(AOEMOD_FRUIT)) cir.setReturnValue(true);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void aoemod$familyGoals(CallbackInfo ci) {
        var bird = (CassowaryEntity) (Object) this;
        var followParent = new FollowParentGoal(bird, 1.1);
        // The native bird has a resting goal which stops navigation. Following
        // must own MOVE so the lower-priority rest/stroll goals cannot stop it.
        followParent.setFlags(EnumSet.of(Goal.Flag.MOVE));
        bird.goalSelector.addGoal(4, followParent);
        // Native resting owns MOVE at priority 5; food should wake a resting bird.
        bird.goalSelector.addGoal(3, new TemptGoal(bird, 1.0, Ingredient.of(AOEMOD_FRUIT), false));
    }
}

