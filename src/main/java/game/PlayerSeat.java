package game;

import java.io.Serializable;

public enum PlayerSeat implements Serializable {
    IMPERIAL,
    REBEL_1,
    REBEL_2,
    REBEL_3,
    REBEL_4;

    public boolean isRebel() {
        return this == REBEL_1 || this == REBEL_2 || this == REBEL_3 || this == REBEL_4;
    }
}
