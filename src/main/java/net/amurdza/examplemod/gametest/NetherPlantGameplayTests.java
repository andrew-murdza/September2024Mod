package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.RetainedContent;
import net.amurdza.examplemod.worldgen.DocumentedFauna;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NetherPlantGameplayTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void powderCaneDropsOneCaneAndOnlyMatureTopsDropPepper(GameTestHelper helper) {
        var cane = ForgeRegistries.ITEMS.getValue(new ResourceLocation("mynethersdelight:powder_cannon"));
        helper.assertTrue(cane != null && cane != Items.AIR && RetainedContent.isKeptItem(new ItemStack(cane)),
                "The native powder cane item must remain available in the curated content");
        var pepper = ForgeRegistries.ITEMS.getValue(new ResourceLocation("mynethersdelight:bullet_pepper"));
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 18);
        for (String id : new String[]{"powdery_cane", "powdery_cannon", "powdery_chubby_sapling", "bullet_pepper"}) {
            Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("mynethersdelight", id));
            helper.assertTrue(block != null && block != Blocks.AIR, "Missing cane stage " + id);
            // Exercise the actual loot tables across every growth/pressure/leaf state.
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                var drops = Block.getDrops(state, helper.getLevel(), pos,
                        null, null, ItemStack.EMPTY);
                helper.assertTrue(drops.stream().filter(drop -> drop.is(cane)).mapToInt(ItemStack::getCount).sum() == 1,
                        "Every cane state must drop exactly one replantable cane: " + state + " / " + drops);
                boolean mature = state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT)
                        && (!state.hasProperty(BlockStateProperties.AGE_3) || state.getValue(BlockStateProperties.AGE_3) == 3);
                helper.assertTrue(drops.stream().filter(drop -> drop.is(pepper)).mapToInt(ItemStack::getCount).sum() == (mature ? 1 : 0)
                                && drops.size() == (mature ? 2 : 1),
                        "Only a mature top may add one bullet pepper: " + state + " / " + drops);
            }
            if (id.equals("powdery_chubby_sapling")) continue;
            BlockState mature = block.defaultBlockState().trySetValue(BlockStateProperties.AGE_3, 3)
                    .setValue(BlockStateProperties.LIT, true);
            // Lower stalks must not become pepper sources even if an older save
            // retained a mature/lit state beneath another cane segment.
            for (String upperId : new String[]{"powdery_cane", "powdery_cannon", "bullet_pepper"}) {
                var upper = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("mynethersdelight", upperId));
                helper.getLevel().setBlock(pos.above(), upper.defaultBlockState(), 18);
                helper.assertTrue(helper.getLevel().getBlockState(pos.above()).is(upper), "Upper stalk fixture must exist");
                var drops = Block.getDrops(mature, helper.getLevel(), pos, null, null, ItemStack.EMPTY);
                helper.assertTrue(drops.size() == 1 && drops.get(0).is(cane),
                        "A mature lower stalk must drop cane without peppers beneath " + upperId);
            }
            helper.getLevel().setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 18);
        }
        helper.assertFalse(ForgeRegistries.BLOCKS.getKeys().contains(AOEMod.makeID("soul_berry_bush"))
                        || ForgeRegistries.ITEMS.getKeys().contains(AOEMod.makeID("soul_berries")),
                "The retired soul berry plant and item must be absent");
        var menu = new AbstractContainerMenu(null, -1) {
            @Override
            public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
            @Override
            public boolean stillValid(Player player) { return true; }
        };
        var grid = new TransientCraftingContainer(menu, 2, 2);
        grid.setItem(0, new ItemStack(cane));
        var level = helper.getLevel();
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level);
        helper.assertTrue(recipe.isPresent(), "One powder cane must have a loaded crafting recipe");
        var result = recipe.orElseThrow().assemble(grid, level.registryAccess());
        helper.assertTrue(result.is(Items.GUNPOWDER) && result.getCount() == 1,
                "One powder cane must craft exactly one gunpowder");
        for (int count = 1; count <= 2; count++) {
            if (count == 2) grid.setItem(2, new ItemStack(cane));
            helper.assertFalse(level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING).stream()
                            .anyMatch(candidate -> candidate.matches(grid, level)
                                    && candidate.getResultItem(level.registryAccess()).is(Items.STICK)),
                    "Neither one nor two powder canes may craft sticks");
        }
        helper.assertFalse(level.getRecipeManager().byKey(new ResourceLocation("mynethersdelight:cutting/powder_cannon")).isPresent(),
                "The dependency's stick-producing cutting recipe must remain filtered out");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void maturePowderCaneNeverDetonatesOnContactOrHarvest(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.NETHER);
        BlockPos fixture = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos pos = new BlockPos(fixture.getX(), 22, fixture.getZ());
        Map<BlockPos, BlockState> original = new LinkedHashMap<>();
        for (int dy = -2; dy <= 3; dy++) original.put(pos.offset(0, dy, 0), level.getBlockState(pos.offset(0, dy, 0)));
        var player = helper.makeMockSurvivalPlayer();
        var visitor = EntityType.PIG.create(level);
        visitor.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        // The grown flower top only triggers its contact path when the visitor moved.
        visitor.xo = pos.getX() - 0.5;
        visitor.zo = pos.getZ() + 0.5;
        var probe = new ExplosionProbe();
        MinecraftForge.EVENT_BUS.register(probe);
        try {
            level.setBlock(pos.below(), Blocks.BASALT.defaultBlockState(), 18);
            helper.assertTrue(level.getBlockState(pos.below()).is(Blocks.BASALT), "Fixture must have actual basalt support");
            for (int dy = 0; dy <= 3; dy++) level.setBlock(pos.above(dy), Blocks.AIR.defaultBlockState(), 18);
            for (String id : new String[]{"powdery_cane", "powdery_cannon", "bullet_pepper"}) {
                if (id.equals("bullet_pepper")) {
                    level.setBlock(pos.below(2), Blocks.BASALT.defaultBlockState(), 18);
                    Block cane = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("mynethersdelight:powdery_cane"));
                    helper.assertTrue(level.setBlock(pos.below(), cane.defaultBlockState(), 18)
                                    && level.getBlockState(pos.below()).is(cane),
                            "The grown powder flower fixture must sit on an actual cane stalk");
                }
                Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation("mynethersdelight", id));
                BlockState mature = block.defaultBlockState();
                for (var property : mature.getProperties()) {
                    if (property.getName().equals("lit")) mature = mature.setValue((BooleanProperty) property, true);
                    if (property.getName().equals("pressure")) mature = mature.setValue((IntegerProperty) property, 2);
                    if (property.getName().equals("age")) {
                        var age = (IntegerProperty) property;
                        mature = mature.setValue(age, age.getPossibleValues().stream().max(Integer::compareTo).orElseThrow());
                    }
                }
                helper.assertTrue(level.setBlock(pos, mature, 18) && level.getBlockState(pos).equals(mature),
                        "Fixture must contain the mature " + id + " inside Nether build bounds");
                helper.assertTrue(mature.canSurvive(level, pos), "Powder cane fixture must have valid support");
                level.getBlockState(pos).entityInside(level, pos, visitor);
                block.stepOn(level, pos, mature, visitor);
                mature.tick(level, pos, level.random);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
                var use = block.use(mature, level, pos, player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
                helper.assertTrue(use == InteractionResult.PASS,
                        "Shears must not bypass the mature-top block loot with a native pepper harvest");
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                block.playerWillDestroy(level, pos, mature, player);
                helper.assertTrue(probe.explosions == 0,
                        "Mature " + id + " must not explode on contact, pressure ticks, or harvesting");
            }
        } finally {
            MinecraftForge.EVENT_BUS.unregister(probe);
            original.forEach((savedPos, savedState) -> level.setBlock(savedPos, savedState, 18));
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void netherLivestockUseTheirRequestedBiomes(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        for (String name : new String[]{"crimson_forest", "warped_forest", "soul_sand_valley", "basalt_deltas"}) {
            var biome = registry.getHolderOrThrow(ResourceKey.create(Registries.BIOME, AOEMod.makeID(name))).value();
            var types = Arrays.stream(MobCategory.values()).flatMap(category -> biome.getMobSettings()
                            .getMobs(category).unwrap().stream())
                    .map(spawn -> ForgeRegistries.ENTITY_TYPES.getKey(spawn.type).toString()).collect(Collectors.toSet());
            helper.assertTrue(types.contains("minecraft:strider") == name.equals("warped_forest"),
                    "Striders must spawn only in warped forest: " + name);
            helper.assertTrue(types.contains("minecraft:hoglin") == name.equals("crimson_forest"),
                    "Hoglins must spawn only in crimson forest: " + name);
            helper.assertFalse(types.contains("alexsmobs:warped_toad"),
                    "Warped toads must be absent from natural Nether spawning: " + name);
            if (name.equals("soul_sand_valley")) helper.assertTrue(DocumentedFauna.forBiome(name).stream()
                            .noneMatch(species -> species.habitat() == DocumentedFauna.Habitat.LAND),
                    "The soul sand valley must contain no land livestock");
        }
        helper.succeed();
    }

    public static final class ExplosionProbe {
        private int explosions;

        @SubscribeEvent
        public void explosion(ExplosionEvent.Start event) {
            explosions++;
        }
    }
}
