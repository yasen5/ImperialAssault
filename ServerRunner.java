import src.net.GameServer;

public class ServerRunner {
    public static void main(String[] args) throws Exception {
        int port = 5050;
        int rebelPlayers = 1;
        new GameServer(port, rebelPlayers).run();
    }
}
