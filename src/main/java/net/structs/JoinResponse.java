package net.structs;

import java.io.Serializable;

import game.PlayerSeat;

public record JoinResponse(boolean accepted, String message, PlayerSeat seat, GameSessionConfig config,
        LobbySnapshot lobbySnapshot)
        implements Serializable {
}
