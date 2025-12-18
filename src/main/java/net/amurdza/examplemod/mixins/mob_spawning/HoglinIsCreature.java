package net.amurdza.examplemod.mixins.mob_spawning;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(EntityType.class)
public class HoglinIsCreature {
    @ModifyArg(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/EntityType$Builder;of(Lnet/minecraft/world/entity/EntityType$EntityFactory;Lnet/minecraft/world/entity/MobCategory;)Lnet/minecraft/world/entity/EntityType$Builder;"
            ),
            index = 1,
            slice = @Slice(
                    from = @At(value = "CONSTANT", args = "stringValue=hoglin")
            )
    )
    private static MobCategory aoemod$hoglinIsCreature(MobCategory original) {
        // Inside the "hoglin" slice, the first Builder.of(...) should be the HOGLIN one.
        return MobCategory.CREATURE;
    }
}
