package runners;

import net.GameServer;
import net.NetworkConfig;

public class ServerRunner {
    public static void main(String[] args) throws Exception {
        int port = NetworkConfig.PORT;
        int rebelPlayers = 1;
        boolean loadPreviousGame = true;
        boolean showSpectator = true;
        for (String arg : args) {
            String mode = arg.trim().toLowerCase();
            if ("single-client".equals(mode) || "--single-client".equals(mode) || "wait-one".equals(mode)) {
                rebelPlayers = 0;
            } else if ("1".equals(mode) || "2".equals(mode) || "3".equals(mode) || "4".equals(mode)) {
                rebelPlayers = Integer.parseInt(mode);
            } else if ("--fresh".equals(mode) || "--no-load".equals(mode) || "--new-game".equals(mode)) {
                loadPreviousGame = false;
            } else if ("--no-ui".equals(mode) || "--headless".equals(mode)) {
                showSpectator = false;
            } else {
                throw new IllegalArgumentException(
                        "Usage: ServerRunner [--single-client|1|2|3|4] [--fresh|--no-load] [--no-ui]");
            }
        }
        new GameServer(port, rebelPlayers, loadPreviousGame, showSpectator).run();
    }
}
