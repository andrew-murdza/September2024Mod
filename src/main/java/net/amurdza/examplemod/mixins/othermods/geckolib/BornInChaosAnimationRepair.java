package net.amurdza.examplemod.mixins.othermods.geckolib;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import software.bernie.geckolib.loading.FileLoader;

/** Repair five invalid Molang components in the pinned Born in Chaos animations. */
@Mixin(value = FileLoader.class, remap = false)
public abstract class BornInChaosAnimationRepair {
    @Inject(method = "loadFile", at = @At("RETURN"), require = 1)
    private static void aoe$repairBareMinus(ResourceLocation resource, ResourceManager manager,
                                          CallbackInfoReturnable<JsonObject> cir) {
        if (!resource.getNamespace().equals("born_in_chaos_v1")) {
            return;
        }
        switch (resource.getPath()) {
            case "animations/babyskeleton.animation.json",
                 "animations/babyskeletondeamon.animation.json",
                 "animations/boneimp.animation.json" ->
                    aoe$repairComponent(cir.getReturnValue(), "idle", "tail", "4.0", 1);
            case "animations/scarletpersecutor.animation.json" -> {
                aoe$repairComponent(cir.getReturnValue(), "attack", "right_arm", "0.8333", 0);
                aoe$repairComponent(cir.getReturnValue(), "attack", "left_arm", "0.8333", 0);
            }
            default -> {
            }
        }
    }

    @Unique
    private static void aoe$repairComponent(JsonObject root, String animation, String bone,
                                            String time, int component) {
        JsonElement value = root;
        for (String part : new String[]{"animations", animation, "bones", bone, "rotation", time, "vector"}) {
            if (value == null || !value.isJsonObject()) {
                return;
            }
            value = value.getAsJsonObject().get(part);
        }
        if (value == null || !value.isJsonArray()) {
            return;
        }
        JsonArray vector = value.getAsJsonArray();
        if (vector.size() <= component) {
            return;
        }
        JsonElement current = vector.get(component);
        // Only replace the malformed token; a corrected upstream/resource-pack value wins.
        if (current.isJsonPrimitive() && current.getAsJsonPrimitive().isString()
                && current.getAsString().trim().equals("-")) {
            vector.set(component, new com.google.gson.JsonPrimitive(0));
        }
    }
}
