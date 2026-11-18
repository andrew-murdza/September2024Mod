package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.ColdCowYaks;
import net.amurdza.examplemod.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.amurdza.examplemod.entity.YakEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class YakSpawnEggTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void yakEggSpawnsColdAdultsAndBabiesWithoutReplacingCowEgg(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockSurvivalPlayer();
        var egg = ModEntities.YAK_SPAWN_EGG.get();
        helper.assertTrue(SpawnEggItem.byId(EntityType.COW) == Items.COW_SPAWN_EGG
                        && ForgeSpawnEggItem.fromEntityType(EntityType.COW) == Items.COW_SPAWN_EGG,
                "Adding a variant egg must not change the vanilla cow egg lookup");
        var stack = new ItemStack(egg, 2);
        stack.setHoverName(Component.literal("Test Yak"));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos ground = helper.absolutePos(new BlockPos(2, 1, 2));
        level.setBlockAndUpdate(ground, Blocks.GRASS_BLOCK.defaultBlockState());
        egg.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(ground), Direction.UP, ground, false)));
        var cows = level.getEntitiesOfClass(YakEntity.class, new AABB(ground).inflate(2),
                cow -> cow.hasCustomName() && cow.getCustomName().getString().equals("Test Yak"));
        helper.assertTrue(cows.size() == 1 && cows.get(0).getType() == ModEntities.YAK.get() && stack.getCount() == 1,
                "A plain registered egg must produce one dedicated yak and consume one egg");
        var saved = new CompoundTag();
        cows.get(0).saveWithoutId(saved);
        var reloaded = ModEntities.YAK.get().create(level);
        reloaded.load(saved);
        helper.assertTrue(reloaded.getType() == ModEntities.YAK.get(), "The spawned yak must retain its variant after saving");
        var parent = ModEntities.YAK.get().create(level);
        var child = egg.spawnOffspringFromSpawnEgg(player, parent, ModEntities.YAK.get(), level,
                Vec3.atCenterOf(ground.above()), stack);
        helper.assertTrue(child.isPresent() && child.get().isBaby() && child.get().getType() == ModEntities.YAK.get()
                        && stack.isEmpty(),
                "Using a yak egg on an yak must create a yak baby, not inherit the parent's variant");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void yakEggKeepsColdVariantInSpawner(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockSurvivalPlayer();
        var stack = new ItemStack(ModEntities.YAK_SPAWN_EGG.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        level.setBlockAndUpdate(pos, Blocks.SPAWNER.defaultBlockState());
        var spawner = (SpawnerBlockEntity) level.getBlockEntity(pos);
        spawner.getSpawner().setEntityId(EntityType.PIG, level, level.random, pos);
        var oldSpawner = spawner.getSpawner().save(new CompoundTag());
        oldSpawner.remove("SpawnPotentials");
        spawner.getSpawner().load(level, pos, oldSpawner);
        helper.assertTrue(spawner.getSpawner().save(new CompoundTag()).getList("SpawnPotentials", 10).size() == 1,
                "The fixture must begin with an existing non-yak weighted choice");
        ModEntities.YAK_SPAWN_EGG.get().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
        CompoundTag saved = spawner.getSpawner().save(new CompoundTag());
        CompoundTag entity = saved.getCompound("SpawnData").getCompound("entity");
        var cow = ModEntities.YAK.get().create(level);
        cow.load(entity);
        helper.assertTrue(entity.getString("id").equals("aoemod:yak")
                        && stack.isEmpty(),
                "Spawner data must save the dedicated yak type and consume one egg");
        var potentials = saved.getList("SpawnPotentials", 10);
        helper.assertTrue(potentials.size() == 1 && potentials.getCompound(0).getCompound("data")
                        .getCompound("entity").getString("id").equals("aoemod:yak"),
                "Spawner must not later select its old non-yak spawn potentials");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 40)
    public static void dispenserUsesRegisteredYakEggBehavior(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(pos, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
        var dispenser = (DispenserBlockEntity) level.getBlockEntity(pos);
        var stack = new ItemStack(ModEntities.YAK_SPAWN_EGG.get(), 2);
        stack.setHoverName(Component.literal("Dispensed Yak"));
        dispenser.setItem(0, stack);
        level.setBlockAndUpdate(pos.west(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        helper.runAfterDelay(8, () -> {
            var cows = level.getEntitiesOfClass(YakEntity.class, new AABB(pos).inflate(3),
                    cow -> cow.hasCustomName() && cow.getCustomName().getString().equals("Dispensed Yak"));
            helper.assertTrue(cows.size() == 1 && cows.get(0).getType() == ModEntities.YAK.get()
                            && dispenser.getItem(0).getCount() == 1,
                    "A powered dispenser must spawn one yak and retain the remaining egg");
            helper.succeed();
        });
    }
}
