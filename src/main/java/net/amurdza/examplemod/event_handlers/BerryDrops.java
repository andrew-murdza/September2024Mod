package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.config.BlockBehaviorConfig;
import net.amurdza.examplemod.util.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static net.amurdza.examplemod.AOEMod.MOD_ID;

@Mod.EventBusSubscriber(modid = MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class BerryDrops {
    @SubscribeEvent
    public static void berriesFromUse(PlayerInteractEvent.RightClickBlock event){
        Level level=event.getLevel();
        BlockPos pos=event.getPos();
        BlockState state=level.getBlockState(pos);
        if(state.is(Blocks.SWEET_BERRY_BUSH)){
            int age=state.getValue(BlockStateProperties.AGE_3);
            int amount=0;
            if(age==2){
                Float value = Helper.getBiomeValue(level, pos, BlockBehaviorConfig.BIOME_TO_SWEET_BERRIES_PARTIALLY_GROWN_AMOUNTS);
                float count = value != null ? value : 0;
                amount = Helper.computeIncrements(level.random,count);
            }
            if(age==3){
                Float value = Helper.getBiomeValue(level, pos, BlockBehaviorConfig.BIOME_TO_MATURE_BERRY_AMOUNTS);
                float count = value != null ? value : 0;
                amount = Helper.computeIncrements(level.random,count);
            }
            if(amount>0){
                Block.popResource(level, pos, new ItemStack(Items.SWEET_BERRIES,amount));
            }
        }
    }
}
