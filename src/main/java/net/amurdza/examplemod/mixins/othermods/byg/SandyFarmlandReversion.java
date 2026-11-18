package net.amurdza.examplemod.mixins.othermods.byg;

import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;
import net.potionstudios.biomeswevegone.world.level.block.custom.BWGFarmLandBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BWGFarmLandBlock.class)
public final class SandyFarmlandReversion {
    @Redirect(method = "turnToDirtBlock", remap = false,
            at = @At(value = "INVOKE", target = "Ljava/util/function/Supplier;get()Ljava/lang/Object;"))
    private Object aoemod$revertToSand(Supplier<?> original) {
        return aoemod$soil(original);
    }

    @Redirect(method = "getStateForPlacement",
            at = @At(value = "INVOKE", target = "Ljava/util/function/Supplier;get()Ljava/lang/Object;", remap = false))
    private Object aoemod$obstructedPlacement(Supplier<?> original) {
        return aoemod$soil(original);
    }

    private Object aoemod$soil(Supplier<?> original) {
        ResourceLocation id = ForgeRegistries.BLOCKS.getKey((BWGFarmLandBlock) (Object) this);
        return id != null && id.toString().equals("biomeswevegone:sandy_farmland") ? Blocks.SAND : original.get();
    }
}
