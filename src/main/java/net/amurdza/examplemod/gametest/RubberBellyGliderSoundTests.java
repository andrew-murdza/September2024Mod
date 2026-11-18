package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.PlayLevelSoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import teamdraco.finsandstails.registry.FTEntities;
import teamdraco.finsandstails.registry.FTSounds;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RubberBellyGliderSoundTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void gliderCallsAndPuffingAreSilentWithoutMutingPufferfish(GameTestHelper helper) {
        var level = helper.getLevel();
        var glider = FTEntities.RUBBER_BELLY_GLIDER.get().create(level);
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        glider.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        CompoundTag saved = new CompoundTag();
        glider.saveWithoutId(saved);
        saved.putBoolean("Silent", false);
        glider.load(saved);
        helper.assertTrue(glider.isSilent(), "Previously saved audible gliders must also become silent");

        var sounds = new SoundAudit(level);
        MinecraftForge.EVENT_BUS.register(sounds);
        try {
            glider.playAmbientSound();
            glider.playSound(FTSounds.RUBBER_BELLY_GLIDER_HURT.get(), 1.0F, 1.0F);
            glider.playSound(FTSounds.RUBBER_BELLY_GLIDER_DEATH.get(), 1.0F, 1.0F);
            glider.playSound(SoundEvents.PUFFER_FISH_BLOW_UP, 1.0F, 1.0F);
            glider.playSound(SoundEvents.PUFFER_FISH_BLOW_OUT, 1.0F, 1.0F);
            helper.assertTrue(sounds.played.isEmpty(), "Glider vocalizations and puffing must emit no sounds");

            var pufferfish = EntityType.PUFFERFISH.create(level);
            pufferfish.moveTo(glider.getX(), glider.getY(), glider.getZ());
            pufferfish.playSound(SoundEvents.PUFFER_FISH_BLOW_UP, 1.0F, 1.0F);
            helper.assertTrue(sounds.played.contains(SoundEvents.PUFFER_FISH_BLOW_UP),
                    "The same puff sound must remain audible for ordinary pufferfish");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(sounds);
        }
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
