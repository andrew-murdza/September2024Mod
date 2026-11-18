package net.amurdza.examplemod.util;

import dev.hybridlabs.aquatic.entity.crustacean.GiantIsopodEntity;
import dev.hybridlabs.aquatic.entity.crustacean.FlowerCrabEntity;
import dev.hybridlabs.aquatic.entity.crustacean.LightfootCrabEntity;
import dev.hybridlabs.aquatic.entity.crustacean.SpiderCrabEntity;
import dev.hybridlabs.aquatic.entity.fish.MantaRayEntity;
import dev.hybridlabs.aquatic.entity.shark.WhaleSharkEntity;
import dev.hybridlabs.hapi.entity.base.aquatic.BaseWaterAnimal;
import net.amurdza.examplemod.mixins.othermods.hybridaquatic.MaximumAquaticSize;
import net.minecraft.world.entity.Entity;

/** The largest native individual scale, without changing species, colors, or age. */
public final class AquaticMobSizes {
    public static final float TALL_CRAB_SCALE = 2.2F;
    public static final float FLOWER_CRAB_SCALE = 1.925F;
    public static final float TALL_CRAB_HEAD_HEIGHT = 17.0F / 16.0F;
    public static final float TALL_CRAB_HEIGHT = TALL_CRAB_HEAD_HEIGHT + 1.25F * TALL_CRAB_SCALE / 16.0F;
    public static final float TALL_CRAB_WIDTH = 1.65F;

    private AquaticMobSizes() { }

    public static boolean isTallCrab(Entity entity) {
        return entity instanceof FlowerCrabEntity || entity instanceof LightfootCrabEntity;
    }

    public static float tallCrabScale(Entity entity) {
        return entity instanceof FlowerCrabEntity ? FLOWER_CRAB_SCALE : TALL_CRAB_SCALE;
    }

    public static float tallCrabHeight(Entity entity) {
        // The eye stalks extend one unscaled model unit above the solid shell.
        return TALL_CRAB_HEAD_HEIGHT + 1.25F * tallCrabScale(entity) / 16.0F;
    }

    public static float tallCrabWidth(Entity entity) {
        return TALL_CRAB_WIDTH * tallCrabScale(entity) / TALL_CRAB_SCALE;
    }

    public static float tallCrabShadowRadius(Entity entity) {
        // Outermost resting leg corners, measured by generate_tall_crabs.py.
        return entity instanceof FlowerCrabEntity ? 1.3509507F : 1.3422233F;
    }

    public static boolean usesMaximumSize(BaseWaterAnimal animal) {
        return isTallCrab(animal) || GameplayDimensions.is(animal.level(), GameplayDimensions.TROPICAL);
    }

    public static int maximum(BaseWaterAnimal animal) {
        if (isTallCrab(animal)) return 5;
        // These four native limits query the current biome. Their largest native
        // values must also work in artificial tropical ponds and on the client,
        // without loading a biome during rendering or shrinking when fish move.
        if (GameplayDimensions.is(animal.level(), GameplayDimensions.TROPICAL)) {
            if (animal instanceof MantaRayEntity || animal instanceof WhaleSharkEntity) return 10;
            if (animal instanceof SpiderCrabEntity) return 8;
            if (animal instanceof GiantIsopodEntity) return 5;
        }
        return ((MaximumAquaticSize) animal).aoemod$maximumSize();
    }
}
