import javax.sound.sampled.*;
import java.io.File;

public class Sound { // handles all the sound effects and background music in the game

    private static Clip background = null;
    private static String[] playlist = {};
    private static int track = 0;
    private static float volume = 0.75f;
    private static boolean skipping = false;

    public static void setPlaylist(String[] files) { // sets the playlist
        playlist = files;
        track = 0;
    }

    public static void playBgMusic(String filename) { //plays background music
        stopBgMusic();
        try {
            AudioInputStream input = AudioSystem.getAudioInputStream(new File(filename));
            background = AudioSystem.getClip();
            background.open(input);
            background.start();
            applyVolume(background);
            background.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP && background != null && !skipping) {
                    playNext();
                }
            });
        } catch (Exception e) {
            System.out.println("BGM Error: " + e.getMessage());
        }
    }

    public static void playPlaylist(String[] files) { // convenience method to set the playlist and start playing the first track
        setPlaylist(files);
        if (playlist.length > 0) {
            playBgMusic(playlist[0]);
        }
    }

    public static void nextTrack() { // skips to the next track in the playlist
        if (playlist.length == 0) return;
        skipping = true;
        track = (track + 1) % playlist.length;
        playBgMusic(playlist[track]);
        skipping = false;
    }

    private static void playNext() { // automatically plays the next track when the current one finishes
        if (playlist.length == 0) return;
        track = (track + 1) % playlist.length;
        playBgMusic(playlist[track]);
    }

    public static void stopBgMusic() { // stops the background music
        if (background != null) {
            Clip clip = background;
            background = null; // null first so the LineListener doesn't fire playNext()
            if (clip.isRunning()) clip.stop();
            clip.close();
        }
    }

    public static void setVolume(float vol) { // sets the volume for the music
        volume = Math.max(0f, Math.min(1f, vol));
        applyVolume(background);
    }

    private static void applyVolume(Clip clip) { // applies volume to the audio clip
        if (clip == null) return;
        try {
            FloatControl fc = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = volume <= 0 ? fc.getMinimum() : (float)(Math.log10(volume) * 20);
            fc.setValue(Math.max(fc.getMinimum(), Math.min(fc.getMaximum(), dB)));
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            // MASTER_GAIN not supported on this clip/format — skip silently
        }
    }

    public static void sfx(String filename) { // plays a sound effect
        try {
            AudioInputStream input = AudioSystem.getAudioInputStream(new File(filename));
            Clip clip = AudioSystem.getClip();
            clip.open(input);
            applyVolume(clip);
            clip.start();
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) clip.close();
            });
        } catch (Exception e) {
            System.out.println("SFX Error: " + e.getMessage());
        }
    }
}