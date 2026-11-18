package net.amurdza.examplemod.mixins.othermods.backport;

import com.blackgear.platform.core.helper.BlockRegistry;
import com.blackgear.vanillabackport.client.registries.ModSoundTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Function;
import java.util.function.Supplier;

/** Nonflammable plant properties remove lava ignition while retaining the shrub's own sound. */
@Mixin(targets = "com.blackgear.vanillabackport.common.registries.blocks.ModBlocks", remap = false)
public abstract class RedShrubProperties {
    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", target =
            "Lcom/blackgear/platform/core/helper/BlockRegistry;register(Ljava/lang/String;Ljava/util/function/Function;Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;)Ljava/util/function/Supplier;"))
    private static Supplier<Block> aoemod$useRootProperties(BlockRegistry registry, String name,
            Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
        return registry.register(name, factory, name.equals("red_shrub")
                ? BlockBehaviour.Properties.copy(Blocks.CRIMSON_ROOTS).sound(ModSoundTypes.RED_SHRUB) : properties);
    }
}
