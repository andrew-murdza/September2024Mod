package net.amurdza.examplemod.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.level.block.Block;

import java.util.Map;

/**
 * A planting-only alternative to a crop's normal item.
 *
 * <p>Block items normally register themselves as the canonical item for their
 * block. These seeds must be able to place the crop without replacing the
 * carrot, potato, onion, or rice item used by pick block.</p>
 */
public class CropSeedItem extends ItemNameBlockItem {
    public CropSeedItem(Block crop, Properties properties) {
        super(crop, properties);
    }

    @Override
    public void registerBlocks(Map<Block, Item> blockToItemMap, Item item) {
        // Keep the crop's original item as its canonical pick-block item.
    }
}
