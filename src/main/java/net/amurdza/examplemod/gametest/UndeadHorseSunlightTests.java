package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.event_handlers.NoTropicalFire;
import net.amurdza.examplemod.event_handlers.UndeadHorseSunlight;
import net.amurdza.examplemod.registry.ModEntities;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class UndeadHorseSunlightTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void horsemenBurnButCamelHusksKeepTropicalProtection(GameTestHelper helper) {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        for (var type : java.util.List.of(EntityType.SKELETON_HORSE, EntityType.ZOMBIE_HORSE,
                ModEntities.SKELETON_HORSEMAN.get(), ModEntities.ZOMBIE_HORSEMAN.get())) {
            LivingEntity horse = (LivingEntity) type.create(tropical);
            helper.assertTrue(UndeadHorseSunlight.applies(horse), "Ordinary horses and horseman mounts must share sunlight policy");
            horse.setSecondsOnFire(8);
            NoTropicalFire.onLivingTick(new LivingEvent.LivingTickEvent(horse));
            helper.assertTrue(horse.isOnFire(), "Tropical fire suppression must not erase undead-horse burning");
            var damage = new LivingAttackEvent(horse, horse.damageSources().onFire(), 1);
            NoTropicalFire.onLivingAttack(damage);
            helper.assertFalse(damage.isCanceled(), "Undead-horse sunlight fire must deal actual damage");
            horse.discard();
        }
        for (var type : java.util.List.of(EntityType.HORSE, EntityType.CAMEL, ModEntities.CAMEL_HUSK.get(),
                ModEntities.CAMEL_HUSK_JOCKEY.get())) {
            LivingEntity camel = (LivingEntity) type.create(tropical);
            helper.assertFalse(UndeadHorseSunlight.applies(camel), "Living mounts and camel husks are excluded");
            camel.setSecondsOnFire(8);
            NoTropicalFire.onLivingTick(new LivingEvent.LivingTickEvent(camel));
            helper.assertFalse(camel.isOnFire(), "Excluded mounts retain tropical fire protection");
            camel.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "plant_gameplay_empty", timeoutTicks = 100)
    public static void actualDaylightIgnitesExposedHorsesButARoofPreventsNewIgnition(GameTestHelper helper) {
        var level = helper.getLevel();
        long time = level.getDayTime();
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        BlockPos feet = new BlockPos(helper.absolutePos(BlockPos.ZERO).getX(), level.getMaxBuildHeight() - 12,
                helper.absolutePos(BlockPos.ZERO).getZ());
        var horse = EntityType.SKELETON_HORSE.create(level);
        horse.setPos(feet.getX() + .5, feet.getY(), feet.getZ() + .5);
        BlockPos eyes = BlockPos.containing(horse.getX(), horse.getEyeY(), horse.getZ());
        BlockPos roof = eyes.above();
        var previous = level.getBlockState(roof);
        helper.runAfterDelay(5, () -> {
            try {
                helper.assertTrue(UndeadHorseSunlight.exposed(horse), "Open midday sky must expose the undead horse");
                UndeadHorseSunlight.tick(new LivingEvent.LivingTickEvent(horse));
                helper.assertTrue(horse.isOnFire(), "The real sunlight event must ignite the horse");
                helper.assertTrue(level.setBlockAndUpdate(roof, Blocks.STONE.defaultBlockState()),
                        "The roof fixture must actually place a solid block");
            } catch (RuntimeException | Error failure) {
                level.setBlockAndUpdate(roof, previous);
                horse.discard();
                level.setDayTime(time);
                throw failure;
            }
            // canSeeSky reads propagated skylight, which is updated asynchronously.
            // The block placement itself must not be mistaken for a completed light update.
            helper.runAfterDelay(10, () -> {
                try {
                    helper.assertFalse(level.canSeeSky(eyes), "The placed roof must have blocked the actual sky light");
                    horse.clearFire();
                    UndeadHorseSunlight.tick(new LivingEvent.LivingTickEvent(horse));
                    helper.assertFalse(horse.isOnFire(), "A solid roof must prevent new sunlight ignition");
                    helper.succeed();
                } finally {
                    level.setBlockAndUpdate(roof, previous);
                    horse.discard();
                    level.setDayTime(time);
                }
            });
        });
    }
}
