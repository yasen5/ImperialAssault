package game;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.ImageIcon;

public class LoaderUtils {
    private static final int PLACEHOLDER_SIZE = 96;

    public static BufferedImage getImage(String name) {
        BufferedImage image = getImageIfPresent(name);
        return image == null ? createPlaceholderImage(name) : image;
    }

    public static BufferedImage getImageIfPresent(String name) {
        URL resource = findImageResource(name);
        if (resource == null) {
            return null;
        }
        try {
            return ImageIO.read(resource);
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
            return null;
        }
    }

    public static ImageIcon getImageIcon(String name) {
        URL resource = findImageResource(name);
        if (resource == null) {
            return new ImageIcon();
        }
        return new ImageIcon(resource);
    }

    private static URL findImageResource(String name) {
        URL resource = LoaderUtils.class.getResource("/images/" + name + ".jpg");
        if (resource != null) {
            return resource;
        }
        return LoaderUtils.class.getResource("/images/" + name + ".png");
    }

    private static BufferedImage createPlaceholderImage(String name) {
        BufferedImage image = new BufferedImage(PLACEHOLDER_SIZE, PLACEHOLDER_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(40, 40, 40));
        g.fillRect(0, 0, PLACEHOLDER_SIZE, PLACEHOLDER_SIZE);
        g.setColor(new Color(180, 40, 40));
        g.drawLine(0, 0, PLACEHOLDER_SIZE, PLACEHOLDER_SIZE);
        g.drawLine(PLACEHOLDER_SIZE, 0, 0, PLACEHOLDER_SIZE);
        g.setColor(Color.WHITE);
        g.drawString(name == null ? "missing" : name, 8, PLACEHOLDER_SIZE / 2);
        g.dispose();
        return image;
    }

    public static void playSound(String name) {
        try {
            URL resource = LoaderUtils.class.getResource("/sounds/" + name + ".wav");
            if (resource == null) {
                System.err.println("Missing sound resource: " + name);
                return;
            }
            Clip clip = AudioSystem.getClip();
            clip.open(AudioSystem.getAudioInputStream(resource));
            clip.start();
        } catch (IOException | LineUnavailableException | UnsupportedAudioFileException | IllegalArgumentException exc) {
            exc.printStackTrace(System.out);
        }
    }
}
