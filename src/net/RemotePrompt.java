package src.net;

import java.io.Serializable;
import java.util.List;

import src.Screen.SelectingType;
import src.game.PlayerSeat;

public record RemotePrompt(
        long promptId,
        PlayerSeat seat,
        PromptType type,
        String title,
        String message,
        List<String> optionLabels,
        int minValue,
        int maxValue,
        List<String> allowedValues,
        String subjectId,
        SelectingType selectionType) implements Serializable {
    public static enum PromptType {
        MULTIPLE_CHOICE,
        YES_NO,
        NUMERIC,
        DIRECTION,
        TARGET
    }
}
