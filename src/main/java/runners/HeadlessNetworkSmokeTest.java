package runners;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import game.PlayerSeat;
import net.GameServer;
import net.structs.ClientMissionSelection;
import net.structs.JoinRequest;
import net.structs.JoinResponse;
import net.structs.MatchSnapshot;
import net.structs.MissionOption;
import net.structs.PromptResponse;
import net.structs.RemotePrompt;

public class HeadlessNetworkSmokeTest {
    public static void main(String[] args) throws IOException, InterruptedException {
        int rebelPlayers = args.length > 0 ? Integer.parseInt(args[0]) : 4;
        int port = findOpenPort();
        CountDownLatch imperialTurnReached = new CountDownLatch(1);
        CountDownLatch makDeploymentPromptReached = new CountDownLatch(rebelPlayers >= 4 ? 1 : 0);
        SmokeTestStatus status = new SmokeTestStatus();

        Thread serverThread = new Thread(() -> {
            try {
                new GameServer(port, rebelPlayers, false, false).run();
            } catch (Throwable ex) {
                status.recordFailure(ex);
            }
        }, "headless-smoke-server");
        serverThread.setDaemon(true);
        serverThread.start();

        Thread.sleep(250L);
        startBot(port, PlayerSeat.IMPERIAL, imperialTurnReached, makDeploymentPromptReached, status);
        for (int i = 0; i < rebelPlayers; i++) {
            startBot(port, PlayerSeat.values()[PlayerSeat.REBEL_1.ordinal() + i], imperialTurnReached,
                    makDeploymentPromptReached, status);
        }

        boolean reached = imperialTurnReached.await(20, TimeUnit.SECONDS);
        if (!reached || makDeploymentPromptReached.getCount() > 0 || status.failure().isPresent()) {
            Throwable ex = status.failure().orElse(null);
            if (ex != null) {
                ex.printStackTrace(System.err);
            }
            status.printPromptTrace();
            System.err.println("Headless network smoke test did not reach the Imperial turn after Mak moved");
            System.exit(1);
            return;
        }
        System.out.println("Headless network smoke test moved Mak through the door and reached Imperial turn on port " + port);
        System.exit(0);
    }

    private static int findOpenPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void startBot(int port, PlayerSeat seat, CountDownLatch imperialTurnReached,
            CountDownLatch makDeploymentPromptReached, SmokeTestStatus status) {
        Thread thread = new Thread(() -> {
            try (Socket socket = new Socket("127.0.0.1", port)) {
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                out.writeObject(new JoinRequest(seat));
                out.flush();
                JoinResponse response = (JoinResponse) in.readObject();
                if (!response.accepted()) {
                    System.err.println(response.message());
                    imperialTurnReached.countDown();
                    return;
                }
                out.writeObject(new ClientMissionSelection(MissionOption.MISSION_ONE));
                out.flush();
                SmokeBot bot = new SmokeBot();
                while (imperialTurnReached.getCount() > 0) {
                    Object message = in.readObject();
                    if (message instanceof RemotePrompt prompt) {
                        status.recordPrompt(prompt);
                        if (prompt.seat() == PlayerSeat.IMPERIAL && "Deployment Selection".equals(prompt.title())) {
                            imperialTurnReached.countDown();
                        }
                        out.writeObject(new PromptResponse(prompt.promptId(), bot.responseFor(prompt,
                                makDeploymentPromptReached)));
                        out.flush();
                    } else if (message instanceof MatchSnapshot) {
                        // Snapshot delivery proves the bot can deserialize real game state.
                    }
                }
            } catch (Throwable ex) {
                status.recordFailure(ex);
                imperialTurnReached.countDown();
            }
        }, "headless-smoke-" + seat);
        thread.setDaemon(true);
        thread.start();
    }

    private static int indexFor(RemotePrompt prompt, String preferredLabel, int fallback) {
        for (int i = 0; i < prompt.optionLabels().size(); i++) {
            if (preferredLabel.equals(prompt.optionLabels().get(i))) {
                return i;
            }
        }
        return fallback;
    }

    private static final class SmokeBot {
        private boolean controlsMak;
        private int makActionPrompts;
        private int makNumericPrompts;

        private SmokeBot() {
        }

        private String responseFor(RemotePrompt prompt, CountDownLatch makDeploymentPromptReached) {
            rememberMakController(prompt, makDeploymentPromptReached);
            return switch (prompt.type()) {
                case MULTIPLE_CHOICE -> String.valueOf(multipleChoiceResponse(prompt));
                case YES_NO -> "false";
                case NUMERIC -> String.valueOf(numericResponse(prompt));
                case DIRECTION -> directionResponse(prompt);
                case TARGET -> prompt.allowedValues().isEmpty() ? "" : prompt.allowedValues().get(0);
            };
        }

        private void rememberMakController(RemotePrompt prompt, CountDownLatch makDeploymentPromptReached) {
            if ("Deployment Selection".equals(prompt.title()) && prompt.optionLabels().contains("MakEshray")) {
                controlsMak = true;
                makDeploymentPromptReached.countDown();
            }
        }

        private int multipleChoiceResponse(RemotePrompt prompt) {
            if ("Rebel Initiative".equals(prompt.title())) {
                return indexFor(prompt, "Mak Eshka'rey", 0);
            }
            if ("Hero Selection".equals(prompt.title())) {
                return 0;
            }
            if (controlsMak && "Action Selection".equals(prompt.title())) {
                makActionPrompts++;
                return makActionPrompts == 1 ? indexFor(prompt, "MOVE", 0) : indexFor(prompt, "INTERACT", 0);
            }
            return indexFor(prompt, "RECOVER", 0);
        }

        private int numericResponse(RemotePrompt prompt) {
            if (controlsMak) {
                makNumericPrompts++;
                if (makNumericPrompts <= 2 && prompt.minValue() <= 2 && prompt.maxValue() >= 2) {
                    return 2;
                }
            }
            return prompt.minValue();
        }

        private String directionResponse(RemotePrompt prompt) {
            if (controlsMak && prompt.allowedValues().contains("DOWN")) {
                return "DOWN";
            }
            return prompt.allowedValues().isEmpty() ? "" : prompt.allowedValues().get(0);
        }
    }

    private static final class SmokeTestStatus {
        private Throwable failure;
        private final StringBuilder promptTrace = new StringBuilder();

        synchronized void recordFailure(Throwable ex) {
            if (failure == null) {
                failure = ex;
            }
        }

        synchronized java.util.Optional<Throwable> failure() {
            return java.util.Optional.ofNullable(failure);
        }

        synchronized void recordPrompt(RemotePrompt prompt) {
            promptTrace.append(prompt.seat())
                    .append(" | ")
                    .append(prompt.title())
                    .append(" | ")
                    .append(prompt.type())
                    .append(" | ")
                    .append(prompt.optionLabels())
                    .append(System.lineSeparator());
        }

        synchronized void printPromptTrace() {
            if (promptTrace.length() > 0) {
                System.err.println("Prompt trace:");
                System.err.print(promptTrace);
            }
        }
    }
}
