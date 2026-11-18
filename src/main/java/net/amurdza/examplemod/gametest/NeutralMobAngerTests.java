package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.MobAI;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.warden.AngerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NeutralMobAngerTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void endermenRejectWardensBeforeEnablingAngryAnimation(GameTestHelper helper) {
        var level = helper.getLevel();
        var enderman = EntityType.ENDERMAN.create(level);
        var warden = EntityType.WARDEN.create(level);
        var player = helper.makeMockSurvivalPlayer();
        enderman.setTarget(player);
        helper.assertTrue(enderman.getTarget() == player && enderman.isCreepy(),
                "Endermen must still become angry at allowed player targets");
        enderman.setBeingStaredAt();
        enderman.setTarget(warden);
        helper.assertTrue(enderman.getTarget() == null && !enderman.isCreepy() && !enderman.hasBeenStaredAt(),
                "Rejecting a warden must reset the angry flags, not merely the base Mob target");

        var sounds = new SoundAudit(level);
        MinecraftForge.EVENT_BUS.register(sounds);
        try {
            enderman.playAmbientSound();
            helper.assertTrue(sounds.played.contains(SoundEvents.ENDERMAN_AMBIENT)
                            && !sounds.played.contains(SoundEvents.ENDERMAN_SCREAM),
                    "An enderman with a rejected target must use its calm ambient sound");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(sounds);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void wardensIgnoreEndermanVibrationsAndAngerBeforeMakingSounds(GameTestHelper helper) {
        var level = helper.getLevel();
        var warden = EntityType.WARDEN.create(level);
        var enderman = EntityType.ENDERMAN.create(level);
        BlockPos position = helper.absolutePos(new BlockPos(2, 2, 2));
        warden.moveTo(position.getX(), position.getY(), position.getZ());
        enderman.moveTo(position.getX() + 1, position.getY(), position.getZ());
        helper.assertFalse(warden.canTargetEntity(enderman), "Endermen must not become warden anger suspects");
        helper.assertFalse(warden.getVibrationUser().canReceiveVibration(level, enderman.blockPosition(),
                        GameEvent.STEP, GameEvent.Context.of(enderman)),
                "Enderman steps must not trigger warden listening sounds or tendril animations");

        var sounds = new SoundAudit(level);
        MinecraftForge.EVENT_BUS.register(sounds);
        try {
            warden.increaseAngerAt(enderman, 150, true);
            warden.setAttackTarget(enderman);
            helper.assertTrue(warden.getAngerLevel() == AngerLevel.CALM && warden.getTarget() == null
                            && warden.getEntityAngryAt().isEmpty() && sounds.played.isEmpty(),
                    "Rejected mobs must cause no anger, attack target, or listening sound");

            var player = helper.makeMockSurvivalPlayer();
            player.moveTo(position.getX() + 2, position.getY(), position.getZ());
            helper.assertTrue(warden.canTargetEntity(player), "Wardens must retain allowed player aggression");
            helper.assertTrue(warden.getVibrationUser().canReceiveVibration(level, player.blockPosition(),
                            GameEvent.STEP, GameEvent.Context.of(player)),
                    "Player footsteps must still be audible to wardens");
            warden.increaseAngerAt(player, 150, true);
            helper.assertTrue(warden.getAngerLevel() == AngerLevel.ANGRY && !sounds.played.isEmpty(),
                    "Allowed player anger must still produce the ordinary angry sound");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(sounds);
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty")
    public static void staleWardenRoarTargetsAreCleared(GameTestHelper helper) {
        var warden = EntityType.WARDEN.create(helper.getLevel());
        var enderman = EntityType.ENDERMAN.create(helper.getLevel());
        warden.getBrain().setMemory(MemoryModuleType.ROAR_TARGET, enderman);
        MobAI.onLivingTick(new LivingEvent.LivingTickEvent(warden));
        helper.assertFalse(warden.getBrain().hasMemoryValue(MemoryModuleType.ROAR_TARGET),
                "Previously acquired neutral roar targets must not survive the target cleanup");
        helper.succeed();
    }

    public static final class SoundAudit {
        private final Level level;
        private final List<SoundEvent> played = new ArrayList<>();

        SoundAudit(Level level) { this.level = level; }

        @SubscribeEvent
        public void sound(PlayLevelSoundEvent event) {
            if (event.getLevel() == level && event.getSound() != null) played.add(event.getSound().value());
        }
    }
}
