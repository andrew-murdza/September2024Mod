package net.amurdza.examplemod.mixins.block_drops;

import net.amurdza.examplemod.event_handlers.CustomBlockDrops;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Native destruction and loot queries must use the same seed pool as player harvesting. */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class RegionalGrassLoot {
    @Inject(method = "getDrops", at = @At("HEAD"), cancellable = true)
    private void aoemod$regionalSeeds(LootParams.Builder builder, CallbackInfoReturnable<List<ItemStack>> cir) {
        BlockState state = (BlockState) (Object) this;
        if (!state.is(Blocks.GRASS) && !state.is(Blocks.TALL_GRASS) && !state.is(Blocks.FERN) && !state.is(Blocks.LARGE_FERN)
                && !state.is(net.amurdza.examplemod.registry.ModBlocks.DESERT_GRASS.get())
                && !state.is(net.amurdza.examplemod.registry.ModBlocks.DESERT_TALL_GRASS.get())) return;
        Vec3 origin = builder.getOptionalParameter(LootContextParams.ORIGIN);
        if (origin == null) return;
        ItemStack tool = builder.getOptionalParameter(LootContextParams.TOOL);
        var drops = CustomBlockDrops.configuredDrops(builder.getLevel(), BlockPos.containing(origin), state,
                tool == null ? ItemStack.EMPTY : tool, builder.getLevel().random);
        if (drops != null) cir.setReturnValue(drops);
    }
}
