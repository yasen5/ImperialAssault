package game;

import game.Die.DefenseDieResult;
import game.Die.DefenseRoll;
import game.Die.OffenseDieResult;
import game.Die.OffenseRoll;
import util.MyArrayList;

public final class AttackResolver {
    private AttackResolver() {
    }

    public static TotalAttackResult resolve(Personnel attacker, Personnel defender, Game game) {
        if (game != null) {
            game.clearDiceInternal();
        }
        int surges = 0;
        int rawDamage = 0;
        TotalAttackResult totalResults = new TotalAttackResult();
        for (DefenseRoll roll : attacker.getDefense(defender)) {
            DefenseDieResult result = roll.result();
            if (result.dodge()) {
                return totalResults;
            }
            totalResults.addDamage(-result.shields());
            surges -= result.surgeCancel();
        }
        for (OffenseRoll roll : attacker.getOffense()) {
            OffenseDieResult result = roll.result();
            rawDamage += result.damage();
            surges += result.surge();
            totalResults.addAccuracy(result.accuracy());
        }
        if (game != null) {
            game.repaint();
        }
        spendSurges(attacker, defender, game, Math.max(0, surges), totalResults);
        totalResults.addDamage(rawDamage);
        if (attackHasRange(attacker, defender, totalResults, game) && totalResults.getDamage() > 0) {
            defender.dealDamage(totalResults.getDamage());
        }
        if (totalResults.getRecovery() > 0) {
            attacker.dealDamage(-totalResults.getRecovery());
        }
        return totalResults;
    }

    private static void spendSurges(Personnel attacker, Personnel defender, Game game, int surges,
            TotalAttackResult totalResults) {
        MyArrayList<Equipment.SurgeOptions> surgeOptions = new MyArrayList<>();
        for (Equipment.SurgeOptions option : attacker.getSurgeOptions()) {
            surgeOptions.add(option);
        }
        while (surges > 0 && !surgeOptions.isEmpty()) {
            int selectedIndex = game != null
                    ? game.promptMultipleChoice(attacker.getOwnerSeat(), "Surge Selection", "Spend Surges: " + surges,
                            surgeOptions.toArray())
                    : InputUtils.getMultipleChoice("Surge Selection", "Spend Surges: " + surges,
                            surgeOptions.toArray());
            if (selectedIndex < 0 || selectedIndex >= surgeOptions.size()) {
                break;
            }
            Equipment.surgeEffects.get(surgeOptions.remove(selectedIndex)).accept(new Personnel[] { attacker, defender },
                    totalResults);
            surges--;
        }
    }

    private static boolean attackHasRange(Personnel attacker, Personnel defender, TotalAttackResult totalResults,
            Game game) {
        if (attacker.getRange() != Integer.MAX_VALUE) {
            return true;
        }
        return Pathfinder.canReachPoint(attacker.getPos(), defender.getPos(), totalResults.getAccuracy(), false, game);
    }
}
