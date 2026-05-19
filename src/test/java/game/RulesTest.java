package game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import game.Die.DefenseDieResult;
import game.Die.DefenseRoll;
import game.Die.OffenseDieResult;
import game.Die.OffenseRoll;
import game.Personnel.Directions;
import net.GameDecisionProvider;
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
    void diagonalMovementCanCrossWallCorners() {
        Game game = new Game(null, new GameSessionConfig(1), null, true);
        Hero hero = game.getHeroes().get(0);
        hero.setPos(new Pos(3, 4));

        assertFalse(hero.getPos().canMove(Directions.RIGHT, false, true, game));
        assertTrue(MovementRules.canMoveOneSpace(hero, Directions.DOWNRIGHT, game));
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
    void sessionConfigSupportsFourRebelSeats() {
        GameSessionConfig config = new GameSessionConfig(4);

        assertEquals(PlayerSeat.REBEL_1, config.rebelTurnOrder().get(0));
        assertEquals(PlayerSeat.REBEL_2, config.rebelTurnOrder().get(1));
        assertEquals(PlayerSeat.REBEL_3, config.rebelTurnOrder().get(2));
        assertEquals(PlayerSeat.REBEL_4, config.rebelTurnOrder().get(3));
        assertEquals(5, config.requiredSeats().size());
    }

    @Test
    void tutorialSetupUsesFullFourHeroRosterAndImperials() {
        Game game = new Game(null, new GameSessionConfig(4), MissionDefinition.forOption(MissionOption.MISSION_ONE),
                null, true);

        assertEquals(4, game.getHeroes().size());
        assertInstanceOf(DialaPassil.class, game.getHeroes().get(0));
        assertInstanceOf(Gaarkhan.class, game.getHeroes().get(1));
        assertInstanceOf(FennSignis.class, game.getHeroes().get(2));
        assertInstanceOf(MakEshray.class, game.getHeroes().get(3));
        assertEquals(PlayerSeat.REBEL_4, game.getHeroes().get(3).getOwnerSeat());
        assertTrue(hasGroup(game, "StormTrooper"));
        assertTrue(hasGroup(game, "ImperialOfficer"));
        assertTrue(hasGroup(game, "ProbeDroid"));
        assertTrue(hasGroup(game, "EWebEngineer"));
        assertEquals(0, game.getThreatLevel());
        assertEquals(0, game.getRoundLimit());
    }

    @Test
    void tutorialSetupScalesProbeDroidAndEWebByHeroCount() {
        Game twoHeroGame = new Game(null, new GameSessionConfig(2),
                MissionDefinition.forOption(MissionOption.MISSION_ONE), null, true);
        Game threeHeroGame = new Game(null, new GameSessionConfig(3),
                MissionDefinition.forOption(MissionOption.MISSION_ONE), null, true);

        assertFalse(hasGroup(twoHeroGame, "ProbeDroid"));
        assertFalse(hasGroup(twoHeroGame, "EWebEngineer"));
        assertTrue(hasGroup(threeHeroGame, "ProbeDroid"));
        assertFalse(hasGroup(threeHeroGame, "EWebEngineer"));
    }

    @Test
    void tutorialSnapshotRoundTripsNewFigures() {
        Game game = new Game(null, new GameSessionConfig(4), MissionDefinition.forOption(MissionOption.MISSION_ONE),
                null, true);

        MatchSnapshot snapshot = game.createSnapshot();
        Game copy = new Game(null, new GameSessionConfig(4), MissionDefinition.forOption(MissionOption.MISSION_ONE),
                null, false);
        copy.loadSnapshot(snapshot);

        assertInstanceOf(FennSignis.class, copy.getHeroes().get(2));
        assertInstanceOf(MakEshray.class, copy.getHeroes().get(3));
        assertTrue(hasGroup(copy, "ProbeDroid"));
        assertTrue(hasGroup(copy, "EWebEngineer"));
    }

    @Test
    void tutorialObjectivesEndOnHeroWoundOrBothTerminals() {
        Game heroWoundGame = new Game(null, new GameSessionConfig(2),
                MissionDefinition.forOption(MissionOption.MISSION_ONE), null, true);
        heroWoundGame.getHeroes().get(0).dealDamage(50);
        heroWoundGame.checkEndGame();

        assertTrue(heroWoundGame.isGameEnd());
        assertFalse(heroWoundGame.rebelsWin());

        Game terminalGame = new Game(null, new GameSessionConfig(2),
                MissionDefinition.forOption(MissionOption.MISSION_ONE), null, true);
        terminalGame.getInteractables()[0].applySnapshotState(false);
        terminalGame.getInteractables()[1].applySnapshotState(false);
        terminalGame.checkEndGame();

        assertTrue(terminalGame.isGameEnd());
        assertFalse(terminalGame.rebelsWin());
    }

    @Test
    void rebelSideVotesForNextActivationAndThenImperialsAct() throws Exception {
        VotingDecisionProvider decisionProvider = new VotingDecisionProvider(2, 2, 1, 2);
        Game game = new Game(null, new GameSessionConfig(4), MissionDefinition.forOption(MissionOption.MISSION_ONE),
                decisionProvider, true);
        decisionProvider.resetPromptCount();

        assertEquals(PlayerSeat.REBEL_3, invokeChooseNextActivationSeat(game));
        assertEquals(4, decisionProvider.multipleChoicePrompts);

        setCurrentTurnSeat(game, PlayerSeat.IMPERIAL);

        assertEquals(PlayerSeat.IMPERIAL, invokeChooseNextActivationSeat(game));
    }

    @Test
    void movementStopsWhenNoLegalDirectionExists() {
        CountingDecisionProvider decisionProvider = new CountingDecisionProvider();
        Game game = new Game(null, new GameSessionConfig(1), MissionDefinition.forOption(MissionOption.MISSION_ONE),
                decisionProvider, true);
        Hero hero = game.getHeroes().get(0);
        hero.setPos(new Pos(0, 0));

        game.handleMoves(hero, 1);

        assertEquals(0, decisionProvider.directionPrompts);
    }

    @Test
    void setupPromptsRebelHeroesInJoinOrder() {
        VotingDecisionProvider decisionProvider = new VotingDecisionProvider(2, 0, 0, 0);
        Game game = new Game(null, new GameSessionConfig(4), MissionDefinition.forOption(MissionOption.MISSION_ONE),
                decisionProvider, false);
        game.setRebelHeroSelectionOrder(util.MyArrayList.of(PlayerSeat.REBEL_3, PlayerSeat.REBEL_1,
                PlayerSeat.REBEL_4, PlayerSeat.REBEL_2));

        game.setup();

        assertInstanceOf(FennSignis.class, game.getHeroes().get(0));
        assertEquals(PlayerSeat.REBEL_3, game.getHeroes().get(0).getOwnerSeat());
        assertEquals(PlayerSeat.REBEL_1, game.getHeroes().get(1).getOwnerSeat());
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

    private boolean hasGroup(Game game, String name) {
        for (DeploymentGroup<? extends Imperial> group : game.getDeploymentGroups()) {
            if (group.toString().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private PlayerSeat invokeChooseNextActivationSeat(Game game) throws Exception {
        Method method = Game.class.getDeclaredMethod("chooseNextActivationSeat");
        method.setAccessible(true);
        return (PlayerSeat) method.invoke(game);
    }

    private void setCurrentTurnSeat(Game game, PlayerSeat seat) throws Exception {
        Field field = Game.class.getDeclaredField("currentTurnSeat");
        field.setAccessible(true);
        field.set(game, seat);
    }

    private static final class VotingDecisionProvider implements GameDecisionProvider {
        private final int[] votes;
        private int multipleChoicePrompts;

        private VotingDecisionProvider(int... votes) {
            this.votes = votes;
        }

        private void resetPromptCount() {
            multipleChoicePrompts = 0;
        }

        @Override
        public int chooseMultipleChoice(PlayerSeat seat, String name, String explanation, Object[] options) {
            return votes[multipleChoicePrompts++ % votes.length];
        }

        @Override
        public boolean chooseYesNo(PlayerSeat seat, String name, String explanation) {
            return false;
        }

        @Override
        public int chooseNumericChoice(PlayerSeat seat, String name, int minValue, int maxValue) {
            return minValue;
        }

        @Override
        public Directions chooseDirection(PlayerSeat seat, Personnel activeFigure, util.MyArrayList<Directions> allowedDirections) {
            return allowedDirections.get(0);
        }

        @Override
        public Personnel chooseTarget(PlayerSeat seat, SelectionType selectionType, util.MyArrayList<Personnel> availableTargets) {
            return availableTargets.get(0);
        }
    }

    private static final class CountingDecisionProvider implements GameDecisionProvider {
        private int directionPrompts;

        @Override
        public int chooseMultipleChoice(PlayerSeat seat, String name, String explanation, Object[] options) {
            return 0;
        }

        @Override
        public boolean chooseYesNo(PlayerSeat seat, String name, String explanation) {
            return false;
        }

        @Override
        public int chooseNumericChoice(PlayerSeat seat, String name, int minValue, int maxValue) {
            return minValue;
        }

        @Override
        public Directions chooseDirection(PlayerSeat seat, Personnel activeFigure, util.MyArrayList<Directions> allowedDirections) {
            directionPrompts++;
            return allowedDirections.get(0);
        }

        @Override
        public Personnel chooseTarget(PlayerSeat seat, SelectionType selectionType, util.MyArrayList<Personnel> availableTargets) {
            return availableTargets.get(0);
        }
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
