package net.amurdza.examplemod.mixins.othermods.backport;

import com.blackgear.platform.core.helper.BlockRegistry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Function;
import java.util.function.Supplier;

/** Keep the red shrub's appearance while giving it crimson-root block properties. */
@Pseudo
@Mixin(targets = "com.blackgear.vanillabackport.common.registries.blocks.ModBlocks", remap = false)
public abstract class RedShrubProperties {
    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", target =
            "Lcom/blackgear/platform/core/helper/BlockRegistry;register(Ljava/lang/String;Ljava/util/function/Function;Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;)Ljava/util/function/Supplier;"))
    private static Supplier<Block> aoe$useRootProperties(BlockRegistry registry, String name,
            Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
        return registry.register(name, factory, name.equals("red_shrub")
                ? BlockBehaviour.Properties.copy(Blocks.CRIMSON_ROOTS) : properties);
    }
}
