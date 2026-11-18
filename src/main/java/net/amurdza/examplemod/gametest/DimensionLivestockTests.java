package net.amurdza.examplemod.gametest;

import net.amurdza.examplemod.AOEMod;
import net.amurdza.examplemod.config.MobConfig;
import net.amurdza.examplemod.registry.ModEntities;
import net.amurdza.examplemod.util.GameplayDimensions;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DimensionLivestockTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void arthropleuraBreedsAsHerbivorousGrazingLivestock(GameTestHelper helper) {
        var tropical = helper.getLevel().getServer().getLevel(GameplayDimensions.TROPICAL);
        var parent = ModEntities.ARTHROPLEURA.get().create(tropical);
        var mate = ModEntities.ARTHROPLEURA.get().create(tropical);
        helper.assertTrue(parent != null && parent.getMaxHealth() == 24 && parent.getMobType() == MobType.ARTHROPOD,
                "Arthropleura must have registered arthropod attributes");
        helper.assertTrue(parent.isFood(new ItemStack(Items.OAK_LEAVES))
                        && parent.isFood(new ItemStack(Items.FERN))
                        && parent.isFood(new ItemStack(Items.BROWN_MUSHROOM))
                        && !parent.isFood(new ItemStack(Items.COD)),
                "Arthropleura must accept plant forage and reject meat");
        var baby = parent.getBreedOffspring(tropical, mate);
        helper.assertTrue(baby != null && baby.getType() == parent.getType(),
                "Breeding must create actual Arthropleura offspring");
        baby.setAge(-24000);
        baby.setHealth(2);
        baby.ate();
        helper.assertTrue(baby.getAge() > -24000 && baby.getHealth() > 2,
                "Grazing in its native tropical habitat must nourish and mature young Arthropleura; age="
                        + baby.getAge() + ", health=" + baby.getHealth());
        helper.assertTrue(MobConfig.mobDropEntries(parent).size() == 1, "Arthropleura must yield one kind of meat");
        helper.assertTrue(MobConfig.MOB_INFO_BY_MOB.get(parent.getType()).breeding().tropical() == 1.25F,
                "The meat reward must preserve the existing reproduction rate");
        helper.succeed();
    }
}
