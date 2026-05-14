package runners;

import net.GameServer;
import net.NetworkConfig;

public class ServerRunner {
    public static void main(String[] args) throws Exception {
        int port = NetworkConfig.PORT;
        int rebelPlayers = 1;
        boolean loadPreviousGame = true;
        for (String arg : args) {
            String mode = arg.trim().toLowerCase();
            if ("single-client".equals(mode) || "--single-client".equals(mode) || "wait-one".equals(mode)) {
                rebelPlayers = 0;
            } else if ("1".equals(mode) || "2".equals(mode)) {
                rebelPlayers = Integer.parseInt(mode);
            } else if ("--fresh".equals(mode) || "--no-load".equals(mode) || "--new-game".equals(mode)) {
                loadPreviousGame = false;
            } else {
                throw new IllegalArgumentException("Usage: ServerRunner [--single-client|1|2] [--fresh|--no-load]");
            }
        }
        new GameServer(port, rebelPlayers, loadPreviousGame).run();
    }
}
