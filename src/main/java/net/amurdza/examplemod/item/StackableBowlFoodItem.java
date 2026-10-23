package net.amurdza.examplemod.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowlFoodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Preserves remaining servings when the mod's soup stack-size rule is applied. */
public class StackableBowlFoodItem extends BowlFoodItem {
    public StackableBowlFoodItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        // Farmer's Delight already returns remaining soup and gives the bowl from its superclass mixin.
        // Only handle the vanilla path that returns a bowl while servings still remain.
        if (!result.is(Items.BOWL) || stack.isEmpty()
                || entity instanceof Player player && player.getAbilities().instabuild) {
            return result;
        }

        // BowlFoodItem consumes one serving, but returns a bowl instead of the remaining stack.
        if (!level.isClientSide) {
            if (entity instanceof Player player) {
                if (!player.getInventory().add(result)) {
                    player.drop(result, false);
                }
            } else {
                entity.spawnAtLocation(result);
            }
        }
        return stack;
    }
}
