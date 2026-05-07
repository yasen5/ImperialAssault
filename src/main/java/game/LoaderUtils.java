package game;

import java.awt.image.BufferedImage;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.swing.ImageIcon;

public class LoaderUtils {
    public static BufferedImage getImage(String name) {
        URL resource = findImageResource(name);
        if (resource == null) {
            throw new RuntimeException("Couldn't find image resource for " + name);
        }
        try {
            return ImageIO.read(resource);
        } catch (Exception ex) {
            throw new RuntimeException("Couldn't read image resource for " + name, ex);
        }
    }

    public static ImageIcon getImageIcon(String name) {
        URL resource = findImageResource(name);
        if (resource == null) {
            throw new RuntimeException("Couldn't find image icon resource for " + name);
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

    public static void playSound(String name) {
        try {
            Clip clip = AudioSystem.getClip();
            clip.open(AudioSystem.getAudioInputStream(LoaderUtils.class.getResource("/sounds/" + name + ".wav")));
            clip.start();
        } catch (Exception exc) {
            exc.printStackTrace(System.out);
        }
    }
}
