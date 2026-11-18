package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.entity.ai.YakAccess;
import net.amurdza.examplemod.entity.ai.YakFollowCaravanGoal;
import net.amurdza.examplemod.event_handlers.ColdCowYaks;
import net.amurdza.examplemod.event_handlers.CustomMobDrops;
import net.amurdza.examplemod.event_handlers.LegacyYakMigration;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.amurdza.examplemod.registry.ModEntities;
import net.amurdza.examplemod.registry.RetainedCharnia;
import net.amurdza.examplemod.worldgen.SnowyCaveColumns;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import twilightforest.init.TFEntities;

import java.util.ArrayList;
import java.util.Arrays;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SnowyNovember14RevisionTests {
    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 2400)
    public static void exportPersistentSnowyVisualScene(GameTestHelper helper) throws Exception {
        var level = helper.getLevel().getServer().getLevel(net.amurdza.examplemod.util.GameplayDimensions.SNOWY);
        BlockPos center = new BlockPos(8192, level.getMaxBuildHeight() - 48, 8192);
        var piece = new net.amurdza.examplemod.worldgen.structure.BossLairPiece("yeti", center, 67);
        var bounds = piece.getBoundingBox();
        if (!level.getBlockState(center.above(18)).is(Blocks.BLUE_ICE)
                || !level.getBlockState(center.above(17)).isAir()
                || !level.getBlockState(center.above(23)).is(Blocks.BLUE_ICE)
                || !level.getBlockState(center).is(Blocks.SNOW_BLOCK)) {
            // The scene uses a full, real piece. A support layer avoids filling hundreds
            // of empty blocks below this deliberately high, isolated screenshot fixture.
            for (int x = bounds.minX(); x <= bounds.maxX(); x++) for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                level.setBlock(new BlockPos(x, center.getY() - 4, z), Blocks.STONE.defaultBlockState(), 2);
            }
            for (int x = bounds.minX() >> 4; x <= bounds.maxX() >> 4; x++) for (int z = bounds.minZ() >> 4; z <= bounds.maxZ() >> 4; z++) {
                var chunk = new net.minecraft.world.level.ChunkPos(x, z);
                var clip = new net.minecraft.world.level.levelgen.structure.BoundingBox(chunk.getMinBlockX(),
                        level.getMinBuildHeight(), chunk.getMinBlockZ(), chunk.getMaxBlockX(),
                        level.getMaxBuildHeight() - 1, chunk.getMaxBlockZ());
                piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
                        net.minecraft.util.RandomSource.create(197), clip, chunk, center);
            }
        }
        var sceneChunks = new java.util.ArrayList<net.minecraft.world.level.ChunkPos>();
        var forcedHere = new java.util.ArrayList<net.minecraft.world.level.ChunkPos>();
        for (int x = (center.getX() - 24) >> 4; x <= (center.getX() + 24) >> 4; x++) {
            for (int z = (center.getZ() - 24) >> 4; z <= (center.getZ() + 24) >> 4; z++) {
                var chunk = new net.minecraft.world.level.ChunkPos(x, z);
                sceneChunks.add(chunk);
                if (level.setChunkForced(x, z, true)) forcedHere.add(chunk);
            }
        }
        // Block chunks can be present before their asynchronous entity-region
        // reads finish. Wait for those reads before deleting previous actors.
        helper.startSequence().thenWaitUntil(() -> {
            for (var chunk : sceneChunks) helper.assertTrue(level.areEntitiesLoaded(chunk.toLong()),
                    "Waiting for persisted Snowy scene entities in " + chunk);
        }).thenExecute(() -> {
            try {
                populateSnowyVisualScene(helper, level, center);
            } catch (java.io.IOException failure) {
                throw new java.io.UncheckedIOException(failure);
            } finally {
                for (var chunk : forcedHere) level.setChunkForced(chunk.x, chunk.z, false);
            }
        }).thenSucceed();
    }

    private static void populateSnowyVisualScene(GameTestHelper helper, net.minecraft.server.level.ServerLevel level,
            BlockPos center) throws java.io.IOException {
        var scene = new net.minecraft.world.phys.AABB(center.offset(-24, 0, -24), center.offset(25, 18, 25));
        for (var old : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, scene)) {
            if (old.getPersistentData().getBoolean("aoemod:nov14_visual")) old.discard();
            else { old.setNoAi(true); old.setPersistenceRequired(); }
        }
        var yak = ModEntities.YAK.get().create(level);
        yak.setUUID(java.util.UUID.nameUUIDFromBytes("aoemod:nov14-visual-yak-entity".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        yak.moveTo(center.getX() - 12.5, center.getY() + 1, center.getZ() + 12.5, 0, 0);
        yak.setTamed(true);
        yak.setOwnerUUID(java.util.UUID.nameUUIDFromBytes("aoemod:nov14-visual-yak".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        yak.equipPack();
        yak.packInventory().setItem(0, new ItemStack(Items.SADDLE));
        yak.packInventory().setItem(16, new ItemStack(Items.WHEAT, 32));
        var knight = TFEntities.UPPER_GOBLIN_KNIGHT.get().create(level);
        knight.setUUID(java.util.UUID.nameUUIDFromBytes("aoemod:nov14-visual-knight".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        knight.moveTo(center.getX() - 5.5, center.getY() + 1, center.getZ() + 12.5, 0, 0);
        for (var mob : java.util.List.of(yak, knight)) {
            mob.setNoAi(true); mob.setPersistenceRequired(); mob.setInvulnerable(true); mob.setSilent(true);
            mob.getPersistentData().putBoolean("aoemod:nov14_visual", true);
            helper.assertTrue(level.addFreshEntity(mob), "Each stable-identity Snowy actor must be added exactly once");
        }
        helper.assertTrue(level.getEntitiesOfClass(twilightforest.entity.monster.UpperGoblinKnight.class, scene,
                        mob -> mob.getPersistentData().getBoolean("aoemod:nov14_visual")).size() == 1,
                "The fully loaded showcase must contain exactly one actual tagged Goblin Knight");
        helper.assertTrue(yak.hasChest() && yak.isSaddled() && knight.getBbHeight() >= 2.6F
                        && level.noCollision(yak) && level.noCollision(knight),
                "Screenshot actors need their real equipment and full bodies above the floor");
        var views = new java.util.ArrayList<java.util.Map<String, Object>>();
        views.add(snowyView("yak-chest-and-saddle", center, -16.5, 1, 17.5, -140, 10));
        views.add(snowyView("knight-full-body-front", center, -5.5, 1, 18.5, 180, 5));
        views.add(snowyView("knight-chestplate-back", center, -5.5, 1, 6.5, 0, 5));
        views.add(snowyView("yeti-flat-snow-wrapped-entrance", center, 0, 3, 5, 0, -5));
        views.add(snowyView("yeti-snow-exterior-blue-interior", center, 0, 3, 42, 180, -10));
        java.nio.file.Path root = java.nio.file.Path.of("").toAbsolutePath();
        while (root.getParent() != null && !java.nio.file.Files.isDirectory(root.resolve("src/main"))) root = root.getParent();
        var output = root.resolve("build/nov14-revision/snowy-views.json");
        java.nio.file.Files.createDirectories(output.getParent());
        java.nio.file.Files.writeString(output, new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(views));
        AOEMod.LOGGER.info("Persistent Snowy screenshot scene exported to {}", output);
    }

    private static java.util.Map<String, Object> snowyView(String label, BlockPos center,
            double x, double y, double z, float yaw, float pitch) {
        return java.util.Map.of("label", label, "dimension", "aoemod:snowy_dimension",
                "x", center.getX() + x, "y", center.getY() + y, "z", center.getZ() + z, "yaw", yaw, "pitch", pitch);
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void dedicatedYakUsesNativeMountInventoryAndMilk(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockSurvivalPlayer();
        var yak = ModEntities.YAK.get().create(level);
        yak.moveTo(helper.absolutePos(new BlockPos(3, 2, 3)), 0, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        yak.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getVehicle() == yak, "A wild yak must mount through the native donkey interaction");
        helper.assertTrue(yak.tameWithName(player), "Native horse taming must assign this yak's owner");
        player.stopRiding();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CHEST));
        yak.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(yak.hasChest() && player.getMainHandItem().isEmpty()
                        && yak.packInventory().getContainerSize() == 17 && yak.getInventoryColumns() == 5,
                "A real chest interaction must create the donkey's fifteen cargo slots plus native equipment slots");
        yak.packInventory().setItem(0, new ItemStack(Items.SADDLE));
        var cargo = new ItemStack(Items.DIAMOND, 17);
        cargo.setHoverName(Component.literal("Last yak cargo slot"));
        yak.packInventory().setItem(16, cargo);
        helper.assertTrue(yak.isSaddled() && yak.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent(),
                "Native container listeners must enable the saddle and expose cargo automation");
        var menu = new HorseInventoryMenu(1, player.getInventory(), yak.packInventory(), yak);
        helper.assertTrue(menu.slots.size() == 53 && menu.getSlot(16).getItem().getCount() == 17,
                "The native donkey menu must expose its equipment, fifteen cargo slots and player inventory");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        yak.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().is(Items.MILK_BUCKET), "A chested yak must still give milk");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        yak.mobInteract(player, InteractionHand.MAIN_HAND);
        helper.assertTrue(yak.getControllingPassenger() == player && yak.canJump(),
                "The native saddled mount must support steering and the riding jump protocol");
        helper.assertTrue(yak.getAttributeValue(Attributes.MOVEMENT_SPEED) < .175,
                "Yaks must be slower than native donkeys");
        level.addFreshEntity(yak);
        var mirroredMenu = new HorseInventoryMenu(2, player.getInventory(), new net.minecraft.world.SimpleContainer(17), yak);
        mirroredMenu.setItem(16, 1, cargo.copy());
        helper.assertTrue(mirroredMenu.slots.size() == 53 && ItemStack.matches(cargo, mirroredMenu.getSlot(16).getItem()),
                "The native horse-screen client mirror must receive the final donkey cargo slot");
        var follower = ModEntities.YAK.get().create(level);
        follower.moveTo(yak.getX() + 4, yak.getY(), yak.getZ());
        var caravan = new YakFollowCaravanGoal(follower);
        helper.assertTrue(caravan.canUse(), "A ridden yak must anchor a native-yak caravan");
        caravan.start();
        helper.assertTrue(follower.caravanHead() == yak && yak.caravanTail() == follower && caravan.canContinueToUse(),
                "Caravan links must form a single-file train");
        caravan.stop();
        player.stopRiding();
        var saved = yak.saveWithoutId(new CompoundTag());
        yak.discard();
        var restored = ModEntities.YAK.get().create(level);
        restored.load(saved);
        helper.assertTrue(restored.isTamed() && restored.hasChest() && restored.isSaddled()
                        && player.getUUID().equals(restored.getOwnerUUID())
                        && ItemStack.matches(cargo, restored.packInventory().getItem(16)),
                "Native reload must retain owner, saddle, chest and the last cargo slot's complete NBT");
        helper.assertTrue(restored.getBreedOffspring(level, follower).getType() == ModEntities.YAK.get(),
                "Yak breeding must produce the dedicated type rather than cows, donkeys or mules");
        restored.discard();
        follower.discard();
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void legacyCowYaksKeepTheirOwnerIdentityAndCargo(GameTestHelper helper) {
        var cow = EntityType.COW.create(helper.getLevel());
        ColdCowYaks.makeCold(cow);
        var old = (YakAccess) cow;
        var owner = helper.makeMockSurvivalPlayer();
        old.aoe$setYakOwner(owner.getUUID());
        old.aoe$setYakChest(true);
        old.aoe$yakInventory().setItem(29, new ItemStack(Items.EMERALD, 9));
        cow.setCustomName(Component.literal("Saved pack yak"));
        var yak = LegacyYakMigration.convert(cow, helper.getLevel());
        helper.assertTrue(yak.getUUID().equals(cow.getUUID()) && owner.getUUID().equals(yak.getOwnerUUID())
                        && yak.isTamed() && yak.isSaddled() && yak.hasChest()
                        && yak.packInventory().getItem(2).getCount() == 9
                        && yak.getCustomName().equals(cow.getCustomName()),
                "Changing the saved entity class must preserve identity, name, owner and cargo without losing ride access");
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void riddenNautilusFallsOutsideWaterEvenWhenLookingUp(GameTestHelper helper) {
        // A real rider's client owns vanilla mounted physics. A mock server
        // player is never local, so explicitly exercise that same physics path.
        var nautilus = new net.amurdza.examplemod.entity.future.NautilusEntity(ModEntities.NAUTILUS.get(), helper.getLevel()) {
            @Override public boolean isControlledByLocalInstance() { return true; }
        };
        var player = helper.makeMockSurvivalPlayer();
        nautilus.moveTo(helper.absolutePos(new BlockPos(3, 18, 3)), 0, 0);
        player.startRiding(nautilus);
        player.setXRot(-90);
        player.zza = 1;
        helper.assertTrue(nautilus.getControllingPassenger() == player && !nautilus.isInWater(),
                "The fixture must exercise an actively controlled Nautilus in air");
        double initialY = nautilus.getY();
        for (int tick = 0; tick < 12; tick++) nautilus.travel(Vec3.ZERO);
        helper.assertTrue(nautilus.getY() < initialY - 1 && nautilus.getDeltaMovement().y < 0,
                "Holding forward and looking up must not create lift or suspend a Nautilus outside water");
        player.stopRiding();
        nautilus.discard();
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void goblinKnightHasOneFullBodyAndCosmeticShield(GameTestHelper helper) {
        var level = helper.getLevel();
        var knight = TFEntities.UPPER_GOBLIN_KNIGHT.get().create(level);
        var player = helper.makeMockSurvivalPlayer();
        knight.moveTo(helper.absolutePos(new BlockPos(3, 2, 3)), 0, 0);
        helper.assertTrue(knight.getBbHeight() >= 2.6 && !knight.isPassenger() && !knight.isVehicle(),
                "The combined knight must have one full-height body and no lower goblin entity; height=" + knight.getBbHeight());
        knight.refreshDimensions();
        helper.assertTrue(knight.getBbHeight() >= 2.6 && knight.getEyeHeight() >= 2.2,
                "Dimension refreshes must preserve the combined body and its raised eye height");
        double armor = knight.getAttributeValue(Attributes.ARMOR), attack = knight.getAttributeValue(Attributes.ATTACK_DAMAGE);
        helper.assertTrue(armor == 8.0, "A full Goblin Knight must have moderate eight-point armor, not the native twenty-point boost");
        float damage = -1;
        for (int direction : new int[]{-1, 1}) {
            knight.setHealth(knight.getMaxHealth());
            knight.invulnerableTime = 0;
            player.moveTo(knight.getX(), knight.getY(), knight.getZ() + direction * 3);
            var source = level.damageSources().playerAttack(player);
            helper.assertFalse(knight.takeHitOnShield(source, 4), "The visible shield must never absorb a frontal hit");
            helper.assertTrue(knight.hurt(source, 4), "Attacks from both sides must reach the same health pool");
            float taken = knight.getMaxHealth() - knight.getHealth();
            if (damage >= 0) helper.assertTrue(Math.abs(taken - damage) < .001, "Front and rear armor must remain equal");
            damage = taken;
            helper.assertTrue(knight.hasArmor() && knight.hasShield() && !knight.isShieldDisabled()
                            && knight.getAttributeValue(Attributes.ARMOR) == armor
                            && knight.getAttributeValue(Attributes.ATTACK_DAMAGE) == attack,
                    "Attacks cannot strip equipment, disable the shield or alter combat attributes");
        }
        var drops = new LivingDropsEvent(knight, level.damageSources().generic(), new ArrayList<>(), 0, true);
        CustomMobDrops.addConfiguredDrops(drops);
        helper.assertTrue(drops.getDrops().size() == 2
                        && drops.getDrops().stream().anyMatch(d -> d.getItem().is(Items.BONE) && d.getItem().getCount() == 1)
                        && drops.getDrops().stream().anyMatch(d -> d.getItem().is(Items.IRON_INGOT) && d.getItem().getCount() == 2),
                "The combined knight keeps the AoE bone-and-iron reward");
        knight.discard();
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void snowyMouthsCarveDirtAndGrassButPreserveMud(GameTestHelper helper) throws Exception {
        var finish = SnowyCaveColumns.class.getDeclaredMethod("finishColumn", BlockState[].class, int.class,
                int.class, double.class, BlockState.class);
        finish.setAccessible(true);
        BlockState[] column = new BlockState[24];
        Arrays.fill(column, Blocks.AIR.defaultBlockState());
        for (int i = 0; i <= 14; i++) column[i] = Blocks.STONE.defaultBlockState();
        for (int i = 5; i <= 9; i++) column[i] = Blocks.CAVE_AIR.defaultBlockState();
        column[11] = Blocks.DIRT.defaultBlockState();
        column[12] = Blocks.GRASS_BLOCK.defaultBlockState();
        column[13] = column[14] = Blocks.AIR.defaultBlockState();
        var carved = (BlockState[]) finish.invoke(null, column, 70, 82, 100, Blocks.STONE.defaultBlockState());
        helper.assertTrue(carved[10].isAir() && carved[11].isAir() && carved[12].isAir(),
                "An existing cave mouth must reach the hillside through its last stone, dirt and grass blocks");
        column[11] = Blocks.MUD.defaultBlockState();
        var protectedColumn = (BlockState[]) finish.invoke(null, column, 70, 82, 100, Blocks.STONE.defaultBlockState());
        helper.assertTrue(protectedColumn[11].is(Blocks.MUD) && protectedColumn[12].is(Blocks.GRASS_BLOCK),
                "A mud-covered column remains sealed");
        helper.assertTrue(RetainedCharnia.CYAN.get() != RetainedCharnia.LIGHT_BLUE.get(),
                "Cyan and light blue Charnia must remain separate registered plants");
        helper.succeed();
    }
}
