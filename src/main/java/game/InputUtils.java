package game;

import javax.swing.JOptionPane;

import game.Constants;

public class InputUtils {
    // Get which response they chose, keep asking until response is valid
    public static int getMultipleChoice(String name, String explanation, Object[] options) {
        Game currentGame = Game.current();
        if (currentGame != null && currentGame.hasDecisionProvider()) {
            return currentGame.promptMultipleChoice(currentGame.getActingSeat(), name, explanation, options);
        }
        return showMultipleChoiceDialog(name, explanation, options);
    }

    public static int showMultipleChoiceDialog(String name, String explanation, Object[] options) {
        if (Constants.screen != null) {
            return Constants.screen.promptMultipleChoice(name, explanation, options);
        }
        int selectedIndex;
        do {
            selectedIndex = JOptionPane.showOptionDialog(
                    Constants.frame,
                    explanation,
                    name,
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    null);
        } while (selectedIndex < 0 || selectedIndex >= options.length);
        return selectedIndex;
    }

    // Give yes/no prompt
    public static boolean getYesNo(String name, String explanation) {
        Game currentGame = Game.current();
        if (currentGame != null && currentGame.hasDecisionProvider()) {
            return currentGame.promptYesNo(currentGame.getActingSeat(), name, explanation);
        }
        return showYesNoDialog(name, explanation);
    }

    public static boolean showYesNoDialog(String name, String explanation) {
        if (Constants.screen != null) {
            return Constants.screen.promptYesNo(name, explanation);
        }
        int response = JOptionPane.showConfirmDialog(
                Constants.frame,
                explanation,
                name,
                JOptionPane.YES_NO_OPTION);
        return response == JOptionPane.YES_OPTION;
    }

    // Get a numeric answer, force it to be valid
    public static int getNumericChoice(String name, int minValue, int maxValue) {
        Game currentGame = Game.current();
        if (currentGame != null && currentGame.hasDecisionProvider()) {
            return currentGame.promptNumericChoice(currentGame.getActingSeat(), name, minValue, maxValue);
        }
        return showNumericChoiceDialog(name, name, minValue, maxValue);
    }

    public static int showNumericChoiceDialog(String name, int minValue, int maxValue) {
        return showNumericChoiceDialog(name, name, minValue, maxValue);
    }

    public static int showNumericChoiceDialog(String name, String explanation, int minValue, int maxValue) {
        if (Constants.screen != null) {
            return Constants.screen.promptNumericChoice(name, explanation, minValue, maxValue);
        }
        int input = Integer.MIN_VALUE;
        String message = explanation == null || explanation.isBlank() ? name : explanation;
        message += " (" + minValue + " to " + maxValue + ")";
        do {
            String rawInput = JOptionPane.showInputDialog(Constants.frame, message, name, JOptionPane.QUESTION_MESSAGE);
            if (rawInput != null) {
                try {
                    input = Integer.valueOf(rawInput.trim());
                } catch (NumberFormatException ex) {
                    input = Integer.MIN_VALUE;
                }
            }
        } while (input < minValue || input > maxValue);
        return input;
    }
}
