package net;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Optional;
import util.MyArrayList;
import util.MyDLList;
import util.MyHashMap;

import java.util.concurrent.CancellationException;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import game.Game;
import game.MissionDefinition;
import net.structs.MissionOption;
import game.Personnel;
import game.PlayerSeat;
import game.RotationMove;
import game.SelectionType;
import visual.UiContext;
import game.Personnel.Directions;
import game.MovementChoice;
import net.GameDecisionProvider;
import net.NetworkConfig;
import net.structs.ClientMissionSelection;
import net.structs.ClientFinishGameRequest;
import net.structs.GameSessionConfig;
import net.structs.JoinRequest;
import net.structs.JoinResponse;
import net.structs.LobbySnapshot;
import net.structs.MatchSnapshot;
import net.structs.PromptResponse;
import net.structs.RemoteBanner;
import net.structs.RemotePromptCancel;
import net.structs.RemotePrompt;
import visual.Screen;
import visual.WindowFocus;

public class GameServer {
  private final int port;
  private final GameSessionConfig config;
  private final boolean loadPreviousGame;
  private Path savePath;
  private final Object saveLock = new Object();
  private final MyHashMap<PlayerSeat, ClientConnection> clients = new MyHashMap<>(PlayerSeat.class);
  private final MyArrayList<PlayerSeat> rebelJoinOrder = new MyArrayList<>();
  private long nextPromptId = 1;
  private final Object lobbyLock = new Object();
  private volatile Game spectatorGame;
  private volatile Game activeGame;
  private volatile Screen spectatorScreen;
  private volatile String hostAddress;
  private final boolean showSpectator;
  private final boolean debugWallLines;

  public GameServer(int port, int rebelPlayers) {
    this(port, rebelPlayers, true);
  }

  public GameServer(int port, int rebelPlayers, boolean loadPreviousGame) {
    this(port, rebelPlayers, loadPreviousGame, true);
  }

  public GameServer(int port, int rebelPlayers, boolean loadPreviousGame, boolean showSpectator) {
    this(port, rebelPlayers, loadPreviousGame, showSpectator, false);
  }

  public GameServer(int port, int rebelPlayers, boolean loadPreviousGame, boolean showSpectator,
      boolean debugWallLines) {
    this.port = port;
    this.config = new GameSessionConfig(rebelPlayers);
    this.loadPreviousGame = loadPreviousGame;
    this.showSpectator = showSpectator;
    this.debugWallLines = debugWallLines;
    this.savePath = Path.of("server-game-state.ser");
  }

  public void run() throws IOException, ClassNotFoundException, InterruptedException, InvocationTargetException {
    hostAddress = NetworkConfig.resolveMachineHostAddress();
    if (showSpectator) {
      startSpectatorDisplay();
    }
    try (ServerSocket serverSocket = new ServerSocket(port, 50,
        InetAddress.getByName(NetworkConfig.SERVER_BIND_ADDRESS))) {
      while (true) {
        ClientConnection connection;
        Socket socket = serverSocket.accept();
        connection = new ClientConnection(socket);
        JoinRequest request = (JoinRequest) connection.in.readObject();
        Optional<PlayerSeat> assignedSeat = assignSeat(request.requestedSeat());
        if (assignedSeat.isEmpty()) {
          JoinResponse response = new JoinResponse(false, "Seat unavailable", request.requestedSeat(), config,
              createLobbySnapshot());
          connection.send(response);
          socket.close();
          continue;
        }
        PlayerSeat seat = assignedSeat.get();
        synchronized (lobbyLock) {
          clients.put(seat, connection);
          connection.seat = seat;
          if (seat.isRebel()) {
            rebelJoinOrder.add(seat);
          }
        }
        LobbySnapshot lobbySnapshot = config.rebelPlayerCount() == 0 ? null : createLobbySnapshot();
        connection.send(new JoinResponse(true, "Joined", seat, config, lobbySnapshot));
        connection.startReader();
        if (config.rebelPlayerCount() > 0) {
          broadcastLobbyState();
        }
        if (allSeatsFilled()) {
          break;
        }
      }
      if (config.rebelPlayerCount() > 0) {
        waitForAllMissionSelections();
      }
      MissionOption mission = config.rebelPlayerCount() == 0 ? MissionOption.MISSION_ONE
          : getSelectedMission();
      savePath = savePathForMission(mission);
      Game game = createGameForMission(mission);
      activeGame = game;
      Optional<MatchSnapshot> loadedSnapshot = loadPreviousGame ? tryLoadSavedSnapshot(mission) : Optional.empty();
      game.setSnapshotListener(this::broadcastSnapshot);
      if (loadedSnapshot.isPresent()) {
        game.loadSnapshot(loadedSnapshot.get());
      } else {
        game.setup();
      }
      SwingUtilities.invokeLater(() -> {
        if (spectatorScreen != null) {
          spectatorScreen.setIncreaseThreatAction(game::increaseThreat);
          spectatorScreen.setNextRoundAction(game::requestAdvanceStatusPhase);
          spectatorScreen.setFinishGameAction(game::skipToEndScreen);
          spectatorScreen.setRestartGameAction(game::requestRestartFromBeginning);
        }
      });
      broadcastSnapshot(game.createSnapshot());
      Thread gameThread = new Thread(game::playRound);
      gameThread.start();
      gameThread.join();
    }
  }

  private boolean hasClient(PlayerSeat seat) {
    synchronized (lobbyLock) {
      return clients.containsKey(seat);
    }
  }

  private Optional<PlayerSeat> assignSeat(PlayerSeat requestedSeat) {
    synchronized (lobbyLock) {
      if (requestedSeat != null) {
        return config.requiredSeats().contains(requestedSeat) && !clients.containsKey(requestedSeat)
            ? Optional.of(requestedSeat)
            : Optional.empty();
      }
      for (PlayerSeat seat : config.requiredSeats()) {
        if (!clients.containsKey(seat)) {
          return Optional.of(seat);
        }
      }
      return Optional.empty();
    }
  }

  private boolean allSeatsFilled() {
    synchronized (lobbyLock) {
      return clients.size() >= config.requiredSeats().size();
    }
  }

  private void waitForAllMissionSelections() throws InterruptedException {
    synchronized (lobbyLock) {
      while (!allMissionSelectionsLocked()) {
        lobbyLock.wait();
      }
    }
  }

  private boolean allMissionSelectionsLocked() {
    if (clients.size() < config.requiredSeats().size()) {
      return false;
    }
    MissionOption mission = null;
    for (PlayerSeat seat : config.requiredSeats()) {
      ClientConnection connection = clients.get(seat);
      Optional<MissionOption> connectionMission = connection == null ? Optional.empty() : connection.selectedMission();
      if (connectionMission.isEmpty()) {
        return false;
      }
      if (mission == null) {
        mission = connectionMission.get();
      } else if (mission != connectionMission.get()) {
        return false;
      }
    }
    return true;
  }

  private LobbySnapshot createLobbySnapshot() {
    synchronized (lobbyLock) {
      MyArrayList<PlayerSeat> occupiedSeats = new MyArrayList<>(clients.keySet());
      MyHashMap<PlayerSeat, MissionOption> missionSelections = new MyHashMap<>(PlayerSeat.class);
      MissionOption selectedMission = null;
      boolean allMissionSelections = true;
      for (MyHashMap.Entry<PlayerSeat, ClientConnection> entry : clients.entrySet()) {
        Optional<MissionOption> mission = entry.getValue().selectedMission();
        if (mission.isPresent()) {
          missionSelections.put(entry.getKey(), mission.get());
          if (selectedMission == null) {
            selectedMission = mission.get();
          } else if (selectedMission != mission.get()) {
            allMissionSelections = false;
          }
        } else {
          allMissionSelections = false;
        }
      }
      boolean allSeatsFilled = clients.size() >= config.requiredSeats().size();
      boolean allMissionSelectionsMatch = allSeatsFilled && allMissionSelections && allMissionSelectionsLocked();
      if (!allMissionSelectionsMatch) {
        selectedMission = null;
      }
      return new LobbySnapshot(config, occupiedSeats, missionSelections,
          allSeatsFilled, allMissionSelections && allSeatsFilled, allMissionSelectionsMatch,
          selectedMission);
    }
  }

  private void broadcastLobbyState() {
    LobbySnapshot snapshot = createLobbySnapshot();
    updateSpectatorLobbySnapshot(snapshot);
    MyArrayList<ClientConnection> connections;
    synchronized (lobbyLock) {
      connections = new MyArrayList<>(clients.values());
    }
    for (ClientConnection connection : connections) {
      connection.send(snapshot);
    }
  }

  private void handleClientMissionSelection(ClientConnection connection, ClientMissionSelection missionSelection) {
    synchronized (lobbyLock) {
      connection.selectMission(missionSelection.mission());
      lobbyLock.notifyAll();
    }
    broadcastLobbyState();
  }

  private void handleClientFinishGameRequest() {
    Optional.ofNullable(activeGame).ifPresent(Game::skipToEndScreen);
  }

  private Game createGameForMission(MissionOption mission) {
    Game game = switch (mission) {
      case MISSION_ONE, MISSION_TWO -> new Game(null, config, MissionDefinition.forOption(mission), null, false);
    };
    game.setDebugWallLines(debugWallLines);
    game.setDecisionProvider(new RemoteDecisionProvider(game));
    synchronized (lobbyLock) {
      game.setRebelHeroSelectionOrder(new MyArrayList<>(rebelJoinOrder));
    }
    return game;
  }

  private MissionOption getSelectedMission() {
    synchronized (lobbyLock) {
      for (PlayerSeat seat : config.requiredSeats()) {
        ClientConnection connection = clients.get(seat);
        Optional<MissionOption> mission = connection == null ? Optional.empty() : connection.selectedMission();
        if (mission.isPresent()) {
          return mission.get();
        }
      }
    }
    return MissionOption.MISSION_ONE;
  }

  private void broadcastSnapshot(MatchSnapshot snapshot) {
    saveSnapshot(snapshot);
    updateSpectatorSnapshot(snapshot);
    sendSnapshotToClients(snapshot);
  }

  private void sendSnapshotToClients(MatchSnapshot snapshot) {
    MyArrayList<ClientConnection> connections;
    synchronized (lobbyLock) {
      connections = new MyArrayList<>(clients.values());
    }
    for (ClientConnection connection : connections) {
      connection.send(snapshot);
    }
  }

  private void sendPromptBannerToOtherClients(RemotePrompt prompt) {
    RemoteBanner banner = new RemoteBanner(formatSeat(prompt.seat()) + " has been prompted");
    MyArrayList<ClientConnection> connections;
    synchronized (lobbyLock) {
      connections = new MyArrayList<>(clients.values());
    }
    for (ClientConnection connection : connections) {
      if (connection.seat != prompt.seat()) {
        connection.send(banner);
      }
    }
  }

  private String formatSeat(PlayerSeat seat) {
    return switch (seat) {
      case IMPERIAL -> "Imperial";
      case REBEL_1 -> "Rebel 1";
      case REBEL_2 -> "Rebel 2";
      case REBEL_3 -> "Rebel 3";
      case REBEL_4 -> "Rebel 4";
    };
  }

  private void startSpectatorDisplay() throws InterruptedException, InvocationTargetException {
    spectatorGame = new Game(null, config, null, false);
    spectatorGame.setDebugWallLines(debugWallLines);
    SwingUtilities.invokeAndWait(() -> {
      spectatorScreen = new Screen(spectatorGame, true, true);
      spectatorGame.setUi(spectatorScreen);
      spectatorScreen.setServerStatusText("Hosting on " + hostAddress + ":" + port + " | read-only spectator");
      JFrame frame = new JFrame("Imperial Assault Server - " + hostAddress + ":" + port);
      UiContext.setFrame(frame);
      frame.add(spectatorScreen);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.pack();
      WindowFocus.showWithoutTakingFocus(frame);
      spectatorScreen.updateLobbySnapshot(createLobbySnapshot());
    });
  }

  private void updateSpectatorLobbySnapshot(LobbySnapshot snapshot) {
    Optional.ofNullable(spectatorScreen)
        .ifPresent(screen -> SwingUtilities.invokeLater(() -> screen.updateLobbySnapshot(snapshot)));
  }

  private void updateSpectatorSnapshot(MatchSnapshot snapshot) {
    Game game = spectatorGame;
    if (game == null) {
      return;
    }
    SwingUtilities.invokeLater(() -> {
      if (spectatorScreen != null) {
        spectatorScreen.markGameStarted();
      }
      game.loadSnapshot(snapshot);
    });
  }

  private Optional<MatchSnapshot> tryLoadSavedSnapshot(MissionOption mission) {
    if (!Files.exists(savePath)) {
      return Optional.empty();
    }
    try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(savePath))) {
      Object object = in.readObject();
      if (!(object instanceof MatchSnapshot snapshot)) {
        System.err.println("Ignoring saved game state because it is not a match snapshot: " + savePath);
        return Optional.empty();
      }
      if (!snapshot.config().equals(config)) {
        System.err.println("Ignoring saved game state because it was created for " +
            snapshot.config().rebelPlayerCount() + " rebel player(s), not " + config.rebelPlayerCount() + ".");
        return Optional.empty();
      }
      MissionOption snapshotMission = snapshot.mission() == null ? MissionOption.MISSION_ONE : snapshot.mission();
      if (snapshotMission != mission) {
        System.err.println("Ignoring saved game state because it was created for " +
            snapshotMission.displayName() + ", not " + mission.displayName() + ".");
        return Optional.empty();
      }
      return Optional.of(snapshot);
    } catch (IOException | ClassNotFoundException ex) {
      System.err.println("Unable to load saved game state from " + savePath + ": " + ex.getMessage());
      return Optional.empty();
    }
  }

  private Path savePathForMission(MissionOption mission) {
    String missionKey = mission.displayName().toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    return Path.of("server-game-state-" + missionKey + ".ser");
  }

  private void saveSnapshot(MatchSnapshot snapshot) {
    synchronized (saveLock) {
      try {
        Path parent = savePath.toAbsolutePath().getParent();
        if (parent != null) {
          Files.createDirectories(parent);
        }
        Path tempPath = savePath.resolveSibling(savePath.getFileName() + ".tmp");
        try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(tempPath))) {
          out.writeObject(snapshot);
        }
        try {
          Files.move(tempPath, savePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
          Files.move(tempPath, savePath, StandardCopyOption.REPLACE_EXISTING);
        }
      } catch (IOException ex) {
        System.err.println("Unable to save game state to " + savePath + ": " + ex.getMessage());
      }
    }
  }

  private class RemoteDecisionProvider implements GameDecisionProvider {
    private final Game game;

    private RemoteDecisionProvider(Game game) {
      this.game = game;
    }

    @Override
    public int chooseMultipleChoice(PlayerSeat seat, String name, String explanation, Object[] options) {
      if (options.length == 1 && !shouldPromptSingleChoice(name)) {
        return 0;
      }
      MyArrayList<String> labels = new MyArrayList<>();
      for (Object option : options) {
        labels.add(String.valueOf(option));
      }
      RemotePrompt prompt = new RemotePrompt(nextPromptId(), seat, RemotePrompt.PromptType.MULTIPLE_CHOICE,
          name, explanation, labels, 0, labels.size() - 1, labels, null, null);
      return parseBoundedIntResponse(requestResponse(prompt), 0, options.length - 1, prompt);
    }

    private boolean shouldPromptSingleChoice(String name) {
      return "Deployment Selection".equals(name);
    }

    @Override
    public boolean chooseYesNo(PlayerSeat seat, String name, String explanation) {
      RemotePrompt prompt = new RemotePrompt(nextPromptId(), seat, RemotePrompt.PromptType.YES_NO,
          name, explanation, MyArrayList.of("No", "Yes"), 0, 1, MyArrayList.of("false", "true"), null, null);
      return Boolean.parseBoolean(requestResponse(prompt));
    }

    @Override
    public int chooseNumericChoice(PlayerSeat seat, String name, int minValue, int maxValue) {
      if (minValue == maxValue) {
        return minValue;
      }
      RemotePrompt prompt = new RemotePrompt(nextPromptId(), seat, RemotePrompt.PromptType.NUMERIC,
          name, name + " (" + minValue + " to " + maxValue + ")", MyArrayList.of(), minValue, maxValue,
          MyArrayList.of(),
          null, null);
      return parseBoundedIntResponse(requestResponse(prompt), minValue, maxValue, prompt);
    }

    @Override
    public Directions chooseDirection(PlayerSeat seat, Personnel activeFigure,
        MyArrayList<Directions> allowedDirections) {
      return chooseMovement(seat, activeFigure, allowedDirections, new MyArrayList<>()).direction().orElseThrow();
    }

    @Override
    public MovementChoice chooseMovement(PlayerSeat seat, Personnel activeFigure,
        MyArrayList<Directions> allowedDirections, MyArrayList<RotationMove> legalRotations) {
      if (allowedDirections.size() == 1 && legalRotations.isEmpty()) {
        return MovementChoice.direction(allowedDirections.get(0));
      }
      if (allowedDirections.isEmpty() && legalRotations.size() == 1) {
        return MovementChoice.rotate(legalRotations.get(0));
      }
      MyArrayList<String> values = new MyArrayList<>();
      for (Directions direction : allowedDirections) {
        values.add(direction.name());
      }
      for (RotationMove rotationMove : legalRotations) {
        values.add(rotationMove.token());
      }
      RemotePrompt prompt = new RemotePrompt(nextPromptId(), seat, RemotePrompt.PromptType.DIRECTION,
          "Movement", "Choose a direction", values, 0, 0, values, activeFigure.getId(), null);
      String response = requestResponse(prompt);
      if ("ROTATE".equals(response)) {
        return MovementChoice.rotate(legalRotations.get(0));
      }
      if (response == null || response.isBlank()) {
        System.err.println("Prompt " + prompt.promptId() + " returned no movement; using first available option.");
        return firstMovementChoice(allowedDirections, legalRotations);
      }
      if (RotationMove.isToken(response)) {
        return MovementChoice.rotate(RotationMove.fromToken(response));
      }
      try {
        return MovementChoice.direction(Directions.valueOf(response));
      } catch (IllegalArgumentException ex) {
        System.err.println("Prompt " + prompt.promptId() + " returned invalid movement " + response
            + "; using first available option.");
        return firstMovementChoice(allowedDirections, legalRotations);
      }
    }

    @Override
    public Personnel chooseTarget(PlayerSeat seat, SelectionType selectionType,
        MyArrayList<Personnel> availableTargets) {
      if (availableTargets.size() == 1) {
        return availableTargets.get(0);
      }
      MyArrayList<String> values = new MyArrayList<>();
      MyArrayList<String> labels = new MyArrayList<>();
      for (Personnel target : availableTargets) {
        values.add(target.getId());
        labels.add(target.getName());
      }
      RemotePrompt prompt = new RemotePrompt(nextPromptId(), seat, RemotePrompt.PromptType.TARGET,
          "Target Selection", "Choose a target", labels, 0, 0, values, null, selectionType);
      String response = requestResponse(prompt);
      if (response == null || response.isBlank()) {
        System.err.println("Prompt " + prompt.promptId() + " returned no target; using first available target.");
        return availableTargets.get(0);
      }
      return game.getPersonnelById(response)
          .orElseGet(() -> {
            System.err.println("Unknown target id " + response + "; using first available target.");
            return availableTargets.get(0);
          });
    }

    private MovementChoice firstMovementChoice(MyArrayList<Directions> allowedDirections,
        MyArrayList<RotationMove> legalRotations) {
      return !allowedDirections.isEmpty()
          ? MovementChoice.direction(allowedDirections.get(0))
          : MovementChoice.rotate(legalRotations.get(0));
    }

    private int parseBoundedIntResponse(String response, int minValue, int maxValue, RemotePrompt prompt) {
      if (response == null || response.isBlank()) {
        System.err.println("Prompt " + prompt.promptId() + " returned no value; using " + minValue + ".");
        return minValue;
      }
      try {
        int value = Integer.parseInt(response);
        if (value < minValue || value > maxValue) {
          System.err.println("Prompt " + prompt.promptId() + " returned out-of-range value " + value
              + "; using " + minValue + ".");
          return minValue;
        }
        return value;
      } catch (NumberFormatException ex) {
        System.err.println("Prompt " + prompt.promptId() + " returned non-numeric value " + response
            + "; using " + minValue + ".");
        return minValue;
      }
    }

    private String requestResponse(RemotePrompt prompt) {
      ClientConnection connection = Optional.ofNullable(clients.get(prompt.seat()))
          .orElseThrow(() -> new CancellationException("No client connected for " + prompt.seat()));
      Thread waitingThread = Thread.currentThread();
      game.setActivePromptCancelAction(() -> {
        connection.send(new RemotePromptCancel(prompt.promptId()));
        waitingThread.interrupt();
      });
      try {
        sendSnapshotToClients(game.createSnapshot());
        sendPromptBannerToOtherClients(prompt);
        connection.send(prompt);
        PromptResponse response;
        do {
          response = connection.takeResponse(prompt.promptId());
        } while (response.promptId() != prompt.promptId());
        return response.value();
      } finally {
        game.clearActivePromptCancelAction();
      }
    }
  }

  private class ClientConnection {
    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private final ResponseQueue responses = new ResponseQueue();
    private PlayerSeat seat;
    private MissionOption selectedMission;

    private ClientConnection(Socket socket) throws IOException {
      this.socket = socket;
      this.out = new ObjectOutputStream(socket.getOutputStream());
      this.out.flush();
      this.in = new ObjectInputStream(socket.getInputStream());
    }

    private void startReader() {
      Thread readerThread = new Thread(() -> {
        try {
          while (true) {
            Object object = in.readObject();
            if (object instanceof PromptResponse response) {
              responses.put(response);
            } else if (object instanceof ClientMissionSelection clientMissionSelection) {
              handleClientMissionSelection(this, clientMissionSelection);
            } else if (object instanceof ClientFinishGameRequest) {
              handleClientFinishGameRequest();
            }
          }
        } catch (EOFException | SocketException eof) {
        } catch (IOException | ClassNotFoundException ex) {
          ex.printStackTrace(System.err);
        }
      });
      readerThread.setDaemon(true);
      readerThread.start();
    }

    private void send(Object object) {
      synchronized (out) {
        try {
          out.writeObject(object);
          out.flush();
          out.reset();
        } catch (SocketException ex) {
          // The headless smoke bots and closed clients may disconnect after they
          // have received enough state. Treat that as a normal send outcome.
        } catch (IOException ex) {
          ex.printStackTrace(System.err);
        }
      }
    }

    private Optional<MissionOption> selectedMission() {
      return Optional.ofNullable(selectedMission);
    }

    private void selectMission(MissionOption mission) {
      selectedMission = mission;
    }

    private PromptResponse takeResponse(long promptId) {
      try {
        return responses.take();
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw new CancellationException("Prompt " + promptId + " was interrupted");
      }
    }
  }

  private long nextPromptId() {
    return nextPromptId++;
  }

  private static final class ResponseQueue {
    private final MyDLList<PromptResponse> values = new MyDLList<>();

    synchronized void put(PromptResponse response) {
      values.add(response);
      notifyAll();
    }

    synchronized PromptResponse take() throws InterruptedException {
      while (values.isEmpty()) {
        wait();
      }
      return values.remove(0);
    }
  }
}
