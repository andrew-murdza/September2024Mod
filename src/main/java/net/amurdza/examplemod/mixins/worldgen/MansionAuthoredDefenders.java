package net.amurdza.examplemod.mixins.worldgen;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.amurdza.examplemod.util.StructureSpawnedMobData;
import net.amurdza.examplemod.worldgen.structure.AuthoredEncounterProgress;
import net.amurdza.examplemod.worldgen.structure.MansionEncounterAccess;
import net.amurdza.examplemod.worldgen.structure.MansionEncounterData;
import net.amurdza.examplemod.worldgen.structure.MansionDefenderSpawns;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.structures.WoodlandMansionPieces.WoodlandMansionPiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WoodlandMansionPiece.class)
public abstract class MansionAuthoredDefenders implements MansionEncounterAccess {
    @Unique private MansionEncounterData aoemod$encounter;
    @Override public MansionEncounterData aoemod$encounter() { return aoemod$encounter; }
    @Override public void aoemod$encounter(MansionEncounterData data) { aoemod$encounter = data; }

    @Inject(method = "<init>(Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplateManager;Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"))
    private void aoemod$load(StructureTemplateManager manager, CompoundTag tag, CallbackInfo ci) {
        aoemod$encounter = MansionEncounterData.load(tag.getCompound("AOEEncounter"));
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void aoemod$save(StructurePieceSerializationContext context, CompoundTag tag, CallbackInfo ci) {
        if (aoemod$encounter != null) tag.put("AOEEncounter", aoemod$encounter.save());
    }

    @WrapOperation(method = "handleDataMarker", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerLevelAccessor;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private void aoemod$mark(ServerLevelAccessor level, Entity entity, Operation<Void> original) {
        var piece = (WoodlandMansionPiece) (Object) this;
        if (aoemod$encounter != null && entity instanceof Mob mob) {
            // New mansions own a fixed roster; native Mage/Warrior markers must
            // not add extra residents. Other native markers (such as allays) survive.
            if (aoemod$encounter.fixedRoster() && mob instanceof net.minecraft.world.entity.monster.AbstractIllager) return;
            Integer index = aoemod$encounter.indices().get(mob.blockPosition().subtract(piece.templatePosition()).asLong());
            if (index != null) {
                MansionDefenderSpawns.get(level.getLevel()).plan(level.getLevel(), new MansionDefenderSpawns.Plan(aoemod$encounter.id(),
                        piece.templatePosition().offset(aoemod$encounter.anchorOffset()), aoemod$encounter.expected(), index,
                        net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()).toString(), mob.blockPosition(), piece.getBoundingBox()));
                if (!AuthoredEncounterProgress.get(level.getLevel()).canPlaceDefender(aoemod$encounter.id(), index)) return;
                mob.setUUID(MansionDefenderSpawns.entityId(aoemod$encounter.id(), index));
                AuthoredEncounterProgress.markDefender(mob, aoemod$encounter.id(), piece.templatePosition().offset(aoemod$encounter.anchorOffset()),
                        aoemod$encounter.expected(), "mansion", index);
                StructureSpawnedMobData.mark(mob, mob.blockPosition());
            }
        }
        original.call(level, entity);
    }
}
