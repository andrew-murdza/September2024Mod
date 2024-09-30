package net.amurdza.examplemod.mixins.othermods.iceandfire;

import com.github.alexthe666.iceandfire.entity.EntitySiren;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(EntitySiren.class)
public class PassiveSirens {
    @Redirect(method = "triggerOtherSirens",at=@At(value = "INVOKE",target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private List<Entity> hi(Level instance, Entity entity, AABB aabb){
        return List.of();
    }
}
