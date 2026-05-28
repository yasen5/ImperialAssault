package runners;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import game.Constants;
import game.DeploymentCard;
import game.DeploymentGroup;
import game.Game;
import game.Hero;
import game.Imperial;
import game.MissionDefinition;
import game.Pos;
import net.structs.GameSessionConfig;
import net.structs.MissionOption;
import visual.Screen;

public class DeploymentCardVisualTest {
  private static final int WIDTH = 1920;
  private static final int HEIGHT = 1080;
  private static final int PAINT_SETTLE_MS = 350;

  public static void main(String[] args) throws Exception {
    File outputDir = new File(args.length > 0 ? args[0] : "build/visual-test/deployment-cards");
    if (!outputDir.exists() && !outputDir.mkdirs()) {
      throw new IOException("Could not create screenshot directory: " + outputDir.getAbsolutePath());
    }

    TestWindow testWindow = createTestWindow();
    Robot robot = createRobot();
    try {
      waitForPaint(robot);
      saveFrameScreenshot(testWindow.frame(), robot, new File(outputDir, "00-all-figures-frame.png"));
      runFigureScreenshots(outputDir, testWindow, robot);
    } finally {
      SwingUtilities.invokeAndWait(() -> testWindow.frame().dispose());
    }
    System.exit(0);
  }

  private record TestWindow(Game game, Screen screen, JFrame frame) {
  }

  private static TestWindow createTestWindow() throws Exception {
    final TestWindow[] holder = new TestWindow[1];
    SwingUtilities.invokeAndWait(() -> {
      Game game = new Game(null, new GameSessionConfig(4),
          MissionDefinition.forOption(MissionOption.MISSION_TWO), null, true);
      for (DeploymentGroup<? extends Imperial> group : game.getDeploymentGroups()) {
        group.setDeployed(true);
      }
      Screen screen = new Screen(game, true, false);
      game.setUi(screen);
      screen.markGameStarted();
      screen.setPreferredSize(new Dimension(WIDTH, HEIGHT));

      JFrame frame = new JFrame("Deployment Card Visual Test - Real Mission");
      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      frame.setContentPane(screen);
      frame.pack();
      frame.setLocation(new Point(20, 20));
      frame.setVisible(true);
      holder[0] = new TestWindow(game, screen, frame);
    });
    return holder[0];
  }

  private static Robot createRobot() throws AWTException {
    Robot robot = new Robot();
    robot.setAutoDelay(60);
    return robot;
  }

  private static void runFigureScreenshots(File outputDir, TestWindow testWindow, Robot robot)
      throws Exception {
    Game game = testWindow.game();
    Screen screen = testWindow.screen();
    int index = 1;
    for (Hero hero : game.getHeroes()) {
      selectFigure(screen, hero.getPos());
      waitForPaint(robot);
      saveFrameScreenshot(testWindow.frame(), robot, new File(outputDir, fileName(index++, hero.getName())));
      assertCardOffset(game, hero.getDeploymentCard());
    }
    for (DeploymentGroup<? extends Imperial> group : game.getDeploymentGroups()) {
      group.setDeployed(true);
      int memberIndex = 1;
      for (Imperial imperial : group.getMembers()) {
        selectFigure(screen, imperial.getPos());
        waitForPaint(robot);
        saveFrameScreenshot(testWindow.frame(), robot, new File(outputDir, fileName(index++, group + "-" + memberIndex)));
        assertCardOffset(game, group.getDeploymentCard());
        memberIndex++;
      }
    }
  }

  private static void selectFigure(Screen screen, Pos pos) throws Exception {
    int x = pos.getX() * Constants.tileSize + Constants.tileSize / 2;
    int y = pos.getY() * Constants.tileSize + Constants.tileSize / 2;
    SwingUtilities.invokeAndWait(() -> {
      MouseEvent event = new MouseEvent(screen, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, x, y, 1,
          false, MouseEvent.BUTTON1);
      screen.dispatchEvent(event);
    });
  }

  private static void assertCardOffset(Game game, DeploymentCard card) {
    Rectangle bounds = card.getBounds();
    if (bounds.x <= game.getMapDrawWidth()) {
      throw new IllegalStateException("Deployment card overlaps the map edge: card x=" + bounds.x + ", map width="
          + game.getMapDrawWidth());
    }
  }

  private static void saveFrameScreenshot(JFrame frame, Robot robot, File file) throws IOException {
    Rectangle bounds = new Rectangle(frame.getLocationOnScreen(), frame.getSize());
    BufferedImage image = robot.createScreenCapture(bounds);
    ImageIO.write(image, "png", file);
  }

  private static void waitForPaint(Robot robot) {
    robot.delay(PAINT_SETTLE_MS);
  }

  private static String fileName(int index, String label) {
    return String.format("%02d-%s.png", index, label.replaceAll("[^A-Za-z0-9]+", "-").replaceAll("(^-|-$)", ""));
  }
}
