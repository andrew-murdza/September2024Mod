package net.amurdza.examplemod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.ForgeRegistries;

public class BlueBerryBushBlock extends SweetBerryBushBlock {
    private static final ResourceLocation FRUITS_DELIGHT_BLUEBERRY = new ResourceLocation("fruitsdelight", "blueberry");

    public BlueBerryBushBlock(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(blueberryItem());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        int age = state.getValue(AGE);
        boolean usingBonemeal = player.getItemInHand(hand).is(Items.BONE_MEAL);

        if (age < 3 && usingBonemeal) {
            return InteractionResult.PASS;
        }

        if (age > 1) {
            int amount = 1 + level.random.nextInt(2);

            if (age == 3) {
                amount++;
            }

            Block.popResource(level, pos, new ItemStack(blueberryItem(), amount));
            level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.8F + level.random.nextFloat() * 0.4F);
            BlockState pickedState = state.setValue(AGE, 1);
            level.setBlock(pos, pickedState, 2);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, pickedState));
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        return super.use(state, level, pos, player, hand, hit);
    }

    private static Item blueberryItem() {
        Item item = ForgeRegistries.ITEMS.getValue(FRUITS_DELIGHT_BLUEBERRY);
        return item == null ? Items.SWEET_BERRIES : item;
    }
}
