package net.amurdza.examplemod.event_handlers;

import com.github.alexthe666.alexsmobs.entity.*;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import net.amurdza.examplemod.Config;
import net.amurdza.examplemod.Helper;
import net.amurdza.examplemod.block.ModBlocks;
import net.amurdza.examplemod.item.ModItems;
import net.amurdza.examplemod.util.ModTags;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Donkey;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.violetmoon.quark.content.world.module.GlimmeringWealdModule;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static net.amurdza.examplemod.AOEMod.MOD_ID;
import static net.amurdza.examplemod.Config.BLACKLISTED_USE_ITEMS;

@Mod.EventBusSubscriber(modid = MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public class UseEvent {

    private static final Map<DyeColor, Item> woolMap = new HashMap<>();
    static {
        woolMap.put(DyeColor.WHITE, Items.WHITE_WOOL);
        woolMap.put(DyeColor.ORANGE, Items.ORANGE_WOOL);
        woolMap.put(DyeColor.MAGENTA, Items.MAGENTA_WOOL);
        woolMap.put(DyeColor.LIGHT_BLUE, Items.LIGHT_BLUE_WOOL);
        woolMap.put(DyeColor.YELLOW, Items.YELLOW_WOOL);
        woolMap.put(DyeColor.LIME, Items.LIME_WOOL);
        woolMap.put(DyeColor.PINK, Items.PINK_WOOL);
        woolMap.put(DyeColor.GRAY, Items.GRAY_WOOL);
        woolMap.put(DyeColor.LIGHT_GRAY, Items.LIGHT_GRAY_WOOL);
        woolMap.put(DyeColor.CYAN, Items.CYAN_WOOL);
        woolMap.put(DyeColor.PURPLE, Items.PURPLE_WOOL);
        woolMap.put(DyeColor.BLUE, Items.BLUE_WOOL);
        woolMap.put(DyeColor.BROWN, Items.BROWN_WOOL);
        woolMap.put(DyeColor.GREEN, Items.GREEN_WOOL);
        woolMap.put(DyeColor.RED, Items.RED_WOOL);
        woolMap.put(DyeColor.BLACK, Items.BLACK_WOOL);
    }

    @SubscribeEvent
    public static void extraOrCancelBreeding(PlayerInteractEvent.EntityInteract event){
        if(event.getTarget() instanceof Mob mob){
            BlockPos pos=mob.getOnPos();
            if(!Helper.isOkMob(event.getLevel(),pos,mob)){
                event.setCanceled(true);
            }
            else {
                Player player=event.getEntity();
                ItemStack stack=event.getItemStack();


                if(mob instanceof MushroomCow){
                    useFood(mob,player,stack, Items.BROWN_MUSHROOM,Items.RED_MUSHROOM, GlimmeringWealdModule.glow_shroom_block.asItem());
                    if(event.getItemStack().is(Items.SHEARS)){
                        event.setCanceled(true);
                    }
                }
                else if(mob instanceof Sheep sheep&&event.getItemStack().is(Items.SHEARS)){
                    Item wool=woolMap.get(sheep.getColor());
                    mob.spawnAtLocation(new ItemStack(wool,Config.WOOL_FROM_SHEAR));
                    event.setCanceled(true);
                }
                else if(mob instanceof Rabbit){
                    useFood(mob,player,stack,ModTags.Items.smallerFlowers);
                }
                else if(mob instanceof Horse || mob instanceof Donkey){
                    useFood(mob,player,stack,Items.HAY_BLOCK);
                }
                else if(mob instanceof Strider){
                    useFood(mob,player,stack, Items.CRIMSON_ROOTS,Items.WARPED_ROOTS);
                }
                else if(mob instanceof Frog){
                    useFood(mob,player,stack,Items.SEAGRASS);
                }
                else if(mob instanceof Ocelot ||mob instanceof Cat){
                    useFood(mob,player,stack, Items.COD,Items.TROPICAL_FISH,Items.SALMON);
                }
                else if(mob instanceof Fox){
                    useFood(mob,player,stack, Items.CHICKEN,Items.RABBIT,Items.COD,Items.TROPICAL_FISH,Items.SALMON);
                }
                else if(mob instanceof Parrot){
                    useFood(mob,player,stack,Items.APPLE);
                }
                else if(mob instanceof PolarBear){
                    useFood(mob,player,stack, Items.BEEF,Items.MUTTON,Items.COD,Items.SALMON);
                }
                else if(mob instanceof EntityMoose){
                    useFood(mob,player,stack,ModTags.Items.smallerFlowers);
                }
                else if(mob instanceof EntityJerboa){
                    useFood(mob,player,stack, Items.DEAD_BUSH,Items.GRASS,Items.FERN);
                    useFood(mob,player,stack,ModTags.Items.smallerFlowers);
                }
                else if(mob instanceof EntityRoadrunner ||mob instanceof EntityBlueJay ||mob instanceof EntityMudskipper
                        ||mob instanceof EntityPotoo){
                    useFood(mob, player,stack,Items.COD,Items.SALMON,Items.TROPICAL_FISH);
                }
                else if(mob instanceof EntityRainFrog){
                    useFood(mob,  player,stack, Items.SEAGRASS);
                }
                else if(mob instanceof EntityBananaSlug){
                    useFood(mob,  player,stack, Items.RED_MUSHROOM, GlimmeringWealdModule.glow_shroom_block.asItem());
                }
                else if(mob instanceof EntityKangaroo){
                    useFood(mob,  player,stack, Items.FERN);
                }
                else if(mob instanceof EntityCapuchinMonkey){
                    useFood(mob,  player,stack, AMItemRegistry.BANANA.get());
                }
            }
        }
    }


    //Black Listed Blocks
    @SubscribeEvent
    public static void cancelPlace(PlayerInteractEvent.RightClickBlock event){
        final LevelAccessor level = event.getLevel();
        final BlockPos pos = event.getPos();
        if(Helper.isSpecialBiome(level,pos)){
            final Item item=event.getItemStack().getItem();
            if(!item.equals(Items.AIR)&&BLACKLISTED_USE_ITEMS.contains(item)){
                String message="You cannot place "+item+" in starter biomes";//+SPECIAL_BIOME;
                Helper.sendMessage(event.getEntity(),message);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void gatherFromGrapes(PlayerInteractEvent.RightClickBlock event){
        Level pLevel=event.getLevel();
        BlockPos pPos=event.getPos();
        Player pEntity=event.getEntity();
        BlockState pState=event.getLevel().getBlockState(pPos);
        int numGrapes=Helper.isSpecialBiome(pLevel,pPos)?Config.NUM_GRAPES:1;
        if(pState.is(ModBlocks.GRAPE_VINE.get())&&pState.getValue(BlockStateProperties.BERRIES)){
            Block.popResource(pLevel, pPos, new ItemStack(ModBlocks.GRAPE_VINE.get().asItem(), numGrapes));
            float f = Mth.randomBetween(pLevel.random, 0.8F, 1.2F);
            pLevel.playSound(null, pPos, SoundEvents.CAVE_VINES_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, f);
            BlockState blockstate = pState.setValue(BlockStateProperties.BERRIES, false);
            pLevel.setBlock(pPos, blockstate, 2);
            pLevel.gameEvent(GameEvent.BLOCK_CHANGE, pPos, GameEvent.Context.of(pEntity, blockstate));
        }
    }

    @SubscribeEvent
    public static void shearsPreventGrowingBerries(PlayerInteractEvent.RightClickBlock event){
        Level level=event.getLevel();
        BlockPos blockpos=event.getPos();
        ItemStack itemStack=event.getItemStack();
        BlockState blockstate=event.getLevel().getBlockState(blockpos);
        if(itemStack.is(ItemTags.AXES)){
            if(blockstate.is(ModBlocks.CAVE_VINES_PLANT.get())||blockstate.is(ModBlocks.CAVE_VINES_HEAD.get())&&
                    blockstate.getValue(BlockStateProperties.ENABLED)){
                Player player = event.getEntity();
                if (player instanceof ServerPlayer) {
                    CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger((ServerPlayer)player, blockpos, itemStack);
                }
                level.playSound(player, blockpos, SoundEvents.GROWING_PLANT_CROP, SoundSource.BLOCKS, 1.0F, 1.0F);
                BlockState blockstate1 = blockstate.setValue(BlockStateProperties.ENABLED,false);
                level.setBlockAndUpdate(blockpos, blockstate1);
                level.gameEvent(GameEvent.BLOCK_CHANGE, blockpos, GameEvent.Context.of(player, blockstate1));
                if (player != null) {
                    itemStack.hurtAndBreak(1, player, (p_186374_) -> p_186374_.broadcastBreakEvent(event.getHand()));
                }
            }
        }
    }

    @SubscribeEvent
    public static void hoeOnMossPodzol(PlayerInteractEvent.RightClickBlock event){
        Level level=event.getLevel();
        Player player=event.getEntity();
        BlockPos pos=event.getPos();
        BlockState blockState=level.getBlockState(pos);
        if(event.getItemStack().getItem() instanceof HoeItem){
            if(blockState.is(Blocks.MOSS_BLOCK)|| blockState.is(Blocks.PODZOL)){
                level.playSound(player, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (!level.isClientSide) {
                    level.setBlock(pos,Blocks.FARMLAND.defaultBlockState(),11);
                    if (player != null) {
                        event.getItemStack().hurtAndBreak(1, player, (p_150845_) -> p_150845_.broadcastBreakEvent(event.getHand()));
                    }
                }
            }
        }
    }
    @SubscribeEvent
    public static void berriesFromUse(PlayerInteractEvent.RightClickBlock event){
        Level level=event.getLevel();
        BlockPos pos=event.getPos();
        BlockState state=level.getBlockState(pos);
        Block block=state.getBlock();
        if(Helper.isSpecialBiome(level,pos)){
            int amount=Math.max(Config.GLOW_BERRY_HARVEST_AMOUNT-1,0);
            if((block instanceof CaveVines)&&state.getValue(BlockStateProperties.BERRIES)){
                Block.popResource(level, pos, new ItemStack(ModItems.GLOW_BERRIES.get(), amount));
            }
            if(state.is(Blocks.SWEET_BERRY_BUSH)){
                int age=state.getValue(BlockStateProperties.AGE_3);
                amount=0;
                if(age==2){
                    amount= Config.SWEET_BERRIES_PARTIALLY_GROWN;
                }
                if(age==3){
                    amount = Config.SWEET_BERRIES_FULLY_GROWN;
                }
                if(amount>0){
                    Block.popResource(level, pos, new ItemStack(Items.SWEET_BERRIES,amount));
                }
            }
        }
    }

    private static void useItem(Player player, ItemStack stack){
        if(!player.getAbilities().instabuild){
            stack.shrink(1);
        }
    }

    private static void useFood(Entity entity, Player player, ItemStack stack, Item... items){
        useFood(entity,player,stack,p-> Arrays.asList(items).contains(p));
    }
    private static void useFood(Entity entity, Player player, ItemStack stack, TagKey<Item> itemTag){
        useFoodStack(entity,player,stack,p->p.is(itemTag));
    }
    private static void useFood(Entity entity, Player player, ItemStack stack, Item item){
        useFood(entity,player,stack,p-> p==item);
    }
    private static void useFoodStack(Entity entity, Player player, ItemStack stack, Function<ItemStack,Boolean> func){
        if(entity instanceof Animal animal &&func.apply(stack)){
            if(!animal.level().isClientSide&&animal.getAge()==0&&animal.canFallInLove()){
                if(!(animal instanceof TamableAnimal)||((TamableAnimal)animal).isTame()){
                    if(!(animal instanceof AbstractHorse)||((AbstractHorse)animal).isTamed()){
                        useItem(player,stack);
                        animal.setInLove(player);
                        animal.gameEvent(GameEvent.ENTITY_INTERACT,animal);
                        return;
                    }
                }
            }
            if(animal.isBaby()){
                useItem(player,stack);
                animal.ageUp((int)((float)(-animal.getAge() / 20) * 0.1F), true);
                animal.gameEvent(GameEvent.ENTITY_INTERACT,animal);
            }
        }
    }
    private static void useFood(Entity entity, Player player, ItemStack stack, Function<Item,Boolean> func){
        useFoodStack(entity,player,stack,t->func.apply(t.getItem()));
    }
}
