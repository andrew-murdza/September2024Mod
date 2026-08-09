package net.amurdza.examplemod.item;

import net.amurdza.examplemod.entity.render.item.MaterialShieldRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class MaterialShieldItem extends ShieldItem {
    private static final ResourceLocation BLOCKING = new ResourceLocation("blocking");

    private final ResourceLocation texture;
    private final Supplier<Ingredient> repairIngredient;
    private final float modelScale;

    public MaterialShieldItem(
            Properties properties,
            ResourceLocation texture,
            Supplier<Ingredient> repairIngredient,
            float modelScale
    ) {
        super(properties);
        this.texture = texture;
        this.repairIngredient = repairIngredient;
        this.modelScale = modelScale;
    }

    public ResourceLocation texture() {
        return this.texture;
    }

    public float modelScale() {
        return this.modelScale;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        // ShieldItem's vanilla fallback accepts planks. Material shields must use
        // their own tier material instead (the wooden shield explicitly uses planks).
        return this.repairIngredient.get().test(repairCandidate);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        ItemProperties.register(
                this,
                BLOCKING,
                (stack, level, entity, seed) -> entity != null
                        && entity.isUsingItem()
                        && entity.getUseItem() == stack
                        ? 1.0F
                        : 0.0F
        );
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new MaterialShieldRenderer();
                }
                return this.renderer;
            }
        });
    }
}
