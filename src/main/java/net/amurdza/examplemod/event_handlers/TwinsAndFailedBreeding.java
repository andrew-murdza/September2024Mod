package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.config.MobConfig;
import net.amurdza.examplemod.util.Helper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TwinsAndFailedBreeding {
    private static final String PARENT_VARIANT_TAG = "aoe.parentVariant";

    @SubscribeEvent
    public static void twinsOrInfertile(BabyEntitySpawnEvent event) {
        if (!(event.getParentA() instanceof AgeableMob parentA)) {
            return;
        }

        if (!(event.getParentB() instanceof AgeableMob parentB)) {
            return;
        }

        if (!(parentA.level() instanceof ServerLevel world)) {
            return;
        }

        if (event.getChild() == null) {
            return;
        }

        applyParentBasedVariant(event.getChild(), parentA, parentB);

        float childMultiplier = MobConfig.mobTwinsChance(event.getChild(), parentA);

        if (childMultiplier < 0.0F) {
            return;
        }

        int children = Helper.computeIncrements(parentA.getRandom(), childMultiplier);

        if (children <= 0) {
            event.setCanceled(true);
            return;
        }

        if (parentA instanceof Turtle || parentA instanceof Frog) {
            return;
        }

        // Vanilla still adds the first child after this event, so only add extras here.
        for (int i = 1; i < children; i++) {
            AgeableMob extraChild = parentA.getBreedOffspring(world, parentB);

            if (extraChild == null) {
                continue;
            }
            applyParentBasedVariant(extraChild, parentA, parentB);
            extraChild.moveTo(parentA.getX(), parentA.getY(), parentA.getZ(), 0.0F, 0.0F);
            extraChild.setBaby(true);
            world.addFreshEntity(extraChild);
        }
    }

    private static void applyParentBasedVariant(AgeableMob child, AgeableMob parentA, AgeableMob parentB) {
        if (parentA.getClass() != parentB.getClass() || child.getClass() != parentA.getClass()) {
            return;
        }

        Object variantA = getVariant(parentA);
        Object variantB = getVariant(parentB);
        if (variantA == null || variantB == null || variantA.getClass() != variantB.getClass()) {
            return;
        }

        int roll = parentA.getRandom().nextInt(100);
        Object chosenVariant;
        if (roll < 45) {
            chosenVariant = variantA;
        } else if (roll < 90) {
            chosenVariant = variantB;
        } else {
            chosenVariant = mutationVariant(child, parentA, variantA, variantB);
        }

        if (setVariant(child, chosenVariant)) {
            child.addTag(PARENT_VARIANT_TAG);
        }
    }

    private static Object mutationVariant(AgeableMob child, AgeableMob randomSource, Object variantA, Object variantB) {
        Object childVariant = getVariant(child);
        if (childVariant != null && !Objects.equals(childVariant, variantA) && !Objects.equals(childVariant, variantB)) {
            return childVariant;
        }

        if (variantA instanceof Enum<?>) {
            Object[] mutations = Arrays.stream(variantA.getClass().getEnumConstants())
                    .filter(variant -> !Objects.equals(variant, variantA) && !Objects.equals(variant, variantB))
                    .toArray();
            if (mutations.length > 0) {
                return mutations[randomSource.getRandom().nextInt(mutations.length)];
            }
        }

        return childVariant != null ? childVariant : variantA;
    }

    private static Object getVariant(AgeableMob mob) {
        Method getter = findMethod(mob.getClass(), "getVariant", 0);
        if (getter == null) {
            return null;
        }

        try {
            getter.setAccessible(true);
            return getter.invoke(mob);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static boolean setVariant(AgeableMob mob, Object variant) {
        if (variant == null) {
            return false;
        }

        Method setter = findVariantSetter(mob.getClass(), variant.getClass());
        if (setter == null) {
            return false;
        }

        try {
            setter.setAccessible(true);
            setter.invoke(mob, variant);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static Method findVariantSetter(Class<?> type, Class<?> variantType) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals("setVariant")
                        && method.getParameterCount() == 1
                        && method.getParameterTypes()[0].isAssignableFrom(variantType)) {
                    return method;
                }
            }
        }

        return null;
    }

    private static Method findMethod(Class<?> type, String name, int parameterCount) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == parameterCount) {
                    return method;
                }
            }
        }

        return null;
    }
}
