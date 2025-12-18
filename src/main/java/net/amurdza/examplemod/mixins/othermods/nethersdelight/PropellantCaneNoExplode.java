package net.amurdza.examplemod.mixins.othermods.nethersdelight;

//import net.minecraft.tags.TagKey;
//import net.minecraft.world.entity.Entity;
//import net.minecraft.world.item.Item;
//import net.minecraft.world.item.ItemStack;
//import net.minecraft.world.level.Explosion;
//import net.minecraft.world.level.Level;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Redirect;
//import umpaz.nethersdelight.common.block.util.PropelplantBlock;
//
//@Mixin(PropelplantBlock.class)
//public class PropellantCaneNoExplode {
//    @Redirect(method = "entityInside",at= @At(value = "FIELD", target = "Lnet/minecraft/world/level/Level;isClientSide:Z"))
//    private boolean hi(Level instance){
//        return true;
//    }
//    @Redirect(method = "playerDestroy",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/tags/TagKey;)Z"))
//    private boolean hi(ItemStack instance, TagKey<Item> pTag){
//        return true;
//    }
//    @Redirect(method = "onBlockExploded",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)Lnet/minecraft/world/level/Explosion;"))
//    private Explosion hi(Level instance, Entity pSource, double pX, double pY, double pZ, float pRadius, boolean pFire, Level.ExplosionInteraction pExplosionInteraction){
//        return null;
//    }
//}
