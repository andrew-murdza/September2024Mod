package net.amurdza.examplemod.mixins.othermods.alexscaves;

import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import com.github.alexmodguy.alexscaves.server.entity.living.SubterranodonEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry.CAVE_CREATURE;

@Mixin(ACEntityRegistry.class)
public class CaveCreatureMobCap {
    @Redirect(method = "lambda$static$12",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType$Builder;of(Lnet/minecraft/world/entity/EntityType$EntityFactory;Lnet/minecraft/world/entity/MobCategory;)Lnet/minecraft/world/entity/EntityType$Builder;"))
    private static <T extends Entity> EntityType.Builder<T> hi(EntityType.EntityFactory<T> pFactory, MobCategory pCategory){
        return EntityType.Builder.of(pFactory, pCategory==CAVE_CREATURE?MobCategory.CREATURE:pCategory);
    }
    @Redirect(method = "lambda$static$13",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType$Builder;of(Lnet/minecraft/world/entity/EntityType$EntityFactory;Lnet/minecraft/world/entity/MobCategory;)Lnet/minecraft/world/entity/EntityType$Builder;"))
    private static <T extends Entity> EntityType.Builder<T> hi1(EntityType.EntityFactory<T> pFactory, MobCategory pCategory){
        return EntityType.Builder.of(pFactory, pCategory==CAVE_CREATURE?MobCategory.CREATURE:pCategory);
    }
    @Redirect(method = "lambda$static$14",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType$Builder;of(Lnet/minecraft/world/entity/EntityType$EntityFactory;Lnet/minecraft/world/entity/MobCategory;)Lnet/minecraft/world/entity/EntityType$Builder;"))
    private static <T extends Entity> EntityType.Builder<T> hi2(EntityType.EntityFactory<T> pFactory, MobCategory pCategory){
        return EntityType.Builder.of(pFactory, pCategory==CAVE_CREATURE?MobCategory.CREATURE:pCategory);
    }
    @Redirect(method = "lambda$static$16",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType$Builder;of(Lnet/minecraft/world/entity/EntityType$EntityFactory;Lnet/minecraft/world/entity/MobCategory;)Lnet/minecraft/world/entity/EntityType$Builder;"))
    private static <T extends Entity> EntityType.Builder<T> hi3(EntityType.EntityFactory<T> pFactory, MobCategory pCategory){
        return EntityType.Builder.of(pFactory, pCategory==CAVE_CREATURE?MobCategory.CREATURE:pCategory);
    }
    @Redirect(method = "lambda$static$17",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType$Builder;of(Lnet/minecraft/world/entity/EntityType$EntityFactory;Lnet/minecraft/world/entity/MobCategory;)Lnet/minecraft/world/entity/EntityType$Builder;"))
    private static <T extends Entity> EntityType.Builder<T> hi4(EntityType.EntityFactory<T> pFactory, MobCategory pCategory){
        return EntityType.Builder.of(pFactory, pCategory==CAVE_CREATURE?MobCategory.CREATURE:pCategory);
    }
    @Redirect(method = "lambda$static$20",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType$Builder;of(Lnet/minecraft/world/entity/EntityType$EntityFactory;Lnet/minecraft/world/entity/MobCategory;)Lnet/minecraft/world/entity/EntityType$Builder;"))
    private static <T extends Entity> EntityType.Builder<T> hi5(EntityType.EntityFactory<T> pFactory, MobCategory pCategory){
        return EntityType.Builder.of(pFactory, pCategory==CAVE_CREATURE?MobCategory.CREATURE:pCategory);
    }
    @Redirect(method = "lambda$static$33",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType$Builder;of(Lnet/minecraft/world/entity/EntityType$EntityFactory;Lnet/minecraft/world/entity/MobCategory;)Lnet/minecraft/world/entity/EntityType$Builder;"))
    private static <T extends Entity> EntityType.Builder<T> hi6(EntityType.EntityFactory<T> pFactory, MobCategory pCategory){
        return EntityType.Builder.of(pFactory, pCategory==CAVE_CREATURE?MobCategory.CREATURE:pCategory);
    }
    @Redirect(method = "lambda$static$35",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType$Builder;of(Lnet/minecraft/world/entity/EntityType$EntityFactory;Lnet/minecraft/world/entity/MobCategory;)Lnet/minecraft/world/entity/EntityType$Builder;"))
    private static <T extends Entity> EntityType.Builder<T> hi7(EntityType.EntityFactory<T> pFactory, MobCategory pCategory){
        return EntityType.Builder.of(pFactory, pCategory==CAVE_CREATURE?MobCategory.CREATURE:pCategory);
    }
}
