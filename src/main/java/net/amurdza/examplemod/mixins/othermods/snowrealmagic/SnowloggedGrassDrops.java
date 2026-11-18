package net.amurdza.examplemod.mixins.othermods.snowrealmagic;

import net.amurdza.examplemod.event_handlers.CustomBlockDrops;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import snownee.snow.block.EntitySnowLayerBlock;

@Mixin(EntitySnowLayerBlock.class)
public class SnowloggedGrassDrops {
    @Redirect(method = "attack", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/block/Block;dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V"))
    private void aoemod$useForestSeedPool(BlockState plant, Level level, BlockPos pos,
                                         BlockEntity blockEntity, Entity breaker, ItemStack originalTool) {
        if (level instanceof ServerLevel server && GameplayDimensions.is(level, GameplayDimensions.SNOWY)
                && (plant.is(Blocks.GRASS) || plant.is(Blocks.FERN)
                || plant.is(Blocks.TALL_GRASS) || plant.is(Blocks.LARGE_FERN))) {
            if (breaker instanceof Player player && player.isCreative()) return;
            // The native snow attack skips BreakEvent and passes an empty stack.
            // Use the same biome-aware roll as normal grass, including shears and
            // Fortune, while leaving removal/snow retention to Snow! Real Magic.
            ItemStack tool = breaker instanceof Player player ? player.getMainHandItem() : originalTool;
            var drops = CustomBlockDrops.configuredDrops(server, pos, plant, tool, server.random);
            if (drops != null) {
                for (ItemStack drop : drops) Block.popResource(level, pos, drop);
            } else {
                Block.dropResources(plant, level, pos, blockEntity, breaker, tool);
            }
            return;
        }
        Block.dropResources(plant, level, pos, blockEntity, breaker, originalTool);
    }
}
