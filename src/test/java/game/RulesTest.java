package game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

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
    void horizontalDoorExtendsRightFromSpecPosition() {
        Door<Personnel> door = new Door<>(new Pos(4, 3), Personnel.class, false);
        Constants.WallLine[] wallLines = door.getWallLines();

        assertEquals(2, wallLines.length);
        assertFalse(wallLines[0].vertical());
        assertFalse(wallLines[1].vertical());
        assertTrue(wallLines[0].pos().equalTo(new Pos(4, 3)));
        assertTrue(wallLines[1].pos().equalTo(new Pos(5, 3)));
    }

    @Test
    void verticalDoorExtendsDownFromSpecPosition() {
        Door<Personnel> door = new Door<>(new Pos(4, 3), Personnel.class, true);
        Constants.WallLine[] wallLines = door.getWallLines();

        assertEquals(2, wallLines.length);
        assertTrue(wallLines[0].vertical());
        assertTrue(wallLines[1].vertical());
        assertTrue(wallLines[0].pos().equalTo(new Pos(4, 3)));
        assertTrue(wallLines[1].pos().equalTo(new Pos(4, 4)));
    }

    @Test
    void eWebEngineerOccupiesTwoSpaces() {
        Game game = new Game(null, new GameSessionConfig(4), MissionDefinition.forOption(MissionOption.MISSION_ONE),
                null, true);
        EWebEngineer eWeb = findEWeb(game);

        assertTrue(eWeb.isLargeFigure());
        assertTrue(eWeb.occupiesSpace(new Pos(6, 10)));
        assertTrue(eWeb.occupiesSpace(new Pos(6, 11)));
        assertEquals(eWeb, game.getPersonnelAtPos(new Pos(6, 11)));
    }

    @Test
    void eWebEngineerCannotMoveDiagonallyOrIntoOccupiedFootprint() {
        EWebEngineer eWeb = new EWebEngineer(new Pos(6, 10));
        Game game = new Game(null, new GameSessionConfig(4), MissionDefinition.forOption(MissionOption.MISSION_ONE),
                null, true);
        Hero blocker = game.getHeroes().get(0);
        blocker.setPos(new Pos(7, 11));
        eWeb.setGame(game);

        assertFalse(MovementRules.canMoveOneSpace(eWeb, Directions.DOWNRIGHT, game));
        assertFalse(MovementRules.canMoveOneSpace(eWeb, Directions.RIGHT, game));
    }

    @Test
    void nonSquareLargeFiguresCanRotateWhenFootprintOverlapsAndIsOpen() {
        EWebEngineer eWeb = new EWebEngineer(new Pos(4, 4));

        assertTrue(MovementRules.canRotate(eWeb, null));
        assertEquals(3, MovementRules.getLegalRotations(eWeb, null).size());
        assertFalse(hasRotation(MovementRules.getLegalRotations(eWeb, null), 3, 4, 2, 1));

        eWeb.rotate();

        assertEquals(2, eWeb.getXSize());
        assertEquals(1, eWeb.getYSize());
        assertTrue(eWeb.occupiesSpace(new Pos(5, 4)));
    }

    @Test
    void rotationCanShiftAroundOccupiedSpace() {
        EWebEngineer eWeb = new EWebEngineer(new Pos(4, 4));
        Game game = new Game(null, new GameSessionConfig(1), null, true);
        game.getHeroes().get(0).setPos(new Pos(5, 4));
        eWeb.setGame(game);

        assertTrue(eWeb.canRotate());

        assertFalse(hasRotation(MovementRules.getLegalRotations(eWeb, game), 3, 4, 2, 1));

        RotationMove shifted = findRotation(MovementRules.getLegalRotations(eWeb, game), 3, 5, 2, 1);
        eWeb.rotateTo(shifted);

        assertEquals(3, eWeb.getPos().getX());
        assertEquals(5, eWeb.getPos().getY());
        assertEquals(2, eWeb.getXSize());
        assertEquals(1, eWeb.getYSize());
        assertTrue(eWeb.occupiesSpace(new Pos(3, 5)));
        assertTrue(eWeb.occupiesSpace(new Pos(4, 5)));
        assertFalse(eWeb.occupiesSpace(new Pos(5, 4)));
    }

    @Test
    void rotationCannotEnterOccupiedSpaceWhenAllPivotsAreBlocked() {
        EWebEngineer eWeb = new EWebEngineer(new Pos(4, 4));
        Game game = new Game(null, new GameSessionConfig(4), MissionDefinition.forOption(MissionOption.MISSION_ONE),
                null, true);
        game.getHeroes().get(0).setPos(new Pos(3, 4));
        game.getHeroes().get(1).setPos(new Pos(5, 4));
        game.getHeroes().get(2).setPos(new Pos(3, 5));
        game.getHeroes().get(3).setPos(new Pos(5, 5));
        eWeb.setGame(game);

        assertFalse(eWeb.canRotate());
    }

    @Test
    void personnelWithSameIdCompareAsSameFigure() {
        FennSignis first = new FennSignis(new Pos(1, 1));
        FennSignis second = new FennSignis(new Pos(5, 5));
        FennSignis unassigned = new FennSignis(new Pos(1, 1));
        first.setId("hero-fenn");
        second.setId("hero-fenn");

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertFalse(first.equals(unassigned));
        assertFalse(unassigned.equals(new FennSignis(new Pos(1, 1))));
    }

    @Test
    void movementIgnoresSameIdFigureOccupyingItsCurrentFootprint() {
        Game game = new Game(null, new GameSessionConfig(4), MissionDefinition.forOption(MissionOption.MISSION_ONE),
                null, true);
        EWebEngineer boardEWeb = findEWeb(game);
        boardEWeb.setPos(new Pos(4, 4));
        EWebEngineer movingCopy = new EWebEngineer(new Pos(4, 4));
        movingCopy.setId(boardEWeb.getId());
        movingCopy.setGame(game);

        assertTrue(MovementRules.canMoveOneSpace(movingCopy, Directions.UP, game));
    }

    @Test
    void eWebEngineerCanAttackTwiceButCannotMixMoveAndAttack() {
        Game attackGame = new Game(null, new GameSessionConfig(4),
                MissionDefinition.forOption(MissionOption.MISSION_ONE), new CountingDecisionProvider(), true);
        EWebEngineer attacker = findEWeb(attackGame);
        attacker.setPos(new Pos(4, 4));
        attackGame.getHeroes().get(0).setPos(new Pos(4, 6));

        assertFalse(attacker.gainsMoveBeforeImperialAction());
        assertEquals(2, attacker.getImperialActionCount());
        assertTrue(attackGame.getAvailableActions(attacker, false).contains(Personnel.Actions.ATTACK));

        attackGame.takeAction(attacker, Personnel.Actions.ATTACK);

        assertTrue(attackGame.getAvailableActions(attacker, false).contains(Personnel.Actions.ATTACK));
        assertFalse(attackGame.getAvailableActions(attacker, false).contains(Personnel.Actions.MOVE));

        Game moveGame = new Game(null, new GameSessionConfig(4),
                MissionDefinition.forOption(MissionOption.MISSION_ONE), new CountingDecisionProvider(), true);
        EWebEngineer mover = findEWeb(moveGame);
        mover.setPos(new Pos(4, 4));
        moveGame.getHeroes().get(0).setPos(new Pos(4, 6));

        moveGame.takeAction(mover, Personnel.Actions.MOVE);

        assertFalse(moveGame.getAvailableActions(mover, false).contains(Personnel.Actions.ATTACK));
        assertTrue(moveGame.getAvailableActions(mover, false).contains(Personnel.Actions.MOVE));
    }

    @Test
    void movementRotationAppliesSelectedShiftedPlacement() {
        RotatingDecisionProvider decisionProvider = new RotatingDecisionProvider(3, 5);
        Game game = new Game(null, new GameSessionConfig(4),
                MissionDefinition.forOption(MissionOption.MISSION_ONE), decisionProvider, true);
        EWebEngineer eWeb = findEWeb(game);
        eWeb.setPos(new Pos(4, 4));
        game.getHeroes().get(0).setPos(new Pos(5, 4));

        game.takeAction(eWeb, Personnel.Actions.MOVE);

        assertEquals(3, eWeb.getPos().getX());
        assertEquals(5, eWeb.getPos().getY());
        assertEquals(2, eWeb.getXSize());
        assertEquals(1, eWeb.getYSize());
    }

    @Test
    void printedDeploymentCardSurgesMatchImplementedFigures() {
        FennSignis fenn = new FennSignis(new Pos(1, 1));
        EWebEngineer eWeb = new EWebEngineer(new Pos(1, 1));
        StormTrooper stormTrooper = new StormTrooper(new Pos(1, 1));
        Officer officer = new Officer(new Pos(1, 1));

        assertTrue(fenn.getActions().contains(Personnel.Actions.SPECIAL));
        assertTrue(hasSurge(fenn.getSurgeOptions(), Equipment.SurgeOptions.ACCURACY2));
        assertTrue(hasSurge(eWeb.getSurgeOptions(), Equipment.SurgeOptions.ACCURACY3));
        assertTrue(hasSurge(stormTrooper.getSurgeOptions(), Equipment.SurgeOptions.ACCURACY2));
        assertTrue(hasSurge(officer.getSurgeOptions(), Equipment.SurgeOptions.ACCURACY2));
    }

    @Test
    void fennHavocShotAddsBlastOnlyDuringSpecialAttack() {
        FennSignis fenn = new FennSignis(new Pos(1, 1));
        BlastRecordingGame game = new BlastRecordingGame();
        fenn.setGame(game);

        fenn.performSpecial();

        assertEquals(1, game.recordedBlastValue);
        assertEquals(0, fenn.getBlastValue());
    }

    @Test
    void blastDamagesAdjacentFiguresButNotTarget() {
        Game game = new Game(null, new GameSessionConfig(4), MissionDefinition.forOption(MissionOption.MISSION_ONE),
                null, true);
        Hero target = game.getHeroes().get(0);
        Hero adjacentHero = game.getHeroes().get(1);
        Hero farHero = game.getHeroes().get(2);
        StormTrooper adjacentImperial = findStormTrooper(game);
        target.setPos(new Pos(5, 5));
        adjacentHero.setPos(new Pos(6, 6));
        farHero.setPos(new Pos(9, 9));
        adjacentImperial.setPos(new Pos(4, 5));

        game.applyBlast(target, 1);

        assertEquals(target.getStartingHealth(), target.getHealth());
        assertEquals(adjacentHero.getStartingHealth() - 1, adjacentHero.getHealth());
        assertEquals(farHero.getStartingHealth(), farHero.getHealth());
        assertEquals(adjacentImperial.getStartingHealth() - 1, adjacentImperial.getHealth());
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

        assertEquals(MissionOption.MISSION_TWO, snapshot.mission());
        assertEquals(3, copy.getThreatLevel());
        assertEquals(1, copy.getRoundDial());
        assertEquals(6, copy.getRoundLimit());
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
    void imperialWinsWhenAllRebelsAreDefeated() {
        Game game = new Game(null, new GameSessionConfig(1),
                MissionDefinition.forOption(MissionOption.MISSION_ONE), null, true);
        for (Hero hero : game.getHeroes()) {
            hero.dealDamage(50);
        }
        game.checkEndGame();

        assertFalse(game.isGameEnd());

        for (Hero hero : game.getHeroes()) {
            hero.dealDamage(50);
        }
        game.checkEndGame();

        assertTrue(game.isGameEnd());
        assertFalse(game.rebelsWin());
    }

    @Test
    void rebelsWinWhenAllDeployedImperialsAreDefeated() {
        Game game = new Game(null, new GameSessionConfig(2),
                MissionDefinition.forOption(MissionOption.MISSION_ONE), null, true);
        for (Imperial imperial : game.getImperials()) {
            imperial.setDefeated(true);
        }

        game.checkEndGame();

        assertTrue(game.isGameEnd());
        assertTrue(game.rebelsWin());
    }

    @Test
    void imperialWinsWhenImperialInteractsWithTerminal() {
        Game game = new Game(null, new GameSessionConfig(2),
                MissionDefinition.forOption(MissionOption.MISSION_ONE), null, true);
        Imperial imperial = findStormTrooper(game);

        game.getInteractables()[0].interact(imperial);

        assertTrue(game.isGameEnd());
        assertFalse(game.rebelsWin());
    }

    @Test
    void rebelSideVotesForNextActivationAndThenImperialsAct() throws Exception {
        VotingDecisionProvider decisionProvider = new VotingDecisionProvider(2, 2, 1, 2);
        Game game = new Game(null, new GameSessionConfig(4), MissionDefinition.forOption(MissionOption.MISSION_ONE),
                decisionProvider, true);
        decisionProvider.resetPromptCount();

        assertEquals(PlayerSeat.REBEL_3, invokeChooseNextActivationSeat(game));
        assertEquals(4, decisionProvider.multipleChoicePrompts);
        Object[] initiativeOptions = decisionProvider.multipleChoiceOptions.get(0);
        assertEquals("Fenn Signis", initiativeOptions[0].toString());
        assertEquals("Mak Eshka'rey", initiativeOptions[1].toString());
        assertEquals("Gaarkhan", initiativeOptions[2].toString());
        assertEquals("Diala Passil", initiativeOptions[3].toString());

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
        assertFalse(decisionProvider.promptNames.contains("Deployment Orientation"));
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

    private EWebEngineer findEWeb(Game game) {
        for (DeploymentGroup<? extends Imperial> group : game.getDeploymentGroups()) {
            for (Imperial member : group.getMembers()) {
                if (member instanceof EWebEngineer eWeb) {
                    return eWeb;
                }
            }
        }
        throw new AssertionError("E-Web Engineer not found");
    }

    private RotationMove findRotation(util.MyArrayList<RotationMove> rotations, int x, int y, int xSize, int ySize) {
        for (RotationMove rotation : rotations) {
            if (rotation.anchor().getX() == x && rotation.anchor().getY() == y
                    && rotation.xSize() == xSize && rotation.ySize() == ySize) {
                return rotation;
            }
        }
        throw new AssertionError("Rotation not found");
    }

    private boolean hasRotation(util.MyArrayList<RotationMove> rotations, int x, int y, int xSize, int ySize) {
        for (RotationMove rotation : rotations) {
            if (rotation.anchor().getX() == x && rotation.anchor().getY() == y
                    && rotation.xSize() == xSize && rotation.ySize() == ySize) {
                return true;
            }
        }
        return false;
    }

    private StormTrooper findStormTrooper(Game game) {
        for (DeploymentGroup<? extends Imperial> group : game.getDeploymentGroups()) {
            for (Imperial member : group.getMembers()) {
                if (member instanceof StormTrooper stormTrooper) {
                    return stormTrooper;
                }
            }
        }
        throw new AssertionError("Stormtrooper not found");
    }

    private boolean hasSurge(Equipment.SurgeOptions[] options, Equipment.SurgeOptions expected) {
        for (Equipment.SurgeOptions option : options) {
            if (option == expected) {
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
        private final ArrayList<String> promptNames = new ArrayList<>();
        private final ArrayList<Object[]> multipleChoiceOptions = new ArrayList<>();
        private int multipleChoicePrompts;

        private VotingDecisionProvider(int... votes) {
            this.votes = votes;
        }

        private void resetPromptCount() {
            multipleChoicePrompts = 0;
            multipleChoiceOptions.clear();
        }

        @Override
        public int chooseMultipleChoice(PlayerSeat seat, String name, String explanation, Object[] options) {
            promptNames.add(name);
            multipleChoiceOptions.add(options);
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

    private static final class RotatingDecisionProvider implements GameDecisionProvider {
        private final int targetX;
        private final int targetY;

        private RotatingDecisionProvider(int targetX, int targetY) {
            this.targetX = targetX;
            this.targetY = targetY;
        }

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
            return Math.min(1, maxValue);
        }

        @Override
        public Directions chooseDirection(PlayerSeat seat, Personnel activeFigure, util.MyArrayList<Directions> allowedDirections) {
            return allowedDirections.get(0);
        }

        @Override
        public MovementChoice chooseMovement(PlayerSeat seat, Personnel activeFigure,
                util.MyArrayList<Directions> allowedDirections, util.MyArrayList<RotationMove> legalRotations) {
            for (RotationMove rotation : legalRotations) {
                if (rotation.anchor().getX() == targetX && rotation.anchor().getY() == targetY) {
                    return MovementChoice.rotate(rotation);
                }
            }
            throw new AssertionError("Expected rotation not offered");
        }

        @Override
        public Personnel chooseTarget(PlayerSeat seat, SelectionType selectionType, util.MyArrayList<Personnel> availableTargets) {
            return availableTargets.get(0);
        }
    }

    private static final class BlastRecordingGame extends Game {
        private int recordedBlastValue;

        private BlastRecordingGame() {
            super(null, new GameSessionConfig(1), MissionDefinition.forOption(MissionOption.MISSION_ONE), null, false);
        }

        @Override
        public void handleAttack(Personnel activeFigure) {
            recordedBlastValue = activeFigure.getBlastValue();
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
