package net.amurdza.examplemod.gametest;

import io.netty.buffer.Unpooled;
import net.amurdza.examplemod.AOEMod;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundRecipePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.RecipeBookSettings;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Arrays;
import java.util.List;

@GameTestHolder(AOEMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RecipeBookCompatibilityTests {
    @GameTest(template = "plant_gameplay_empty")
    public static void recipeBookTypesExistBeforePlayerPackets(GameTestHelper helper) {
        // Do not access any upstream type fields or construct their menus before this
        // assertion: that would initialize the missing server types and hide the bug.
        RecipeBookType[] types = RecipeBookType.values();
        List<String> actualNames = Arrays.stream(types).map(Enum::name).toList();
        List<String> expectedNames = List.of("CRAFTING", "FURNACE", "BLAST_FURNACE", "SMOKER",
                "COOKING", "HEARTHANDHARVEST_AGING", "HEARTHANDHARVEST_FERMENTING");
        helper.assertTrue(actualNames.equals(expectedNames),
                "Recipe book types must already match the client before the first player: " + actualNames);

        RecipeBookSettings settings = new RecipeBookSettings();
        for (int i = 0; i < types.length; i++) {
            settings.setOpen(types[i], i % 2 == 0);
            settings.setFiltering(types[i], i % 3 == 0);
        }
        List<ResourceLocation> recipes = List.of(new ResourceLocation("minecraft:oak_planks"),
                new ResourceLocation("aoemod:bone_shears"));
        List<ResourceLocation> highlights = List.of(recipes.get(1));
        for (ClientboundRecipePacket.State state : ClientboundRecipePacket.State.values()) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                new ClientboundRecipePacket(state, recipes, highlights, settings).write(buffer);
                ClientboundRecipePacket decoded = new ClientboundRecipePacket(buffer);
                helper.assertTrue(decoded.getState() == state && decoded.getRecipes().equals(recipes),
                        "Recipe IDs must remain aligned after all recipe-book settings for " + state);
                helper.assertTrue(decoded.getBookSettings().equals(settings),
                        "Every recipe-book open/filter pair must survive synchronization for " + state);
                helper.assertTrue(decoded.getHighlights().equals(
                                state == ClientboundRecipePacket.State.INIT ? highlights : List.of()),
                        "Only the initial recipe packet must contain its highlight list");
                helper.assertTrue(buffer.readableBytes() == 0, "Recipe packet must be consumed exactly");
            } finally {
                buffer.release();
            }
        }
        helper.succeed();
    }
}
