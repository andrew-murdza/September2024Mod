package net.amurdza.examplemod.mixins.worldgen;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RandomizableContainerBlockEntity.class)
public abstract class PlainsMansionLoot {
    @Shadow protected ResourceLocation lootTable;

    @Inject(method = "unpackLootTable", at = @At("HEAD"))
    private void aoemod$plainsMansionLoot(Player player, CallbackInfo ci) {
        var container = (RandomizableContainerBlockEntity) (Object) this;
        // Resolve lazily so unopened chests saved with the native table also
        // gain regional loot, without changing already-opened inventories.
        if (BuiltInLootTables.WOODLAND_MANSION.equals(lootTable)
                && GameplayDimensions.is(container.getLevel(), GameplayDimensions.PLAINS)) {
            lootTable = AOEMod.makeID("chests/illager_mansion");
        }
    }
}
