package net.structs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import game.PlayerSeat;

public record GameSessionConfig(int rebelPlayerCount) implements Serializable {
    public GameSessionConfig {
        if (rebelPlayerCount < 1 || rebelPlayerCount > 2) {
            throw new IllegalArgumentException("Rebel player count must be 1 or 2");
        }
    }

    public List<PlayerSeat> rebelTurnOrder() {
        ArrayList<PlayerSeat> seats = new ArrayList<>();
        seats.add(PlayerSeat.REBEL_1);
        if (rebelPlayerCount == 2) {
            seats.add(PlayerSeat.REBEL_2);
        }
        return seats;
    }

    public List<PlayerSeat> requiredSeats() {
        ArrayList<PlayerSeat> seats = new ArrayList<>();
        seats.add(PlayerSeat.IMPERIAL);
        seats.add(PlayerSeat.REBEL_1);
        if (rebelPlayerCount == 2) {
            seats.add(PlayerSeat.REBEL_2);
        }
        return seats;
    }
}
