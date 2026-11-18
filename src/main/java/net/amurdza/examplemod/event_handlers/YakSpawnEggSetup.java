package net.amurdza.examplemod.event_handlers;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.item.YakSpawnEggItem;
import net.amurdza.examplemod.registry.ModEntities;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = AOEMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class YakSpawnEggSetup {
    private YakSpawnEggSetup() { }

    @SubscribeEvent
    public static void creativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) event.accept(ModEntities.YAK_SPAWN_EGG.get());
    }

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> DispenserBlock.registerBehavior(ModEntities.YAK_SPAWN_EGG.get(),
                new DefaultDispenseItemBehavior() {
                    @Override
                    protected ItemStack execute(BlockSource source, ItemStack stack) {
                        Direction face = source.getBlockState().getValue(DispenserBlock.FACING);
                        YakSpawnEggItem.prepareStack(stack);
                        if (ModEntities.YAK.get().spawn(source.getLevel(), stack, null, source.getPos().relative(face),
                                MobSpawnType.DISPENSER, face != Direction.UP, false) != null) {
                            stack.shrink(1);
                            source.getLevel().gameEvent(GameEvent.ENTITY_PLACE, source.getPos(),
                                    GameEvent.Context.of(source.getBlockState()));
                        }
                        return stack;
                    }
                }));
    }
}
