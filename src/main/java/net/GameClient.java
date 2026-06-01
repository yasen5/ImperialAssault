package net;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.ConnectException;
import java.net.Socket;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.Optional;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import net.structs.MissionOption;
import visual.Screen;
import game.Game;
import game.PlayerSeat;
import visual.UiContext;
import net.structs.LobbySnapshot;
import net.structs.JoinRequest;
import net.structs.JoinResponse;
import net.structs.MatchSnapshot;
import net.structs.RemotePrompt;
import net.structs.RemotePromptCancel;
import net.structs.RemoteBanner;
import net.structs.PromptResponse;
import net.structs.ClientMissionSelection;
import net.structs.ClientFinishGameRequest;
import visual.WindowFocus;

public class GameClient {
  private static final long CONNECT_RETRY_DELAY_MS = 500L;

  private final String host;
  private final int port;
  private final PlayerSeat requestedSeat;
  private final boolean debugWallLines;
  private ObjectOutputStream out;
  private Screen screen;
  private Game game;

  public GameClient(String host, int port, PlayerSeat requestedSeat) {
    this(host, port, requestedSeat, false);
  }

  public GameClient(String host, int port, PlayerSeat requestedSeat, boolean debugWallLines) {
    this.host = host;
    this.port = port;
    this.requestedSeat = requestedSeat;
    this.debugWallLines = debugWallLines;
  }

  public void run() throws IOException, ClassNotFoundException, InterruptedException, InvocationTargetException {
    Socket socket = connectWhenAvailable();
    out = new ObjectOutputStream(socket.getOutputStream());
    out.flush();
    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
    send(new JoinRequest(requestedSeat));
    JoinResponse response = (JoinResponse) in.readObject();
    if (!response.accepted()) {
      System.err.println(response.message());
      return;
    }
    game = new Game(null, response.config(), null, false);
    game.setDebugWallLines(debugWallLines);
    SwingUtilities.invokeAndWait(() -> {
      screen = new Screen(game, true);
      game.setUi(screen);
      screen.setLocalSeat(response.seat());
      screen.setMissionSelectionAction((MissionOption mission) -> {
        send(new ClientMissionSelection(mission));
      });
      screen.setFinishGameAction(() -> {
        send(new ClientFinishGameRequest());
      });
      Optional.ofNullable(response.lobbySnapshot()).ifPresent(screen::updateLobbySnapshot);
      JFrame frame = new JFrame("Imperial Assault Client - " + response.seat());
      UiContext.setFrame(frame);
      frame.add(screen);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.pack();
      WindowFocus.showWithoutTakingFocus(frame);
    });
    while (true) {
      Object message = in.readObject();
      if (message instanceof LobbySnapshot lobbySnapshot) {
        SwingUtilities.invokeLater(() -> {
          screen.updateLobbySnapshot(lobbySnapshot);
          screen.repaint();
        });
        continue;
      }
      if (message instanceof MatchSnapshot snapshot) {
        SwingUtilities.invokeLater(() -> {
          screen.markGameStarted();
          game.loadSnapshot(snapshot);
          screen.repaint();
        });
      } else if (message instanceof RemotePrompt prompt) {
        new Thread(() -> handlePrompt(prompt), "Remote Prompt").start();
      } else if (message instanceof RemotePromptCancel cancel) {
        SwingUtilities.invokeLater(() -> screen.cancelPrompt(cancel.promptId()));
      } else if (message instanceof RemoteBanner banner) {
        SwingUtilities.invokeLater(() -> screen.showBanner(banner.text()));
      }
    }
  }

  private Socket connectWhenAvailable() throws IOException, InterruptedException {
    while (true) {
      try {
        return new Socket(host, port);
      } catch (ConnectException ex) {
        try {
          Thread.sleep(CONNECT_RETRY_DELAY_MS);
        } catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          throw interrupted;
        }
      }
    }
  }

  private void handlePrompt(RemotePrompt prompt) {
    try {
      String value = switch (prompt.type()) {
        case MULTIPLE_CHOICE -> String.valueOf(
            screen.promptMultipleChoice(prompt.promptId(), prompt.title(), prompt.message(),
                prompt.optionLabels().toArray()));
        case YES_NO -> String.valueOf(screen.promptYesNo(prompt.promptId(), prompt.title(), prompt.message()));
        case NUMERIC -> String.valueOf(
            screen.promptNumericChoice(prompt.promptId(), prompt.title(), prompt.message(), prompt.minValue(),
                prompt.maxValue()));
        case DIRECTION, TARGET -> {
          CompletableFuture<String> selection = callOnEventThread(() -> screen.beginRemoteBoardPrompt(prompt));
          String result = selection.join();
          yield result;
        }
      };
      send(new PromptResponse(prompt.promptId(), value));
    } catch (java.util.concurrent.CancellationException ex) {
      SwingUtilities.invokeLater(() -> screen.cancelPrompt(prompt.promptId()));
    }
  }

  private void send(Object object) {
    synchronized (out) {
      try {
        out.writeObject(object);
        out.flush();
        out.reset();
      } catch (IOException ex) {
        ex.printStackTrace(System.err);
      }
    }
  }

  private static <T> T callOnEventThread(Callable<T> task) {
    if (SwingUtilities.isEventDispatchThread()) {
      try {
        return task.call();
      } catch (Exception ex) {
        throw new IllegalStateException(ex);
      }
    }
    CompletableFuture<T> result = new CompletableFuture<>();
    try {
      SwingUtilities.invokeAndWait(() -> {
        try {
          result.complete(task.call());
        } catch (Exception ex) {
          result.completeExceptionally(ex);
        }
      });
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new java.util.concurrent.CancellationException("Interrupted while waiting for the UI");
    } catch (InvocationTargetException ex) {
      Throwable cause = ex.getCause();
      if (cause instanceof RuntimeException runtimeException) {
        throw runtimeException;
      }
      if (cause instanceof Error error) {
        throw error;
      }
      throw new IllegalStateException(cause);
    }
    return result.join();
  }
}
