package net.amurdza.examplemod.mixins.mob_spawning;

import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Arrays;

@Mixin(TropicalFish.class)
public abstract class TropicalFishPalette {
    @Unique
    private static final DyeColor[] aoemod$colors = Arrays.stream(DyeColor.values())
            .filter(color -> color != DyeColor.BLACK && color != DyeColor.GRAY).toArray(DyeColor[]::new);

    // Only the second setter is a newly rolled fish. The first restores a captured fish.
    @ModifyArg(method = "finalizeSpawn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/TropicalFish;setPackedVariant(I)V", ordinal = 1), index = 0)
    private int aoemod$rollTropicalVariant(int original) {
        TropicalFish fish = (TropicalFish) (Object) this;
        return GameplayDimensions.is(fish.level(), GameplayDimensions.TROPICAL) ? aoemod$roll(fish) : original;
    }

    @ModifyVariable(method = "setPackedVariant", at = @At("HEAD"), argsOnly = true)
    private int aoemod$restoreTropicalVariant(int packed) {
        TropicalFish fish = (TropicalFish) (Object) this;
        if (!GameplayDimensions.is(fish.level(), GameplayDimensions.TROPICAL)) return packed;
        var pattern = TropicalFish.getPattern(packed);
        var base = TropicalFish.getBaseColor(packed);
        var marking = TropicalFish.getPatternColor(packed);
        return (pattern == TropicalFish.Pattern.BETTY || pattern == TropicalFish.Pattern.CLAYFISH)
                && base != DyeColor.BLACK && base != DyeColor.GRAY && marking != DyeColor.BLACK && marking != DyeColor.GRAY
                ? packed : aoemod$roll(fish);
    }

    @Unique
    private static int aoemod$roll(TropicalFish fish) {
        var random = fish.getRandom();
        var pattern = random.nextBoolean() ? TropicalFish.Pattern.BETTY : TropicalFish.Pattern.CLAYFISH;
        return new TropicalFish.Variant(pattern, aoemod$colors[random.nextInt(aoemod$colors.length)],
                aoemod$colors[random.nextInt(aoemod$colors.length)]).getPackedId();
    }
}
