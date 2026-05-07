package net.amurdza.examplemod.compat;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
import net.minecraft.world.level.material.MapColor;
import net.potionstudios.biomeswevegone.world.level.block.wood.BWGWoodSet;

import java.util.function.Supplier;

public class WitchHazelOnBasalt2 extends BWGWoodSet {
    public WitchHazelOnBasalt2(String name, MapColor color, Supplier<AbstractTreeGrower> grower, TagKey<Block> ground) {
        super(name, color, grower, ground);
    }
}