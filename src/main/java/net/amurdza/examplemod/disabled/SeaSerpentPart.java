//package net.amurdza.examplemod.entity;
//
//import net.minecraft.nbt.CompoundTag;
//import net.minecraft.world.damagesource.DamageSource;
//import net.minecraft.world.entity.EntityDimensions;
//import net.minecraft.world.entity.Pose;
//import net.minecraftforge.entity.PartEntity;
//import org.jetbrains.annotations.NotNull;
//
//public class SeaSerpentPart extends PartEntity<SeaSerpentEntity> {
//    private final EntityDimensions size;
//
//    public SeaSerpentPart(SeaSerpentEntity parent, float width, float height) {
//        super(parent);
//        this.size = EntityDimensions.scalable(width, height);
//        this.refreshDimensions();
//    }
//
//    @Override
//    protected void readAdditionalSaveData(@NotNull CompoundTag tag) {
//    }
//
//    @Override
//    protected void addAdditionalSaveData(@NotNull CompoundTag tag) {
//    }
//
//
//
//    @Override
//    protected void defineSynchedData() {
//    }
//
//    @Override
//    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
//        return this.size;
//    }
//
//    @Override
//    public boolean isPickable() {
//        return true;
//    }
//
//    @Override
//    public boolean hurt(@NotNull DamageSource source, float amount) {
//        return this.getParent().hurt(source, amount * 0.85F);
//    }
//
//    @Override
//    public boolean shouldBeSaved() {
//        return false;
//    }
//
//}