package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.registry.ModCreativeTabs;
import net.amurdza.examplemod.registry.ModBlocks;
import net.amurdza.examplemod.util.BiomePlacementRestrictions;
import net.amurdza.examplemod.util.DimensionMaterialAvailability;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.util.ObtainableContent;
import net.amurdza.examplemod.util.RetainedContent;
import net.amurdza.examplemod.worldgen.structure.SavannaOrcCampProcessor;
import net.amurdza.examplemod.worldgen.structure.OrcCampStock;
import net.amurdza.examplemod.worldgen.structure.OrcVillageLoot;
import com.blackgear.vanillabackport.common.level.entities.decoration.Cushion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SavannaCampAvailabilityTests {
    private SavannaCampAvailabilityTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void smoothBoneFamilyAppearsInItsCreativeCatalogAfterReload(GameTestHelper helper) {
        var level = helper.getLevel();
        ObtainableContent.rebuild(level.getRecipeManager(), level.registryAccess());
        CreativeModeTabs.tryRebuildTabContents(level.enabledFeatures(), true, level.registryAccess());
        var catalog = ModCreativeTabs.catalogs().get("alexscaves").get();
        for (String path : List.of("smooth_bone", "smooth_bone_slab", "smooth_bone_stairs", "smooth_bone_wall")) {
            var id = new ResourceLocation("alexscaves", path);
            helper.assertTrue(ForgeRegistries.ITEMS.containsKey(id), "Smooth bone item is not registered: " + id);
            var item = ForgeRegistries.ITEMS.getValue(id);
            helper.assertTrue(RetainedContent.isKeptItem(new ItemStack(item))
                            && catalog.getDisplayItems().stream().anyMatch(stack -> stack.is(item)),
                    "The requested smooth-bone family must remain visible after recipe and creative reload: " + id);
        }
        var orcCatalog = ModCreativeTabs.catalogs().get("orcz").get();
        for (String path : List.of("palisadedarkoak", "pallisade")) {
            var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("orcz", path));
            helper.assertTrue(item != null && !RetainedContent.isKeptItem(new ItemStack(item))
                            && orcCatalog.getDisplayItems().stream().noneMatch(stack -> stack.is(item)),
                    "Replaced native Orcz palisades must stay registered but leave the creative catalog: " + path);
        }
        helper.assertTrue(ModCreativeTabs.AOE_MOD.get().getDisplayItems().stream()
                        .anyMatch(stack -> stack.is(ModBlocks.ACACIA_PALISADE.get().asItem())),
                "The acacia palisade must remain available in the AoE creative catalog");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void replacementCampMaterialsArePlaceableInSavanna(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        BlockPos pos = new BlockPos(8, 100, 8);
        for (String name : List.of("alexscaves:smooth_bone", "alexscaves:smooth_bone_slab",
                "alexscaves:smooth_bone_stairs", "alexscaves:smooth_bone_wall", "quark:charcoal_block",
                "minecraft:bone_block", "minecraft:black_wool", "minecraft:black_wool_slab", "minecraft:black_wool_stairs",
                "minecraft:acacia_log", "minecraft:acacia_fence", "minecraft:acacia_slab", "aoemod:acacia_palisade",
                "minecraft:acacia_leaves", "minecraft:grass_block", "minecraft:rooted_dirt",
                "minecraft:dirt", "minecraft:mud", "minecraft:campfire", "minecraft:red_wool", "minecraft:hay_block",
                "minecraft:straw_bed", "minecraft:torch", "minecraft:acacia_fence_gate")) {
            var id = new ResourceLocation(name);
            helper.assertTrue(ForgeRegistries.BLOCKS.containsKey(id), "Camp material is not registered: " + id);
            helper.assertTrue(BiomePlacementRestrictions.isAllowed(level, pos,
                            ForgeRegistries.BLOCKS.getValue(id).defaultBlockState()),
                    "Savanna placement policy must permit the requested camp material: " + id);
        }
        var cushion = new ResourceLocation("minecraft", "red_cushion");
        helper.assertTrue(ForgeRegistries.ITEMS.containsKey(cushion)
                        && RetainedContent.isKeptItem(new ItemStack(ForgeRegistries.ITEMS.getValue(cushion))),
                "The native red cushion item must remain available");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void podzolPlacementIsUnavailableOnlyInSavanna(GameTestHelper helper) {
        for (var dimension : List.of(GameplayDimensions.SAVANNA, GameplayDimensions.PLAINS, GameplayDimensions.SNOWY)) {
            var level = helper.getLevel().getServer().getLevel(dimension);
            boolean allowed = !dimension.equals(GameplayDimensions.SAVANNA);
            var pos = new BlockPos(31840, 240, 31840);
            var state = Blocks.PODZOL.defaultBlockState();
            helper.assertTrue(BiomePlacementRestrictions.isAllowed(level, pos, state) == allowed
                            && DimensionMaterialAvailability.isAvailable(level, Blocks.PODZOL.asItem(),
                            BiomePlacementRestrictions.dimensionMask(level)) == allowed,
                    "Podzol availability and placement policy must exclude Savanna while retaining Plains and Snowy");
            level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 18);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
            var player = FakePlayerFactory.getMinecraft(level);
            player.moveTo(pos.getX() + 3, pos.getY(), pos.getZ() + 3);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Blocks.PODZOL, 2));
            var context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, player.getMainHandItem(),
                    new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0, .5, 0), Direction.UP, pos.below(), false));
            try {
                boolean placed = ((BlockItem) Blocks.PODZOL.asItem()).place(context).consumesAction();
                helper.assertTrue(placed == allowed && level.getBlockState(pos).is(allowed ? Blocks.PODZOL : Blocks.AIR),
                        "The real podzol item placement path must enforce the policy in " + dimension.location());
                helper.assertTrue(player.getMainHandItem().getCount() == (allowed ? 1 : 2),
                        "Rejected Savanna placement must not consume the podzol item");
            } finally {
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 18);
                level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 18);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void everyCampLootRollUsesUsefulSavannaMaterials(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        var table = level.getServer().getLootData().getLootTable(SavannaOrcCampProcessor.LOOT_TABLE);
        var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                .create(LootContextParamSets.CHEST);
        helper.assertTrue(OrcVillageLoot.PREVIOUS_CHESTS == 8 && OrcVillageLoot.CHESTS == 36,
                "The village must divide its former eight-chest budget among thirty-six chests");
        for (int seed = 0; seed < 64; seed++) {
            var data = new CompoundTag();
            data.putLong(OrcVillageLoot.SEED, seed);
            data.putLong(OrcVillageLoot.ORIGIN, new BlockPos(131, 80, -712).asLong());
            data.putInt(OrcCampStock.STOCK_TAG, seed * 73 & 255);
            var budget = OrcVillageLoot.budget(table, params, data);
            helper.assertTrue(!budget.isEmpty() && budget.stream().anyMatch(ItemStack::isEdible)
                            && budget.stream().anyMatch(stack -> !stack.isEdible()),
                    "The complete village budget must contain both provisions and useful supplies, seed=" + seed);
            for (var stack : budget) helper.assertTrue(RetainedContent.isKeptItem(stack)
                            && DimensionMaterialAvailability.isAvailable(level, stack.getItem(), BiomePlacementRestrictions.SAVANNA),
                    "Camp loot is unavailable in Savanna: " + ForgeRegistries.ITEMS.getKey(stack.getItem()));
            Map<String, Integer> distributed = new HashMap<>();
            int smallest = Integer.MAX_VALUE, largest = 0;
            for (int chest = 0; chest < OrcVillageLoot.CHESTS; chest++) {
                data.putInt(OrcVillageLoot.INDEX, chest);
                var share = OrcVillageLoot.share(table, params, data);
                helper.assertFalse(share.isEmpty(), "Each village chest must receive its share, seed=" + seed + ", chest=" + chest);
                int amount = share.stream().mapToInt(ItemStack::getCount).sum();
                smallest = Math.min(smallest, amount); largest = Math.max(largest, amount);
                quantities(share).forEach((item, count) -> distributed.merge(item, count, Integer::sum));
            }
            helper.assertTrue(distributed.equals(quantities(budget)) && largest - smallest <= 1,
                    "All thirty-six shares must conserve every item and divide the old budget evenly, seed=" + seed);
        }
        helper.succeed();
    }

    private static Map<String, Integer> quantities(Iterable<ItemStack> stacks) {
        Map<String, Integer> counts = new HashMap<>();
        for (ItemStack stack : stacks) if (!stack.isEmpty()) counts.merge(
                ForgeRegistries.ITEMS.getKey(stack.getItem()) + ":" + stack.getTag(), stack.getCount(), Integer::sum);
        return counts;
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void cushionsClearLateGrassWithoutRemovingAdjacentPlants(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos seat = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlock(seat.below(), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        level.setBlock(seat.east().below(), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
        level.setBlock(seat.east(), Blocks.GRASS.defaultBlockState(), 3);
        var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("minecraft", "cushion"));
        Cushion cushion = (Cushion) type.create(level);
        cushion.setPos(seat.getX() + .5, seat.getY(), seat.getZ() + .5);
        level.addFreshEntity(cushion);
        level.setBlock(seat, Blocks.GRASS.defaultBlockState(), 3);
        cushion.tick();
        helper.assertTrue(level.getBlockState(seat).isAir(), "Grass decorating a cushion's block must be removed");
        DoublePlantBlock.placeAt(level, Blocks.TALL_GRASS.defaultBlockState(), seat, 3);
        cushion.tick();
        helper.assertTrue(level.getBlockState(seat).isAir() && level.getBlockState(seat.above()).isAir(),
                "Clearing tall grass must remove its upper half too");
        helper.assertTrue(level.getBlockState(seat.east()).is(Blocks.GRASS), "Adjacent grass must remain untouched");
        cushion.discard();
        helper.succeed();
    }
}
