//package net.amurdza.examplemod.mixins.monoxide;
//
//import it.unimi.dsi.fastutil.objects.Object2IntMap;
//import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
//import net.amurdza.examplemod.Config;
//import net.amurdza.examplemod.mixins.access.ChunkMapAccess;
//import net.amurdza.examplemod.mixins.access.LocalMobCapCalculatorAccess;
//import net.amurdza.examplemod.mixins.access.NaturalSpawnerAccess;
//import net.minecraft.server.level.ServerLevel;
//import net.minecraft.world.entity.MobCategory;
//import net.minecraft.world.entity.player.Player;
//import net.minecraft.world.inventory.AnvilMenu;
//import net.minecraft.world.level.ChunkPos;
//import net.minecraft.world.level.LocalMobCapCalculator;
//import net.minecraft.world.level.NaturalSpawner;
//import org.spongepowered.asm.mixin.Final;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Shadow;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//
//@Mixin(NaturalSpawner.SpawnState.class)
//public class BloodMoonMonsterCapIncrease {
//  @Shadow
//  @Final
//  private Object2IntOpenHashMap<MobCategory> mobCategoryCounts;
//  @Shadow
//  @Final
//  private int spawnableChunkCount;
//  @Shadow
//  @Final
//  private LocalMobCapCalculator localMobCapCalculator;
//
//  @Inject(method = "canSpawnForCategory", at = @At("HEAD"), cancellable = true)
//  private void modifySpawnCapByCategory(final MobCategory category, final ChunkPos chunkPos, final CallbackInfoReturnable<Boolean> cir) {
//    final ServerLevel level = ((ChunkMapAccess)((LocalMobCapCalculatorAccess)this.localMobCapCalculator).getChunkMap()).getLevel();
//    final boolean isBloodMoon = level.getGameTime() / 24_000 % Config.BLOOD_MOON_FREQUENCY == Config.BLOOD_MOON_FREQUENCY - 1;
//    final float multiplier = isBloodMoon ? Config.BLOOD_MOON_SPAWN_CAP_MULTIPLIER : 1.0f;
//
//    final int i = (int)(category.getMaxInstancesPerChunk() * this.spawnableChunkCount * multiplier / NaturalSpawnerAccess.getMagicNumber());
//
//    // Global Calculation
//    if (this.mobCategoryCounts.getInt(category) >= i) {
//      cir.setReturnValue(false);
//    } else {
//      cir.setReturnValue(this.localMobCapCalculator.canSpawn(category, chunkPos));
//    }
//  }
//}
