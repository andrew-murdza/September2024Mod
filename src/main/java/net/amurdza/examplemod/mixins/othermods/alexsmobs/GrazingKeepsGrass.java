package net.amurdza.examplemod.mixins.othermods.alexsmobs;

import com.github.alexthe666.alexsmobs.entity.EntityBison;
import com.github.alexthe666.alexsmobs.entity.EntityKangaroo;
import com.github.alexthe666.alexsmobs.entity.EntityRhinoceros;
import com.github.alexthe666.alexsmobs.entity.EntityTusklin;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** These grazers write dirt directly, bypassing Forge's mob-griefing event. */
@Mixin({EntityBison.class, EntityKangaroo.class, EntityRhinoceros.class, EntityTusklin.class})
public abstract class GrazingKeepsGrass {
    @Redirect(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"),
            require = 1)
    private boolean aoemod$keepGrazedGrass(Level level, BlockPos pos, BlockState replacement, int flags) {
        // Keep the surrounding feeding counter, healing, particles and animation intact.
        if (replacement.is(Blocks.DIRT) && level.getBlockState(pos).is(Blocks.GRASS_BLOCK)) return false;
        return level.setBlock(pos, replacement, flags);
    }
}
