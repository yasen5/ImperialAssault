package net;

import java.io.Serializable;

import game.PlayerSeat;

public record JoinRequest(PlayerSeat requestedSeat) implements Serializable {
}
