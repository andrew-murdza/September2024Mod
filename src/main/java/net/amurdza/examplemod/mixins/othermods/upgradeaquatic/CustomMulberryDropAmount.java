package net.amurdza.examplemod.mixins.othermods.upgradeaquatic;

import com.teamabnormals.upgrade_aquatic.common.block.MulberryVineBlock;
import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MulberryVineBlock.class)
public class CustomMulberryDropAmount {
    @Redirect(method = "use",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getValue(Lnet/minecraft/world/level/block/state/properties/Property;)Ljava/lang/Comparable;",ordinal = 3))
    public Comparable hi(BlockState instance, Property property, BlockState state, Level level, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit){
        return Helper.isSpecialBiome(level,pos)?false:instance.getValue(property);
    }
    @ModifyConstant(method = "use",constant = @Constant(intValue = 1, ordinal = 2))
    public int hi(int constant, BlockState state, Level level, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit){
        return Helper.isSpecialBiome(level,pos)? Config.MULBERRY_HARVEST_AMOUNT_IN_SPECIAL_BIOME:1;
    }
}
