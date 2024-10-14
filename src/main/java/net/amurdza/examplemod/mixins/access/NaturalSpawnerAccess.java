package net.amurdza.examplemod.mixins.access;

import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NaturalSpawner.class)
public interface NaturalSpawnerAccess {

  @Accessor("MAGIC_NUMBER")
  static int getMagicNumber() {
    throw new Error("Mixin did not apply!");
  }
}
