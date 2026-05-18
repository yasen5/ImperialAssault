package game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import game.Die.DefenseDieResult;
import game.Die.DefenseRoll;
import game.Die.OffenseDieResult;
import game.Die.OffenseRoll;
import game.Personnel.Directions;
import net.structs.GameSessionConfig;
import net.structs.MatchSnapshot;
import net.structs.MissionOption;

class RulesTest {
    @Test
    void attackResolverAppliesShieldsAndDamage() {
        FixedPersonnel attacker = new FixedPersonnel(new Pos(3, 3),
                new OffenseRoll[] { new OffenseRoll(0, new OffenseDieResult(3, 0, 3)) },
                new DefenseRoll[0]);
        FixedPersonnel defender = new FixedPersonnel(new Pos(3, 4), new OffenseRoll[0],
                new DefenseRoll[] { new DefenseRoll(0, new DefenseDieResult(1, 0, false)) });

        AttackResolver.resolve(attacker, defender, null);

        assertEquals(8, defender.getHealth());
    }

    @Test
    void attackResolverStopsOnDodge() {
        FixedPersonnel attacker = new FixedPersonnel(new Pos(3, 3),
                new OffenseRoll[] { new OffenseRoll(0, new OffenseDieResult(9, 0, 9)) },
                new DefenseRoll[0]);
        FixedPersonnel defender = new FixedPersonnel(new Pos(3, 4), new OffenseRoll[0],
                new DefenseRoll[] { new DefenseRoll(0, new DefenseDieResult(0, 0, true)) });

        AttackResolver.resolve(attacker, defender, null);

        assertEquals(10, defender.getHealth());
    }

    @Test
    void heroFirstDefeatWoundsAndSecondDefeatDefeats() {
        Hero hero = new Gaarkhan(new Pos(1, 4));

        hero.dealDamage(50);

        assertTrue(hero.isWounded());
        assertFalse(hero.isDefeated());
        assertEquals(hero.getStartingHealth(), hero.getHealth());

        hero.dealDamage(50);

        assertTrue(hero.isDefeated());
    }

    @Test
    void recoverRemovesStrainBeforeHealingDamage() {
        Hero hero = new Gaarkhan(new Pos(1, 4));
        hero.setHealth(8);
        hero.setStrain(3);
        hero.addCondition(Condition.BLEEDING);

        hero.recover();

        assertEquals(0, hero.getStrain());
        assertEquals(9, hero.getHealth());
        assertFalse(hero.hasCondition(Condition.BLEEDING));
    }

    @Test
    void movementCannotEndInOccupiedSpace() {
        Game game = new Game(null, new GameSessionConfig(1), null, true);
        Hero gaarkhan = game.getHeroes().get(1);
        gaarkhan.setPos(new Pos(1, 4));
        game.getHeroes().get(0).setPos(new Pos(2, 4));

        assertFalse(MovementRules.canMoveOneSpace(gaarkhan, Directions.RIGHT, game));
    }

    @Test
    void missionSnapshotCarriesThreatRoundAndConditions() {
        Game game = new Game(null, new GameSessionConfig(1), MissionDefinition.forOption(MissionOption.MISSION_TWO),
                null, true);
        Hero hero = game.getHeroes().get(0);
        hero.dealDamage(50);
        hero.addCondition(Condition.BLEEDING);

        MatchSnapshot snapshot = game.createSnapshot();
        Game copy = new Game(null, new GameSessionConfig(1), MissionDefinition.forOption(MissionOption.MISSION_TWO),
                null, false);
        copy.loadSnapshot(snapshot);

        assertEquals(3, copy.getThreatLevel());
        assertEquals(1, copy.getRoundDial());
        assertEquals(7, copy.getRoundLimit());
        assertTrue(copy.getHeroes().get(0).isWounded());
        assertTrue(copy.getHeroes().get(0).hasCondition(Condition.BLEEDING));
    }

    @Test
    void deploymentGroupTracksReinforcementCapacityAndCost() {
        DeploymentGroup<StormTrooper> group = new DeploymentGroup<>(
                new Pos[] { new Pos(4, 11), new Pos(4, 12), new Pos(5, 11) },
                StormTrooper::new, "StormTrooper");
        group.setDeploymentCost(6);
        group.removeDeadFigures();
        group.getMembers().get(0).dealDamage(10);
        group.removeDeadFigures();

        assertTrue(group.canReinforce(2));
        assertEquals(2, group.getReinforcementCost());

        group.reinforceMember(new Pos(8, 9));

        assertEquals(3, group.getMembers().size());
        assertFalse(group.canReinforce(2));
    }

    @Test
    void stunnedCanBeClearedByAction() {
        Game game = new Game(null, new GameSessionConfig(1), null, true);
        Hero hero = game.getHeroes().get(0);
        hero.setStunned(true);

        game.takeAction(hero, Personnel.Actions.DISCARD_CONDITION);

        assertFalse(hero.stunned());
    }

    private static final class FixedPersonnel extends Personnel {
        private final OffenseRoll[] offenseRolls;
        private final DefenseRoll[] defenseRolls;

        private FixedPersonnel(Pos pos, OffenseRoll[] offenseRolls, DefenseRoll[] defenseRolls) {
            super("StormTrooper", 10, 4, pos, new Die.DefenseDieType[0], false, false);
            this.offenseRolls = offenseRolls;
            this.defenseRolls = defenseRolls;
        }

        @Override
        public OffenseRoll[] getOffense() {
            return offenseRolls;
        }

        @Override
        public DefenseRoll[] getDefense() {
            return defenseRolls;
        }

        @Override
        public Equipment.SurgeOptions[] getSurgeOptions() {
            return new Equipment.SurgeOptions[0];
        }
    }
}
