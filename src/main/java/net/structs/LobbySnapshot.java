package net;

import java.io.Serializable;
import java.util.List;

import game.GameSessionConfig;
import game.PlayerSeat;

public record LobbySnapshot(
        GameSessionConfig config,
        List<PlayerSeat> occupiedSeats,
        List<PlayerSeat> readySeats,
        boolean allSeatsFilled,
        boolean allReady) implements Serializable {
}
