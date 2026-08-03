package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.config.BlockBehaviorConfig;
import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.util.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import static net.amurdza.examplemod.AOEMod.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class BerryDrops {
    @SubscribeEvent
    public static void berriesFromUse(PlayerInteractEvent.RightClickBlock event){
        Level level=event.getLevel();
        BlockPos pos=event.getPos();
        BlockState state=level.getBlockState(pos);
        if(state.is(Blocks.SWEET_BERRY_BUSH)||state.is(ModBlocks.JUNGLE_SWEET_BERRY_BUSH.get())||state.is(ModBlocks.BLUE_BERRY_BUSH.get())){
            int age=state.getValue(BlockStateProperties.AGE_3);
            Float value = null;
            if(age==2&&!event.getItemStack().is(Items.BONE_MEAL)){
                value = Helper.getBiomeValue(level, pos, BlockBehaviorConfig.BIOME_TO_SWEET_BERRIES_PARTIALLY_GROWN_AMOUNTS);
            }
            if(age==3){
                value = Helper.getBiomeValue(level, pos, BlockBehaviorConfig.BIOME_TO_MATURE_SWEET_BERRY_AMOUNTS);
            }
            float count = value != null ? value : 0;
            int amount = Helper.computeIncrements(level.random,count);
            if(amount>0){
                Item berry;
                if(state.is(Blocks.SWEET_BERRY_BUSH)||state.is(ModBlocks.JUNGLE_SWEET_BERRY_BUSH.get())){
                    berry = Items.SWEET_BERRIES;
                }
                else {
                    ResourceLocation FRUITS_DELIGHT_BLUEBERRY = new ResourceLocation("fruitsdelight", "blueberry");
                    berry = ForgeRegistries.ITEMS.getValue(FRUITS_DELIGHT_BLUEBERRY);
                }
                Block.popResource(level, pos, new ItemStack(berry,amount));
                level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.8F + level.random.nextFloat() * 0.4F);
                BlockState pickedState = state.setValue(BlockStateProperties.AGE_3, 1);
                level.setBlock(pos, pickedState, 2);
                level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(event.getEntity(), pickedState));
                event.setCanceled(true);
            }
        }
    }
}
