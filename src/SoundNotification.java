import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class SoundNotification {
    private static String soundFilePath;
    private AudioInputStream audioStream;
    private Clip clip;

    SoundNotification() {
        // soundFilePath = "./static/new-notification.mp3";
        soundFilePath = "./static/message-notification.wav";
        try {
            audioStream = AudioSystem.getAudioInputStream(new File(soundFilePath).getAbsoluteFile());
        } catch (UnsupportedAudioFileException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            clip = AudioSystem.getClip();
        } catch (LineUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            clip.open(audioStream);
        } catch (LineUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void play() {
        clip.start();
    }

}
