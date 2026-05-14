package visual;

import util.MyArrayList;

import java.util.concurrent.CompletableFuture;

import javax.swing.SwingUtilities;

import game.InputUtils;
import game.Game;
import game.Personnel;
import game.Personnel.Directions;
import game.PlayerSeat;
import game.SelectionType;
import net.GameDecisionProvider;

public class LocalGameDecisionProvider implements GameDecisionProvider {
  private final Screen ui;

  public LocalGameDecisionProvider(Screen ui) {
    this.ui = ui;
  }

  @Override
  public int chooseMultipleChoice(PlayerSeat seat, String name, String explanation, Object[] options) {
    return InputUtils.showMultipleChoiceDialog(name, explanation, options);
  }

  @Override
  public boolean chooseYesNo(PlayerSeat seat, String name, String explanation) {
    return InputUtils.showYesNoDialog(name, explanation);
  }

  @Override
  public int chooseNumericChoice(PlayerSeat seat, String name, int minValue, int maxValue) {
    return InputUtils.showNumericChoiceDialog(name, minValue, maxValue);
  }

  @Override
  public Directions chooseDirection(PlayerSeat seat, Personnel activeFigure, MyArrayList<Directions> allowedDirections) {
    CompletableFuture<Directions> dir = new CompletableFuture<>();
    ui.getGame().setActivePromptCancelAction(() -> dir.cancel(true));
    ui.setMovementButtonOutput(dir);
    double[] angleRads = { Math.PI / 4.0 };
    try {
      SwingUtilities.invokeLater(() -> {
        for (Directions direction : Directions.values()) {
          angleRads[0] += Math.PI / 4;
          if (allowedDirections.contains(direction)) {
            ui.moveAndActivateButton(direction,
                activeFigure.getPos().getX(), activeFigure.getPos().getY(),
                angleRads[0]);
          } else {
            ui.deactivateMovementButton(direction);
          }
        }
      });
      return dir.join();
    } finally {
      ui.getGame().clearActivePromptCancelAction();
    }
  }

  @Override
  public Personnel chooseTarget(PlayerSeat seat, SelectionType selectionType, MyArrayList<Personnel> availableTargets) {
    game.Game game = ui.getGame();
    CompletableFuture<Personnel> currentSelection = new CompletableFuture<>();
    game.setCurrentSelection(currentSelection);
    game.setActivePromptCancelAction(() -> currentSelection.cancel(true));
    ui.setSelectionType(selectionType);
    ui.repaint();
    try {
      return game.getCurrentSelection().join();
    } finally {
      game.clearActivePromptCancelAction();
    }
  }
}
