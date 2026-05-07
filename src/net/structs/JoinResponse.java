package src.net;

import java.io.Serializable;

import src.game.GameSessionConfig;
import src.game.PlayerSeat;

public record JoinResponse(boolean accepted, String message, PlayerSeat seat, GameSessionConfig config)
        implements Serializable {
}
