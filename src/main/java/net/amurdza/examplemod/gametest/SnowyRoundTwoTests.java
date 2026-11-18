package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.entity.YakEntity;
import net.amurdza.examplemod.event_handlers.CustomMobDrops;
import net.amurdza.examplemod.registry.ModEntities;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SnowyRoundTwoTests {
    private SnowyRoundTwoTests() { }

    @GameTest(template = "plant_gameplay_empty")
    public static void yakUsesActualDonkeyMenuAndPreservesOldOverflow(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        var player = helper.makeMockSurvivalPlayer();
        var yak = ModEntities.YAK.get().create(level);
        yak.moveTo(helper.absolutePos(new BlockPos(3, 2, 3)), 0, 0);
        yak.equipPack();
        helper.assertTrue(YakEntity.class.getMethod("openCustomInventoryScreen", Player.class).getDeclaringClass() == AbstractHorse.class,
                "Yak must use the same native open-horse-screen packet and client UI as a donkey");
        var donkey = EntityType.DONKEY.create(level);
        donkey.setChest(true);
        var menu = new HorseInventoryMenu(1, player.getInventory(), yak.packInventory(), yak);
        var expected = new HorseInventoryMenu(2, player.getInventory(), new net.minecraft.world.SimpleContainer(17), donkey);
        helper.assertTrue(yak.getInventoryColumns() == donkey.getInventoryColumns() && menu.slots.size() == expected.slots.size(),
                "Yak storage must match the donkey's fifteen-slot layout exactly");
        for (int slot = 0; slot < menu.slots.size(); slot++) {
            helper.assertTrue(menu.getSlot(slot).x == expected.getSlot(slot).x && menu.getSlot(slot).y == expected.getSlot(slot).y,
                    "Every native Yak inventory slot must use the donkey UI coordinates");
        }
        for (int slot = 2; slot < 17; slot++) yak.packInventory().setItem(slot, new ItemStack(Items.WHEAT, 64));
        ItemStack overflow = new ItemStack(Items.EMERALD, 9);
        overflow.setHoverName(Component.literal("Preserved old yak cargo"));
        CompoundTag saved = yak.saveWithoutId(new CompoundTag());
        CompoundTag oldSlot = overflow.save(new CompoundTag());
        oldSlot.putByte("Slot", (byte) 31);
        saved.getList("Items", Tag.TAG_COMPOUND).add(oldSlot);
        var restored = ModEntities.YAK.get().create(level);
        restored.load(saved);
        helper.assertTrue(restored.packInventory().getContainerSize() == 17,
                "Loading an old thirty-slot yak must not retain hidden active storage slots");
        CompoundTag beforeTick = restored.saveWithoutId(new CompoundTag());
        helper.assertTrue(beforeTick.getList("AoeYakCargoOverflow", Tag.TAG_COMPOUND).size() == 1,
                "Unclaimed old cargo must survive a save before its first world tick");
        var reloaded = ModEntities.YAK.get().create(level);
        reloaded.load(beforeTick);
        reloaded.aiStep();
        var returned = level.getEntitiesOfClass(ItemEntity.class, reloaded.getBoundingBox().inflate(3),
                item -> ItemStack.matches(item.getItem(), overflow));
        helper.assertTrue(returned.size() == 1 && !reloaded.saveWithoutId(new CompoundTag()).contains("AoeYakCargoOverflow"),
                "A full old pack must return its extra named stack beside the Yak exactly once");
        reloaded.aiStep();
        helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, reloaded.getBoundingBox().inflate(3),
                        item -> ItemStack.matches(item.getItem(), overflow)).size() == 1,
                "Migrated cargo must not duplicate on later ticks");
        for (var item : returned) item.discard();
        yak.discard(); restored.discard(); reloaded.discard(); donkey.discard();
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void undeadMountNamesAndDropsMatchTheirUnriddenCounterparts(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var savanna = server.getLevel(GameplayDimensions.SAVANNA);
        var desert = server.getLevel(GameplayDimensions.DESERT);
        LivingEntity[][] pairs = {
                {ModEntities.ZOMBIE_HORSEMAN.get().create(savanna), EntityType.ZOMBIE_HORSE.create(savanna)},
                {ModEntities.CAMEL_HUSK_JOCKEY.get().create(desert), ModEntities.CAMEL_HUSK.get().create(desert)}
        };
        for (LivingEntity[] pair : pairs) {
            LivingEntity mount = pair[0], ordinary = pair[1];
            helper.assertTrue(mount.getName().getString().equals(ordinary.getName().getString()),
                    "A mounted animal must keep the ordinary mount's name: " + mount.getName().getString());
            for (boolean burning : new boolean[]{false, true}) for (int looting : new int[]{0, 3}) {
                mount.setRemainingFireTicks(burning ? 100 : 0);
                ordinary.setRemainingFireTicks(burning ? 100 : 0);
                for (int seed = 0; seed < 64; seed++) {
                    helper.assertTrue(drops(mount, looting, seed).equals(drops(ordinary, looting, seed)),
                            "Mount drops must match its ordinary counterpart without a second rider reward");
                }
            }
            mount.discard(); ordinary.discard();
        }
        helper.succeed();
    }

    private static Map<Item, Integer> drops(LivingEntity entity, int looting, int seed) {
        entity.getRandom().setSeed(seed);
        var vanilla = new ArrayList<ItemEntity>();
        vanilla.add(new ItemEntity(entity.level(), 0, 0, 0, new ItemStack(Items.EMERALD, 4)));
        var event = new LivingDropsEvent(entity, entity.damageSources().generic(), vanilla, looting, true);
        CustomMobDrops.addConfiguredDrops(event);
        Map<Item, Integer> result = new HashMap<>();
        for (var item : event.getDrops()) result.merge(item.getItem().getItem(), item.getItem().getCount(), Integer::sum);
        return result;
    }
}
