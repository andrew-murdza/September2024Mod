package net.amurdza.examplemod.future;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.Optional;

/** Bridges optional backport interfaces without forcing their classes to load on a server. */
public final class FutureMobCompatibility {
    private static final ClassValue<Optional<Method>> AGE_LOCK = new ClassValue<>() {
        @Override
        protected Optional<Method> computeValue(Class<?> type) {
            try {
                return Optional.of(type.getMethod("tiny_takeover_backport$isAgeLocked"));
            } catch (NoSuchMethodException exception) {
                return Optional.empty();
            }
        }
    };

    private FutureMobCompatibility() {
    }

    public static boolean isAgeLocked(Entity entity) {
        if (!ModList.get().isLoaded("tiny_takeover_backport")) return false;
        Optional<Method> method = AGE_LOCK.get(entity.getClass());
        if (method.isEmpty()) return false;
        try {
            return (boolean) method.get().invoke(entity);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Cannot read Tiny Takeover age lock", exception);
        }
    }

    public static boolean isArmadillo(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).equals(new ResourceLocation("minecraft", "armadillo"));
    }
}
