package src.game;

import java.util.Map;
import java.util.function.BiConsumer;

public class Equipment {
    public static record Weapon(String name, Die.OffenseDieType[] attackDice,
            SurgeOptions[] surgeOptions) {
    };

    public static enum SurgeOptions {
        DAMAGE1,
        DAMAGE2,
        RECOVER1,
        RECOVER2,
        ACCURACY1,
        ACCURACY2,
        STUN
    }

    // Attacker, Defender, Damage, Accuracy, Recover
    public static Map<SurgeOptions, BiConsumer<Personnel[], TotalAttackResult>> surgeEffects = Map.of(
            SurgeOptions.STUN, (Personnel[] combatants, TotalAttackResult totalResults) -> {
                combatants[1].setStunned(true);
            },
            SurgeOptions.DAMAGE1, (Personnel[] combatants, TotalAttackResult totalResults) -> {
                totalResults.addDamage(1);
            },
            SurgeOptions.DAMAGE2, (Personnel[] combatants, TotalAttackResult totalResults) -> {
                totalResults.addDamage(2);
            },
            SurgeOptions.RECOVER1,
            (Personnel[] combatants, TotalAttackResult totalResults) -> {
                totalResults.addRecovery(1);
            },
            SurgeOptions.RECOVER2, (
                    Personnel[] combatants, TotalAttackResult totalResults) -> {
                totalResults.addRecovery(2);
            },
            SurgeOptions.ACCURACY1,
            (Personnel[] combatants, TotalAttackResult totalResults) -> {
                totalResults.addAccuracy(1);
            },
            SurgeOptions.ACCURACY2,
            (Personnel[] combatants, TotalAttackResult totalResults) -> {
                totalResults.addAccuracy(2);
            });
}
