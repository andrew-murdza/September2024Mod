package net.amurdza.examplemod.mixins.monoxide;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenu20Lapis {
  private static final int[] AOE_LAPIS_COSTS = {4, 8, 32};
  private static final int AOE_MAX_BOOKSHELVES_FOR_THIRD_SLOT = 45;
  private static final int AOE_MAX_SEPARATE_ENCHANTMENTS = 5;

  @Final
  @Shadow
  private Container enchantSlots;

  @Final
  @Shadow
  private ContainerLevelAccess access;

  @Final
  @Shadow
  private DataSlot enchantmentSeed;

  @Final
  @Shadow
  private RandomSource random;

  @Final
  @Shadow
  public int[] costs;

  @Final
  @Shadow
  public int[] enchantClue;

  @Final
  @Shadow
  public int[] levelClue;

  @Inject(method = "slotsChanged", at = @At("HEAD"), cancellable = true)
  private void aoemod$rebuildOffers(Container container, CallbackInfo ci) {
    if (container != this.enchantSlots) {
      return;
    }

    ci.cancel();

    ItemStack stack = this.enchantSlots.getItem(0);
    if (!aoemod$canShowTableOffers(stack)) {
      aoemod$clearOffers();
      ((EnchantmentMenu)(Object)this).broadcastChanges();
      return;
    }

    this.access.execute((level, tablePos) -> {
      int shelves = aoemod$countNearbyBookshelves(level, tablePos);
      int vanillaShelfPower = Math.min(15, shelves / 3);

      this.random.setSeed(this.enchantmentSeed.get());
      for (int slot = 0; slot < this.costs.length; slot++) {
        this.costs[slot] = 0;
        this.enchantClue[slot] = -1;
        this.levelClue[slot] = -1;

        int cost = EnchantmentHelper.getEnchantmentCost(this.random, slot, vanillaShelfPower, stack);
        if (slot == 2) {
          cost = Math.max(cost, Math.min(AOE_MAX_BOOKSHELVES_FOR_THIRD_SLOT, shelves));
        }
        cost = Math.max(cost, slot + 1);

        EnchantmentInstance offer = aoemod$selectOffer(stack, slot, cost);
        if (offer == null) {
          continue;
        }

        this.costs[slot] = cost;
        this.enchantClue[slot] = BuiltInRegistries.ENCHANTMENT.getId(offer.enchantment);
        this.levelClue[slot] = offer.level;
      }

      ((EnchantmentMenu)(Object)this).broadcastChanges();
    });
  }

  private void aoemod$clearOffers() {
    for (int slot = 0; slot < this.costs.length; slot++) {
      this.costs[slot] = 0;
      this.enchantClue[slot] = -1;
      this.levelClue[slot] = -1;
    }
  }

  @Inject(
    method = "clickMenuButton(Lnet/minecraft/world/entity/player/Player;I)Z",
    at = @At("HEAD"),
    cancellable = true
  )
  private void clickMenuButton(final Player player, final int id, final CallbackInfoReturnable<Boolean> cir) {
    if (id < 0 || id >= this.costs.length || id >= AOE_LAPIS_COSTS.length) {
      cir.setReturnValue(false);
      return;
    }

    ItemStack itemToEnchant = this.enchantSlots.getItem(0);
    ItemStack lapis = this.enchantSlots.getItem(1);
    int lapisCost = AOE_LAPIS_COSTS[id];

    if (this.costs[id] <= 0 || itemToEnchant.isEmpty()) {
      cir.setReturnValue(false);
      return;
    }

    if ((lapis.isEmpty() || lapis.getCount() < lapisCost) && !player.getAbilities().instabuild) {
      cir.setReturnValue(false);
      return;
    }

    this.access.execute((level, pos) -> {
      EnchantmentInstance enchantment = aoemod$selectOffer(itemToEnchant, id, this.costs[id]);
      if (enchantment == null) {
        return;
      }

      player.onEnchantmentPerformed(itemToEnchant, lapisCost);

      boolean wasBook = itemToEnchant.is(Items.BOOK);
      ItemStack resultStack = itemToEnchant;
      if (wasBook) {
        resultStack = new ItemStack(Items.ENCHANTED_BOOK);
        CompoundTag tag = itemToEnchant.getTag();
        if (tag != null) {
          resultStack.setTag(tag.copy());
        }
        this.enchantSlots.setItem(0, resultStack);
      }

      aoemod$applySingleEnchantment(resultStack, enchantment, wasBook);

      if (!player.getAbilities().instabuild) {
        lapis.shrink(lapisCost);
        if (lapis.isEmpty()) {
          this.enchantSlots.setItem(1, ItemStack.EMPTY);
        }
      }

      player.awardStat(Stats.ENCHANT_ITEM);
      if (player instanceof ServerPlayer serverPlayer) {
        CriteriaTriggers.ENCHANTED_ITEM.trigger(serverPlayer, resultStack, lapisCost);
      }

      this.enchantSlots.setChanged();
      this.enchantmentSeed.set(player.getEnchantmentSeed());
      ((EnchantmentMenu)(Object)this).slotsChanged(this.enchantSlots);
      level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F,
              level.random.nextFloat() * 0.1F + 0.9F);
    });

    cir.setReturnValue(true);
  }

  private static boolean aoemod$canShowTableOffers(ItemStack stack) {
    return !stack.isEmpty()
            && (stack.is(Items.BOOK)
            || stack.is(Items.ENCHANTED_BOOK)
            || stack.isEnchantable()
            || stack.getEnchantmentValue() > 0);
  }

  private static int aoemod$countNearbyBookshelves(Level level, BlockPos tablePos) {
    int shelves = 0;

    for (int dx = -3; dx <= 3; dx++) {
      for (int dy = 0; dy <= 2; dy++) {
        for (int dz = -3; dz <= 3; dz++) {
          BlockPos shelfPos = tablePos.offset(dx, dy, dz);
          if (shelfPos.equals(tablePos)) {
            continue;
          }

          BlockState state = level.getBlockState(shelfPos);
          if (state.is(Blocks.BOOKSHELF) || state.is(Blocks.CHISELED_BOOKSHELF)) {
            shelves++;
          }
        }
      }
    }

    return shelves;
  }

  private EnchantmentInstance aoemod$selectOffer(ItemStack stack, int slot, int cost) {
    List<EnchantmentInstance> offers = aoemod$getAvailableOffers(stack, cost);
    aoemod$shuffleOffers(offers, RandomSource.create(aoemod$offerSeed(slot, cost)));
    return aoemod$selectSingleOffer(stack, offers);
  }

  private long aoemod$offerSeed(int slot, int cost) {
    long seed = this.enchantmentSeed.get();
    seed ^= (long) slot * 341873128712L;
    seed ^= (long) cost * 132897987541L;
    return seed;
  }

  private static List<EnchantmentInstance> aoemod$getAvailableOffers(ItemStack stack, int cost) {
    List<EnchantmentInstance> offers = new ArrayList<>();

    for (Enchantment enchantment : BuiltInRegistries.ENCHANTMENT) {
      if (!aoemod$isAvailableForTable(enchantment, stack)) {
        continue;
      }

      int level = aoemod$getOfferLevel(enchantment, cost);
      if (level <= 0) {
        continue;
      }

      offers.add(new EnchantmentInstance(enchantment, level));
    }

    return offers;
  }

  private static boolean aoemod$isAvailableForTable(Enchantment enchantment, ItemStack stack) {
    if (enchantment.isCurse()) {
      return false;
    }

    if (aoemod$isDiversityBundleEnchantment(enchantment)) {
      return stack.getItem() instanceof BundleItem;
    }

    if (stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK)) {
      return enchantment.isAllowedOnBooks();
    }

    return enchantment.canEnchant(stack)
            || enchantment.canApplyAtEnchantingTable(stack)
            || aoemod$isAxeCombatEnchantment(enchantment, stack);
  }

  private static boolean aoemod$isDiversityBundleEnchantment(Enchantment enchantment) {
    ResourceLocation key = BuiltInRegistries.ENCHANTMENT.getKey(enchantment);
    if (key == null || !"diversity".equals(key.getNamespace())) {
      return false;
    }

    return switch (key.getPath()) {
      case "vacuum", "selective_vacuum", "capacity", "refill" -> true;
      default -> false;
    };
  }

  private static boolean aoemod$isAxeCombatEnchantment(Enchantment enchantment, ItemStack stack) {
    return stack.getItem() instanceof AxeItem
            && (enchantment == Enchantments.SHARPNESS
            || enchantment == Enchantments.SMITE
            || enchantment == Enchantments.BANE_OF_ARTHROPODS);
  }

  private static int aoemod$getOfferLevel(Enchantment enchantment, int cost) {
    if (cost >= AOE_MAX_BOOKSHELVES_FOR_THIRD_SLOT) {
      return enchantment.getMaxLevel();
    }

    int selectedLevel = 0;
    for (int level = enchantment.getMinLevel(); level <= enchantment.getMaxLevel(); level++) {
      if (cost >= enchantment.getMinCost(level)) {
        selectedLevel = level;
      }
    }

    return selectedLevel;
  }

  private static void aoemod$shuffleOffers(List<EnchantmentInstance> offers, RandomSource random) {
    Collections.shuffle(offers, new java.util.Random(random.nextLong()));
  }

  private static EnchantmentInstance aoemod$selectSingleOffer(ItemStack stack, List<EnchantmentInstance> offers) {
    for (EnchantmentInstance offer : offers) {
      if (aoemod$canApplyOffer(stack, offer)) {
        return offer;
      }
    }

    return null;
  }

  private static boolean aoemod$canApplyOffer(ItemStack stack, EnchantmentInstance offer) {
    if (stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK)) {
      return true;
    }

    Map<Enchantment, Integer> existingEnchantments = EnchantmentHelper.getEnchantments(stack);
    int currentLevel = existingEnchantments.getOrDefault(offer.enchantment, 0);
    int offeredLevel = Math.min(offer.level, offer.enchantment.getMaxLevel());

    if (currentLevel > 0) {
      return currentLevel < offeredLevel;
    }

    if (existingEnchantments.size() >= AOE_MAX_SEPARATE_ENCHANTMENTS) {
      return false;
    }

    for (Enchantment existing : existingEnchantments.keySet()) {
      if (!existing.isCompatibleWith(offer.enchantment) || !offer.enchantment.isCompatibleWith(existing)) {
        return false;
      }
    }

    return true;
  }

  private static void aoemod$applySingleEnchantment(ItemStack stack, EnchantmentInstance offer, boolean wasBook) {
    int level = Math.min(offer.level, offer.enchantment.getMaxLevel());

    if (wasBook || stack.is(Items.ENCHANTED_BOOK)) {
      EnchantedBookItem.addEnchantment(stack, new EnchantmentInstance(offer.enchantment, level));
      return;
    }

    Map<Enchantment, Integer> enchantments = new LinkedHashMap<>(EnchantmentHelper.getEnchantments(stack));
    int currentLevel = enchantments.getOrDefault(offer.enchantment, 0);
    enchantments.put(offer.enchantment, Math.max(currentLevel, level));
    EnchantmentHelper.setEnchantments(enchantments, stack);
  }
}
