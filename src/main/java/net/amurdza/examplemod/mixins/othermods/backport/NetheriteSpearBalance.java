package net.amurdza.examplemod.mixins.othermods.backport;

import com.blackgear.vanillabackport.common.level.items.spear.SpearItem;
import net.amurdza.examplemod.item.ModToolTiers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** The full spear attack mechanics retain the pack's copper < netherite < iron progression. */
@Pseudo
@Mixin(targets = "com.blackgear.vanillabackport.common.registries.items.ModItems", remap = false)
public abstract class NetheriteSpearBalance {
    @Redirect(method = "*", at = @At(value = "NEW",
            target = "com/blackgear/vanillabackport/common/level/items/spear/SpearItem"))
    private static SpearItem aoemod$balanceNetheriteSpear(Tier tier, float attackDuration,
            float damageMultiplier, float delay, float dismountTime, float dismountThreshold,
            float knockbackTime, float knockbackThreshold, float damageTime, float damageThreshold,
            Item.Properties properties) {
        if (tier == Tiers.NETHERITE) {
            return new SpearItem(ModToolTiers.REBALANCED_NETHERITE,
                    (0.85F + 0.95F) / 2F, (0.82F + 0.95F) / 2F, (0.65F + 0.6F) / 2F,
                    (4F + 2.5F) / 2F, (9F + 8F) / 2F, (8.25F + 6.75F) / 2F,
                    5.1F, (12.5F + 11.25F) / 2F, 4.6F, properties);
        }
        return new SpearItem(tier, attackDuration, damageMultiplier, delay, dismountTime,
                dismountThreshold, knockbackTime, knockbackThreshold, damageTime, damageThreshold, properties);
    }
}
