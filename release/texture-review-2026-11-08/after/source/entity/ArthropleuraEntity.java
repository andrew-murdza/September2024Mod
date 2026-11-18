package net.amurdza.examplemod.entity;

import net.amurdza.examplemod.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Original herbivorous giant millipede: breedable, grazing rainforest livestock. */
public class ArthropleuraEntity extends Animal {
    public ArthropleuraEntity(EntityType<? extends ArthropleuraEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(0.6F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes().add(Attributes.MAX_HEALTH, 24)
                .add(Attributes.MOVEMENT_SPEED, 0.18).add(Attributes.ARMOR, 4)
                .add(Attributes.ATTACK_DAMAGE, 2).add(Attributes.FOLLOW_RANGE, 12);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1, true));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1, Ingredient.of(ItemTags.LEAVES), false));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1,
                Ingredient.of(Items.FERN, Items.BROWN_MUSHROOM, Items.RED_MUSHROOM), false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(5, new EatBlockGoal(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ItemTags.LEAVES) || stack.is(Items.FERN)
                || stack.is(Items.BROWN_MUSHROOM) || stack.is(Items.RED_MUSHROOM);
    }

    @Override
    public @Nullable ArthropleuraEntity getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.ARTHROPLEURA.get().create(level);
    }

    @Override
    public void ate() {
        super.ate();
        this.heal(1);
        if (this.isBaby()) this.ageUp(20);
    }

    @Override
    public MobType getMobType() { return MobType.ARTHROPOD; }

    public static boolean checkSpawnRules(EntityType<ArthropleuraEntity> type, ServerLevelAccessor level,
                                          MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getRawBrightness(pos, 0) > 8
                && level.getBlockState(pos.below()).isSolidRender(level, pos.below())
                && level.getFluidState(pos).isEmpty();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(SoundEvents.SPIDER_STEP, 0.15F, 0.7F);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.SPIDER_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.SPIDER_DEATH; }
}
