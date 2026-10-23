package net.amurdza.examplemod.mixins.othermods.environmental;

import com.teamabnormals.environmental.common.entity.animal.slabfish.Slabfish;
import com.teamabnormals.environmental.common.inventory.SlabfishInventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The upstream menu owns the same entity passed to its custom opening packet. */
@Mixin(value = SlabfishInventoryMenu.class, remap = false)
public interface SlabfishMenuAccess {
    @Accessor("slabfish")
    Slabfish aoe$getSlabfish();
}
