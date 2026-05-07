package src.game;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class LoaderUtils {
    private static URL getResourceUrl(String resourcePath) {
        URL resource = LoaderUtils.class.getClassLoader().getResource(resourcePath);
        if (resource == null) {
            throw new RuntimeException("Missing resource: " + resourcePath);
        }
        return resource;
    }

    public static BufferedImage getImage(String name) {
        try {
            return ImageIO.read(getResourceUrl("game/images/" + name + ".jpg"));
        } catch (IOException ex) {
            try {
                return ImageIO.read(getResourceUrl("game/images/" + name + ".png"));
            } catch (IOException e) {
                throw new java.lang.RuntimeException(
                        "Couldn't read either jpg or png image for " + name, ex);
            }
        }
    }

    public static ImageIcon getImageIcon(String name) {
        URL pngResource = LoaderUtils.class.getClassLoader().getResource("game/images/" + name + ".png");
        if (pngResource != null) {
            return new ImageIcon(pngResource);
        }
        URL jpgResource = LoaderUtils.class.getClassLoader().getResource("game/images/" + name + ".jpg");
        if (jpgResource != null) {
            return new ImageIcon(jpgResource);
        }
        throw new RuntimeException("Couldn't find image icon for " + name);
    }

    public static void playSound(String name) {
        try {
            Clip clip = AudioSystem.getClip();
            clip.open(AudioSystem.getAudioInputStream(getResourceUrl("game/sounds/" + name + ".wav")));
            clip.start();
        } catch (Exception exc) {
            exc.printStackTrace(System.out);
        }
    }
}
