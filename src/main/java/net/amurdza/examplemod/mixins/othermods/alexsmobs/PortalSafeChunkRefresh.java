package net.amurdza.examplemod.mixins.othermods.alexsmobs;

import com.github.alexthe666.alexsmobs.client.event.ClientEvents;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Alex's lava-vision refresh assumes every vanilla render slot exists during portal travel. */
@Mixin(value = ClientEvents.class, remap = false)
public abstract class PortalSafeChunkRefresh {
    @Inject(method = "updateAllChunks", at = @At("HEAD"), cancellable = true)
    private void aoemod$refreshThroughActiveRenderer(CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        // Use the renderer's public invalidation path. Embeddium owns its own chunk
        // meshes, and Immersive Portals can leave null slots in the vanilla view area.
        // This runs only when lava vision changes, not on every rendered frame.
        if (minecraft.level != null) minecraft.levelRenderer.allChanged();
        ci.cancel();
    }
}
