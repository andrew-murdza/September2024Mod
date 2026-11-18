package net.amurdza.examplemod.gametest;

import com.mojang.authlib.GameProfile;
import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.registry.ModMobCategories;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.amurdza.examplemod.worldgen.AquaticSpawnDistanceRules;
import net.amurdza.examplemod.worldgen.DocumentedFauna;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TropicalRoundTwoTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void commonAquaticRosterSharesFishCapacityRangeAndSizeDependentSchools(GameTestHelper helper) {
        var level = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var player = new FakePlayer(level, new GameProfile(UUID.randomUUID(), "CommonAquatic"));
        player.setPos(280000, 200, 280000);
        level.players().add(player);
        try {
            int common = 0, rare = 0;
            for (var species : DocumentedFauna.forBiome("coral_reef")) {
                var category = DocumentedFauna.spawnCategory(species);
                Mob mob = (Mob) species.type().create(level);
                try {
                    if (category == ModMobCategories.COMMON_AQUATIC) {
                        common++;
                        helper.assertTrue(DocumentedFauna.spawnWeight(species) == 10, "Common animals must have equal selection weight: " + species.id());
                        helper.assertTrue(AquaticSpawnDistanceRules.category(level, species.type()).getDespawnDistance() == 64,
                                "Common animals share the actual 64-block spawn/despawn distance");
                        mob.setPos(player.getX() + 64, player.getY(), player.getZ());
                        mob.checkDespawn();
                        helper.assertFalse(mob.isRemoved(), "Boundary resident disappeared: " + species.id());
                        mob.setPos(player.getX() + 65, player.getY(), player.getZ());
                        mob.checkDespawn();
                        helper.assertTrue(mob.isRemoved(), "Native species bypasses common fish despawning: " + species.id());
                        Mob kept = (Mob) species.type().create(level);
                        kept.setCustomName(Component.literal("Kept aquatic"));
                        kept.setPos(player.getX() + 200, player.getY(), player.getZ());
                        kept.checkDespawn();
                        helper.assertFalse(kept.isRemoved(), "Named animals remain protected: " + species.id());
                        kept.discard();
                    } else {
                        rare++;
                        helper.assertTrue(category == MobCategory.WATER_CREATURE && category.getMaxInstancesPerChunk() == 5,
                                "Dolphins, axolotls, turtles and nautili retain their rare budget");
                    }
                } finally { mob.discard(); }
            }
            helper.assertTrue(common == 8 && rare == 4 && ModMobCategories.COMMON_AQUATIC.getMaxInstancesPerChunk() == 40,
                    "The eight common species share exactly twice the original fish capacity");
            var tropical = DocumentedFauna.forBiome("coral_reef").stream().filter(s -> s.type() == EntityType.TROPICAL_FISH).findFirst().orElseThrow();
            var octopus = DocumentedFauna.forBiome("plains_lake").stream().filter(s -> s.id().toString().equals("hybrid_aquatic:octopus")).findFirst().orElseThrow();
            helper.assertTrue(DocumentedFauna.groupSize(tropical).minimum > DocumentedFauna.groupSize(octopus).maximum,
                    "Tiny tropical fish must form strictly larger schools than large lake octopuses");
        } finally { level.players().remove(player); player.discard(); }
        helper.succeed();
    }

}
