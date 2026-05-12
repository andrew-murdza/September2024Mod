package net.amurdza.examplemod.mixins.othermods.biomeswevegone;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.compat.WitchHazelOnBasalt2;
import net.amurdza.examplemod.util.ModTags;
import net.amurdza.examplemod.worldgen.feature.WitchHazelTreeGrower;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.material.MapColor;
import net.potionstudios.biomeswevegone.world.level.block.wood.BWGWood;
import net.potionstudios.biomeswevegone.world.level.block.wood.BWGWoodSet;
import net.potionstudios.biomeswevegone.world.level.levelgen.feature.configured.BWGOverworldTreeConfiguredFeatures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Supplier;

@Mixin(value = BWGWood.class, remap = false)
public class WitchHazelOnBasalt {

    @Redirect(
            method = "<clinit>",
            at = @At(
                    value = "NEW",
                    target = "net/potionstudios/biomeswevegone/world/level/block/wood/BWGWoodSet",
                    ordinal = 22
            ),
            require = 1
    )
    private static BWGWoodSet aoemod$makeWitchHazelUseBasalt(
            String name,
            MapColor color,
            Supplier<AbstractTreeGrower> grower
    ) {
        return new WitchHazelOnBasalt2(name, color, WitchHazelTreeGrower::new, ModTags.Blocks.basaltStones);
    }
}