package net.amurdza.examplemod.mixins.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/** Retire the old cyan-to-light-blue remap now that both saved names are real plants again. */
@Mixin(value = ForgeRegistry.class, remap = false)
public abstract class RestoredCharniaAliases {
    @Unique private static final ResourceLocation AOE_CYAN = new ResourceLocation("betterend", "charnia_cyan");
    @Unique private static final ResourceLocation AOE_LIGHT_BLUE = new ResourceLocation("betterend", "charnia_light_blue");

    @Shadow @Final private ResourceLocation name;
    @Shadow @Final private Map<ResourceLocation, ResourceLocation> aliases;

    @Inject(method = "addAlias", at = @At("HEAD"), cancellable = true)
    private void aoe$ignoreRetiredCharniaRemap(ResourceLocation source, ResourceLocation destination, CallbackInfo ci) {
        // Saved aliases are imported before Forge checks which current registrations are missing.
        // Keeping this alias would make that check mistake light blue for the restored cyan plant.
        if (aoe$isBlockOrItemRegistry() && AOE_CYAN.equals(source) && AOE_LIGHT_BLUE.equals(destination)) {
            aliases.remove(AOE_CYAN, AOE_LIGHT_BLUE);
            ci.cancel();
        }
    }

    @Inject(method = "sync", at = @At(value = "INVOKE",
            target = "Lcom/google/common/collect/BiMap;clear()V", ordinal = 0))
    private void aoe$removeStaleCharniaAliasBeforeRebuildingIds(ResourceLocation registryName,
                                                              ForgeRegistry<?> source, CallbackInfo ci) {
        // sync merges aliases directly, bypassing addAlias. Remove the obsolete pair after that
        // merge and before the first ID/name map clear, covering either source or destination.
        // Otherwise adding cyan can resolve the alias to light blue and steal its numeric ID;
        // the failed sync then leaves Forge's global block-state palette empty on disconnect.
        if (aoe$isBlockOrItemRegistry()) aliases.remove(AOE_CYAN, AOE_LIGHT_BLUE);
    }

    @Unique
    private boolean aoe$isBlockOrItemRegistry() {
        return name.getNamespace().equals("minecraft")
                && (name.getPath().equals("block") || name.getPath().equals("item"));
    }
}
