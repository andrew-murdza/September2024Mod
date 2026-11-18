package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RunAroundLikeCrazyGoal;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class HorseCharacteristicsTests {
    private static final String TEMPLATE = "plant_gameplay_empty";
    private static final BlockPos SPAWN_POS = new BlockPos(8, 250, 8);

    private HorseCharacteristicsTests() {}

    @GameTest(template = TEMPLATE)
    public static void savannaHorsesSpawnWithFullConfiguredHealthAndFedTemper(GameTestHelper helper) {
        ServerLevel savanna = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        for (MobSpawnType reason : new MobSpawnType[]{MobSpawnType.NATURAL, MobSpawnType.SPAWN_EGG, MobSpawnType.COMMAND}) {
            Horse horse = EntityType.HORSE.spawn(savanna, SPAWN_POS, reason);
            helper.assertTrue(horse != null, "Horse must spawn for " + reason);
            try {
                helper.assertTrue(horse.getMaxHealth() == 60.0F && horse.getHealth() == 60.0F,
                        reason + " horse must start with all 60 configured HP, got " + horse.getHealth() + "/" + horse.getMaxHealth());
                helper.assertTrue(horse.getTemper() == horse.getMaxTemper() && !horse.isTamed(),
                        "Savanna horses must have fully fed temper while remaining wild");
            } finally {
                horse.discard();
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void savedHorseHealthIsRepairedOnceWithoutErasingDamage(GameTestHelper helper) {
        ServerLevel savanna = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        Horse legacy = EntityType.HORSE.create(savanna);
        legacy.moveTo(SPAWN_POS, 0.0F, 0.0F);
        legacy.addTag("aoe.checkedCharacteristics");
        legacy.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.4F);
        legacy.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(1.1F);
        legacy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(26.0D);
        legacy.setHealth(21.0F);
        legacy.setTemper(0);
        Horse restored = reload(legacy, savanna);
        try {
            helper.assertTrue(restored.getMaxHealth() == 60.0F && restored.getHealth() == 55.0F,
                    "An old horse must receive 60 max HP and retain its five points of damage");
            helper.assertTrue(restored.getTemper() == restored.getMaxTemper(), "Existing savanna horses must receive fed temper");
            restored.setHealth(42.0F);
            restored = reload(restored, savanna);
            helper.assertTrue(restored.getHealth() == 42.0F && restored.getMaxHealth() == 60.0F,
                    "Reloading a repaired horse must not heal it again");
            ServerLevel plains = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
            restored = reload(restored, plains);
            helper.assertTrue(restored.getHealth() == 42.0F && restored.getMaxHealth() == 60.0F,
                    "A repaired horse must keep its native health when moved to another dimension");
        } finally {
            restored.discard();
        }
        ServerLevel plains = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        Horse injured = EntityType.HORSE.create(plains);
        injured.moveTo(SPAWN_POS, 0.0F, 0.0F);
        injured.addTag("aoe.checkedCharacteristics");
        injured.getAttribute(Attributes.MAX_HEALTH).setBaseValue(26.0D);
        injured.setHealth(5.0F);
        Horse lowered = reload(injured, plains);
        try {
            helper.assertTrue(lowered.isAlive() && lowered.getMaxHealth() == 15.0F
                            && Math.abs(lowered.getHealth() / lowered.getMaxHealth() - 5.0F / 26.0F) < 0.0001F,
                    "Repairing to a lower regional cap must preserve the injured fraction and leave the horse alive");
        } finally {
            lowered.discard();
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void legacyHorsesKeepTheirNativeHealthAfterTravelling(GameTestHelper helper) {
        ServerLevel plains = helper.getLevel().getServer().getLevel(GameplayDimensions.PLAINS);
        for (double previousMax : new double[]{60.0D, 26.0D}) {
            Horse legacy = EntityType.HORSE.create(plains);
            legacy.moveTo(SPAWN_POS, 0.0F, 0.0F);
            legacy.addTag("aoe.checkedCharacteristics");
            legacy.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.4F);
            legacy.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(1.1F);
            legacy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(previousMax);
            legacy.setHealth((float) previousMax - 5.0F);
            Horse restored = reload(legacy, plains);
            try {
                helper.assertTrue(restored.getMaxHealth() == 60.0F && restored.getHealth() == 55.0F,
                        "A legacy savanna horse moved to plains must retain or recover its native max HP and saved damage");
            } finally {
                restored.discard();
            }
        }
        Horse modified = EntityType.HORSE.create(plains);
        modified.moveTo(SPAWN_POS, 0.0F, 0.0F);
        modified.addTag("aoe.checkedCharacteristics");
        modified.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35D);
        modified.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(0.9D);
        modified.getAttribute(Attributes.MAX_HEALTH).setBaseValue(60.0D);
        modified.setHealth(42.0F);
        Horse restored = reload(modified, plains);
        try {
            helper.assertTrue(restored.getMaxHealth() == 60.0F && restored.getHealth() == 42.0F,
                    "An existing configured max HP must survive migration when other traits have been customized");
        } finally {
            restored.discard();
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void fedSavannaHorseTamesOnItsFirstTamingRoll(GameTestHelper helper) {
        ServerLevel savanna = helper.getLevel().getServer().getLevel(GameplayDimensions.SAVANNA);
        Horse horse = EntityType.HORSE.spawn(savanna, SPAWN_POS, MobSpawnType.SPAWN_EGG);
        helper.assertTrue(horse != null, "Savanna horse must spawn");
        var player = helper.makeMockSurvivalPlayer();
        try {
            horse.setAge(0);
            helper.assertTrue(player.startRiding(horse, true), "Player must be able to mount the wild horse");
            RunAroundLikeCrazyGoal taming = (RunAroundLikeCrazyGoal) horse.goalSelector.getAvailableGoals().stream()
                    .map(goal -> goal.getGoal()).filter(RunAroundLikeCrazyGoal.class::isInstance).findFirst().orElseThrow();
            horse.getRandom().setSeed(0L);
            for (int tick = 0; tick < 512 && !horse.isTamed(); tick++) {
                taming.tick();
                helper.assertTrue(player.getVehicle() == horse, "Fully fed temper must prevent a failed taming roll from ejecting the player");
            }
            helper.assertTrue(horse.isTamed() && player.getUUID().equals(horse.getOwnerUUID()),
                    "The first taming roll must tame the savanna horse for its rider");
        } finally {
            player.stopRiding();
            horse.discard();
        }
        helper.succeed();
    }

    private static Horse reload(Horse horse, ServerLevel level) {
        CompoundTag saved = horse.saveWithoutId(new CompoundTag());
        horse.discard();
        Horse restored = EntityType.HORSE.create(level);
        restored.load(saved);
        level.addFreshEntity(restored);
        return restored;
    }
}
