import src.net.GameServer;

public class ServerRunner {
    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5050;
        int rebelPlayers = args.length > 1 ? Integer.parseInt(args[1]) : 2;
        new GameServer(port, rebelPlayers).run();
    }
}
