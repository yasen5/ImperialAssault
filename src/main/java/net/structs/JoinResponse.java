package net;

import java.io.Serializable;

import game.GameSessionConfig;
import game.PlayerSeat;

public record JoinResponse(boolean accepted, String message, PlayerSeat seat, GameSessionConfig config,
        LobbySnapshot lobbySnapshot)
        implements Serializable {
}
