package game;

import net.structs.MissionOption;
import game.Personnel.Actions;

public record MissionDefinition(
    MissionOption option,
    String displayName,
    String mapImageName,
    int[][] tileMatrix,
    Constants.WallLine[] wallLines,
    int threatLevel,
    int roundLimit,
    boolean usesThreat,
    boolean attackableTerminals,
    HeroPlacement heroPlacement,
    Pos[] terminalPositions,
    DoorSpec[] doors,
    Pos[] cratePositions,
    DeploymentSpec[] deployments,
    ActionDescription[] actionDescriptions) {
  public record ActionDescription(Actions action, String description) {
  }

  public MissionDefinition(
      MissionOption option,
      String displayName,
      String mapImageName,
      int[][] tileMatrix,
      Constants.WallLine[] wallLines,
      int threatLevel,
      int roundLimit,
      boolean usesThreat,
      boolean attackableTerminals,
      HeroPlacement heroPlacement,
      Pos[] terminalPositions,
      DoorSpec[] doors,
      Pos[] cratePositions,
      DeploymentSpec[] deployments) {
    this(option, displayName, mapImageName, tileMatrix, wallLines, threatLevel, roundLimit, usesThreat,
        attackableTerminals, heroPlacement, terminalPositions, doors, cratePositions, deployments,
        new ActionDescription[] {});
  }

  public record HeroPlacement(Pos[] positions) {
    public Pos position(int index) {
      if (index < 0 || index >= positions.length) {
        return positions.length == 0 ? new Pos(0, 0) : positions[0];
      }
      return positions[index];
    }
  }

  public record DoorSpec(Pos pos, boolean vertical) {
  }

  public record DeploymentSpec(String id, String groupName, Pos[] positions, int deploymentCost, boolean deployed,
      int minimumHeroCount, Boolean horizontal) {
    public DeploymentSpec(String id, String groupName, Pos[] positions, int deploymentCost, boolean deployed) {
      this(id, groupName, positions, deploymentCost, deployed, 0, null);
    }

    public DeploymentSpec(String id, String groupName, Pos[] positions, int deploymentCost, boolean deployed,
        int minimumHeroCount) {
      this(id, groupName, positions, deploymentCost, deployed, minimumHeroCount, null);
    }
  }

  public String actionDescription(Actions action) {
    if (actionDescriptions == null) {
      return null;
    }
    for (ActionDescription actionDescription : actionDescriptions) {
      if (actionDescription.action() == action) {
        return actionDescription.description();
      }
    }
    return null;
  }

  public static MissionDefinition forOption(MissionOption option) {
    return switch (option) {
      case MISSION_ONE -> new MissionDefinition(option, option.displayName(), "TutorialTile",
          Constants.TUTORIAL_TILE_MATRIX, Constants.TUTORIAL_WALL_LINES,
          0, 0, false, false,
          new HeroPlacement(new Pos[] { new Pos(0, 4), new Pos(0, 5), new Pos(7, 4), new Pos(7, 5) }),
          new Pos[] { new Pos(7, 0), new Pos(0, 3) },
          new DoorSpec[] { new DoorSpec(new Pos(0, 6), false), new DoorSpec(new Pos(6, 8), false) },
          new Pos[] { new Pos(3, 3) },
          new DeploymentSpec[] {
              new DeploymentSpec("imperial-stormtroopers", "StormTrooper",
                  new Pos[] { new Pos(4, 11), new Pos(4, 12), new Pos(5, 11) }, 6, true),
              new DeploymentSpec("imperial-officer", "ImperialOfficer",
                  new Pos[] { new Pos(1, 5) }, 4, true),
              new DeploymentSpec("imperial-probe-droid", "ProbeDroid",
                  new Pos[] { new Pos(7, 11) }, 5, true, 3),
              new DeploymentSpec("imperial-e-web-engineer", "EWebEngineer",
                  new Pos[] { new Pos(6, 10) }, 6, true, 4)
          },
          new ActionDescription[] {
              new ActionDescription(Actions.MOVE,
                  "Gain movement points equal to your figure's speed, then use the arrow buttons around the figure to spend them one space at a time. Large figures may also get Rotate buttons when rotation is legal."),
              new ActionDescription(Actions.ATTACK,
                  "Choose a highlighted enemy target, roll attack dice, then spend any surges before damage is applied."),
              new ActionDescription(Actions.RECOVER,
                  "Rest to remove all strain from your hero, then recover health equal to any strain you could not remove."),
              new ActionDescription(Actions.USE_EQUIPMENT,
                  "Use one of your hero's ready equipment cards that can be used during an activation."),
              new ActionDescription(Actions.DISCARD_CONDITION,
                  "Spend this action to remove Stunned so the figure can attack and use special abilities again."),
              new ActionDescription(Actions.SPECIAL,
                  "Your hero has this special ability. You can read more about it by clicking on your hero."),
              new ActionDescription(Actions.INTERACT,
                  "Use this when your hero is next to a crate, door, or terminal. The mission will resolve the object you clicked or selected.")
          });
      case MISSION_TWO -> new MissionDefinition(option, option.displayName(), "Mission2Map",
          Constants.MISSION_TWO_TILE_MATRIX, Constants.MISSION_TWO_WALL_LINES,
          3, 6, true, true,
          new HeroPlacement(new Pos[] { new Pos(2, 0), new Pos(1, 0), new Pos(2, 1), new Pos(3, 0) }),
          new Pos[] { new Pos(2, 6), new Pos(6, 1), new Pos(5, 3), new Pos(9, 7) },
          new DoorSpec[] { new DoorSpec(new Pos(6, 5), true) },
          new Pos[] {},
          new DeploymentSpec[] {
              new DeploymentSpec("imperial-stormtroopers", "StormTrooper",
                  new Pos[] { new Pos(0, 5), new Pos(2, 4), new Pos(1, 3) }, 6, true),
              new DeploymentSpec("imperial-officer", "ImperialOfficer",
                  new Pos[] { new Pos(4, 6) }, 4, true),
              new DeploymentSpec("imperial-probe-droid", "ProbeDroid",
                  new Pos[] { new Pos(2, 5) }, 5, true),
              new DeploymentSpec("imperial-e-web-engineer-reserve", "EWebEngineer",
                  new Pos[] { new Pos(10, 5) }, 6, false, 0, false),
              new DeploymentSpec("imperial-stormtroopers-reserve", "StormTrooper",
                  new Pos[] { new Pos(5, 1), new Pos(6, 1), new Pos(6, 2) }, 6, false),
              new DeploymentSpec("imperial-officer-reserve", "ImperialOfficer",
                  new Pos[] { new Pos(7, 2) }, 4, false)
          });
    };
  }
}
