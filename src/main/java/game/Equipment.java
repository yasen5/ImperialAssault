package game;

import util.MyHashMap;

import java.util.function.BiConsumer;

public class Equipment {
    public static record Weapon(String name, Die.OffenseDieType[] attackDice,
            SurgeOptions[] surgeOptions, boolean melee, boolean reach) {
    };

    // All the surge options a weapon can have in the tutorial
    public static enum SurgeOptions {
        DAMAGE1,
        DAMAGE2,
        RECOVER1,
        RECOVER2,
        ACCURACY1,
        ACCURACY2,
        PIERCE1,
        PIERCE2,
        STUN,
        FOCUS
    }

    // Param order: Attacker, Defender, Damage, Accuracy, Recover
    // Maps the surge option to a lambda that can run
    public static MyHashMap<SurgeOptions, BiConsumer<Personnel[], TotalAttackResult>> surgeEffects = new MyHashMap<>();

    static {
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
}
