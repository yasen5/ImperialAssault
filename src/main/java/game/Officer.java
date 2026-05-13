package game;

import util.MyArrayList;


import game.Die.DefenseDieType;
import game.Die.DefenseRoll;
import game.Die.OffenseDieType;

public class Officer extends Imperial {
    public Officer(Pos pos) {
        super("ImperialOfficer", 3, 4, pos,
                ImperialType.OFFICER, new DefenseDieType[] { DefenseDieType.WHITE },
                new OffenseDieType[] { OffenseDieType.BLUE,
                        OffenseDieType.YELLOW },
                true, true);
    }

    @Override
    public Equipment.SurgeOptions[] getSurgeOptions() {
        return new Equipment.SurgeOptions[] { Equipment.SurgeOptions.FOCUS, Equipment.SurgeOptions.ACCURACY1,
                Equipment.SurgeOptions.DAMAGE1 };
    }

    // Give another friendly figure two moves
    @Override
    public void performSpecial(Personnel selected) {
        game.handleMoves(selected, 2);
    }

    // Find all imperial figures within two spaces
    @Override
    public MyArrayList<Personnel> getSpecialTargets() {
        MyArrayList<Personnel> targets = new MyArrayList<>();
        Pos thisPos = getPos();
        for (DeploymentGroup<? extends Imperial> group : game.getDeploymentGroups()) {
            for (Imperial imperial : group.getMembers()) {
                if (imperial.equals(this)) {
                    continue;
                }
                if (Pathfinder.canReachPoint(thisPos, imperial.getPos(), 2, false, false, game)) {
                    targets.add(imperial);
                }
            }
        }
        return targets;
    }

    // You get to reroll defence if next to a friendly figure
    @Override
    public DefenseRoll[] getDefense() {
        DefenseRoll[] results = new DefenseRoll[defenseDice.length];
        for (int i = 0; i < defenseDice.length; i++) {
            results[0] = defenseDice[i].roll(game);
        }
        if (game != null) {
            game.repaint();
        }
        if (game != null ? game.promptYesNo(getOwnerSeat(), "Ability", "Reroll defense?")
                : InputUtils.getYesNo("Ability", "Reroll defense?")) {
            results = super.getDefense();
        }
        return results;
    }
}
