package runners;

import net.GameServer;
import net.NetworkConfig;

public class ServerRunner {
    public static void main(String[] args) throws Exception {
        int port = NetworkConfig.PORT;
        int rebelPlayers = 1;
        new GameServer(port, rebelPlayers).run();
    }
}
