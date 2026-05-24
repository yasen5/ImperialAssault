package game;

import util.MyHashMap;
import util.MyArrayList;

import java.util.function.BiConsumer;

public class Equipment {
  public static record Item(String id, String name, String imageName, UseTiming useTiming, int recoverAmount,
      boolean grantsFocus, boolean consumable) {
    public Item(ItemId id, String imageName, UseTiming useTiming, int recoverAmount,
        boolean grantsFocus, boolean consumable) {
      this(id.name(), id.toString(), imageName, useTiming, recoverAmount, grantsFocus, consumable);
    }

    @Override
    public String toString() {
      return name;
    }
  };

  public static record Weapon(String name, Die.OffenseDieType[] attackDice,
      SurgeOptions[] surgeOptions, boolean melee, boolean reach, String imageName) {
  };

  public static enum ItemId {
    EMERGENCY_MEDPACK,
    ADRENAL_STIM;

    @Override
    public String toString() {
      String result = "";
      String[] words = name().split("_");
      for (int i = 0; i < words.length; i++) {
        if (i > 0) {
          result += " ";
        }
        result += words[i].substring(0, 1).toUpperCase() + words[i].substring(1).toLowerCase();
      }
      return result;
    }
  }

  private static final MyArrayList<Item> supplyItems = MyArrayList.of(
      new Item(ItemId.EMERGENCY_MEDPACK, "medkit", UseTiming.AFTER_RECOVER, 5, false, true),
      new Item(ItemId.ADRENAL_STIM, "Adrenal Stim", UseTiming.DURING_ACTIVATION, 3, true, true));

  private static final MyHashMap<String, Item> itemRegistry = new MyHashMap<>();

  public static enum UseTiming {
    PASSIVE,
    DURING_ACTIVATION,
    AFTER_RECOVER
  }

  // All the surge options a weapon can have in the tutorial
  public static enum SurgeOptions {
    DAMAGE1,
    DAMAGE2,
    RECOVER1,
    RECOVER2,
    ACCURACY1,
    ACCURACY2,
    ACCURACY3,
    PIERCE1,
    PIERCE2,
    STUN,
    FOCUS
  }

  // Param order: Attacker, Defender, Damage, Accuracy, Recover
  // Maps the surge option to a lambda that can run
  public static MyHashMap<SurgeOptions, BiConsumer<Personnel[], TotalAttackResult>> surgeEffects = new MyHashMap<>();

  static {
    for (Item item : supplyItems) {
      itemRegistry.put(item.id(), item);
    }
    surgeEffects.put(SurgeOptions.STUN, (Personnel[] combatants, TotalAttackResult totalResults) -> {
      combatants[1].setStunned(true);
    });
    surgeEffects.put(SurgeOptions.DAMAGE1, (Personnel[] combatants, TotalAttackResult totalResults) -> {
      totalResults.addDamage(1);
    });
    surgeEffects.put(SurgeOptions.DAMAGE2, (Personnel[] combatants, TotalAttackResult totalResults) -> {
      totalResults.addDamage(2);
    });
    surgeEffects.put(SurgeOptions.RECOVER1, (Personnel[] combatants, TotalAttackResult totalResults) -> {
      totalResults.addRecovery(1);
    });
    surgeEffects.put(SurgeOptions.RECOVER2, (Personnel[] combatants, TotalAttackResult totalResults) -> {
      totalResults.addRecovery(2);
    });
    surgeEffects.put(SurgeOptions.ACCURACY1, (Personnel[] combatants, TotalAttackResult totalResults) -> {
      totalResults.addAccuracy(1);
    });
    surgeEffects.put(SurgeOptions.ACCURACY2, (Personnel[] combatants, TotalAttackResult totalResults) -> {
      totalResults.addAccuracy(2);
    });
    surgeEffects.put(SurgeOptions.ACCURACY3, (Personnel[] combatants, TotalAttackResult totalResults) -> {
      totalResults.addAccuracy(3);
    });
    surgeEffects.put(SurgeOptions.PIERCE1, (Personnel[] combatants, TotalAttackResult totalResults) -> {
      if (totalResults.getDamage() < 0) {
        totalResults.addDamage(1);
      }
    });
    surgeEffects.put(SurgeOptions.PIERCE2, (Personnel[] combatants, TotalAttackResult totalResults) -> {
      for (int i = 0; i < 2; i++) {
        if (totalResults.getDamage() < 0) {
          totalResults.addDamage(1);
        }
      }
    });
    surgeEffects.put(SurgeOptions.FOCUS, (Personnel[] combatants, TotalAttackResult totalResults) -> {
      combatants[0].setFocused(true);
    });
  }

  public static Item asItem(Weapon weapon) {
    return new Item("weapon-" + normalizeId(weapon.name()), weapon.name(), weapon.imageName(), UseTiming.PASSIVE, 0,
        false, false);
  }

  public static MyArrayList<Item> getSupplyItems() {
    return new MyArrayList<>(supplyItems);
  }

  public static Item getSupplyItem(int index) {
    if (supplyItems.isEmpty()) {
      return null;
    }
    return supplyItems.get(index % supplyItems.size());
  }

  public static Item findItem(String id) {
    return itemRegistry.get(id);
  }

  private static String normalizeId(String text) {
    String result = "";
    for (int i = 0; i < text.length(); i++) {
      char ch = Character.toLowerCase(text.charAt(i));
      if (Character.isLetterOrDigit(ch)) {
        result += ch;
      } else if (!result.endsWith("-")) {
        result += "-";
      }
    }
    if (result.endsWith("-")) {
      result = result.substring(0, result.length() - 1);
    }
    return result.isEmpty() ? "item" : result;
  }
}
