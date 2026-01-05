package net.amurdza.examplemod.mixins.block_behavior;

import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LeavesBlock.class)
public abstract class LeavesBlocksShedAndDecayFartherAway {
    @Shadow protected abstract boolean decaying(BlockState state);

    // ----- CONSTANTS: make these soft so failures don't crash -----

    @ModifyConstant(
            method = "<init>",
            constant = @Constant(intValue = 7),
            require = 0
    )
    private int aoe$maxDistance_init(int original) {
        return Config.MAX_LEAVES_DISTANCE;
    }

    @ModifyConstant(
            method = "isRandomlyTicking",
            constant = @Constant(intValue = 7),
            require = 0
    )
    private int aoe$maxDistance_isRandomlyTicking(int original) {
        return Config.MAX_LEAVES_DISTANCE;
    }

    @ModifyConstant(
            method = "decaying",
            constant = @Constant(intValue = 7),
            require = 0
    )
    private int aoe$maxDistance_decaying(int original) {
        return Config.MAX_LEAVES_DISTANCE;
    }

    // ----- NO OVERWRITE: just force random ticking -----

    @Inject(method = "isRandomlyTicking", at = @At("HEAD"), cancellable = true, require = 0)
    private void aoe$alwaysRandomTick(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }

    // ----- NO REDIRECT: implement shedding at randomTick head -----

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true, require = 0)
    private void aoe$shedLeaves(BlockState state, ServerLevel level, BlockPos pos, RandomSource random,
                                CallbackInfo ci) {

        // If decaying, let vanilla handle it normally.
        if (this.decaying(state)) return;

        // Otherwise, apply your "shed" chance.
        if (random.nextInt(Config.LEAVES_SHED_CHANCE) != 0) return;

        if (!level.isAreaLoaded(pos, 4)) return;

        // Your condition (example: tropical + 4 air blocks below)
        if (level.getBiome(pos).is(ModTags.Biomes.tropicalBiomes)
                && level.isEmptyBlock(pos.below())
                && level.isEmptyBlock(pos.below(2))
                && level.isEmptyBlock(pos.below(3))
                && level.isEmptyBlock(pos.below(4))) {

            // Spawn drops. (LeavesBlock has a static getDrops method; if you call it directly, qualify it.)
            LeavesBlock.getDrops(state, level, pos, null)
                    .forEach(stack -> aoe$popResourceBelow(level, pos, stack));

            state.spawnAfterBreak(level, pos, ItemStack.EMPTY, true);
        }

        // Cancel vanilla randomTick so you don't also run its normal behavior for non-decaying leaves.
        ci.cancel();
    }

    @Unique
    private static void aoe$popResourceBelow(Level level, BlockPos pos, ItemStack stack) {
        double halfH = (double) EntityType.ITEM.getHeight() / 2.0D;
        double x = pos.getX() + 0.5D + Mth.nextDouble(level.random, -0.25D, 0.25D);
        double y = pos.getY() - 1.5D + Mth.nextDouble(level.random, -0.25D, 0.25D) - halfH;
        double z = pos.getZ() + 0.5D + Mth.nextDouble(level.random, -0.25D, 0.25D);

        if (!level.isClientSide
                && !stack.isEmpty()
                && level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)
                && !level.restoringBlockSnapshots) {

            ItemEntity e = new ItemEntity(level, x, y, z, stack);
            e.setDefaultPickUpDelay();
            level.addFreshEntity(e);
        }
    }
}
