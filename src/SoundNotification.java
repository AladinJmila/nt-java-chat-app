import java.io.File;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

// Handles sound notifications by playing a WAV audio file
public class SoundNotification {
    private static final String SOUND_FILE_PATH = "./message-notification.wav";
    private Clip clip;

    // Initializes the audio clip by loading the sound file
    SoundNotification() {
        try (AudioInputStream audioStream = AudioSystem
                .getAudioInputStream(new File(SOUND_FILE_PATH).getAbsoluteFile())) {
            this.clip = AudioSystem.getClip();
            this.clip.open(audioStream);
        } catch (Exception e) {
            System.err.println(e);
            clip = null;
        }
    }

    // Plays the notification sound from the beginning if the clip is available
    public void play() {
        if (clip != null) {
            clip.setFramePosition(0);
            clip.start();
        }
    }

}
