package dev.michey.expo.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.util.Pair;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class SoundGroupDatabase {

    private List<String> qualifiedSoundMap;
    private HashMap<String, Pair<Sound, Float>> soundMap;
    private HashMap<String, Float> customVolumeMap;

    private SoundGroupType type;
    private String groupName;
    private float minPitch;
    private float maxPitch;
    private float volumeMultiplier;

    private int currentPlayIndex;

    public SoundGroupDatabase(SoundGroupType type, String groupName, float minPitch, float maxPitch, float volumeMultiplier) {
        this.type = type;
        this.groupName = groupName;
        this.minPitch = minPitch;
        this.maxPitch = maxPitch;
        this.volumeMultiplier = volumeMultiplier;

        qualifiedSoundMap = new LinkedList<>();
        soundMap = new HashMap<>();
        customVolumeMap = new HashMap<>();
    }

    public void addSound(String line, float volume) {
        // e.g. wood_cut_1.wav
        qualifiedSoundMap.add(line);

        // Create GDX sound instance + fill duration map
        Sound sound = createSound(line);
        float duration = createSoundDuration(line);
        soundMap.put(line, new Pair<>(sound, duration));

        // Insert custom volume if existing
        if(volume != -1.0f) {
            customVolumeMap.put(line, volume);
        }
    }

    public TrackedSoundData playSound(float volumeAbsolute, boolean loop) {
        String currentSound = qualifiedSoundMap.get(currentPlayIndex);
        TrackedSoundData data = playSoundSpecific(currentSound, volumeAbsolute, loop);

        currentPlayIndex++;
        if(currentPlayIndex == qualifiedSoundMap.size()) currentPlayIndex = 0;
        return data;
    }

    public TrackedSoundData playSoundSpecific(String soundName, float volumeAbsolute, boolean loop) {
        float pitch = (minPitch == maxPitch) ? minPitch : MathUtils.random(minPitch, maxPitch);
        float specificVolumeMultiplier = customVolumeMap.containsKey(soundName) ? customVolumeMap.get(soundName) : volumeMultiplier;
        float typeVolume = AudioEngine.get().volumeOf(type);
        float volume = volumeAbsolute * typeVolume * specificVolumeMultiplier * AudioEngine.get().getMasterVolume();

        // Play sound here without panning.
        Sound sound = soundMap.get(soundName).key;
        long id;

        if(loop) {
            id = sound.loop(volume, pitch, 0);
        } else {
            id = sound.play(volume, pitch, 0);
        }

        TrackedSoundData data = new TrackedSoundData();
        data.sound = sound;
        data.id = id;
        data.qualifiedName = soundName;
        data.baseVolume = volumeAbsolute;
        data.typeVolume = typeVolume;
        data.specificVolumeMultiplier = specificVolumeMultiplier;
        data.type = type;
        data.beginTimestamp = System.currentTimeMillis();
        data.loop = loop;

        return data;
    }

    public float getSoundDuration(String name) {
        return soundMap.get(name).value;
    }

    private float createSoundDuration(String name) {
        float duration = -1;

        if(name.endsWith(".wav")) {
            try {
                InputStream bufferedInputStream = new BufferedInputStream(Gdx.files.internal("sounds/" + name).read());
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bufferedInputStream);
                AudioFormat format = audioInputStream.getFormat();

                long frames = audioInputStream.getFrameLength();
                duration = frames / format.getFrameRate();
            } catch (UnsupportedAudioFileException | IOException e) {
                e.printStackTrace();
            }
        }

        return duration;
    }

    private Sound createSound(String qualifiedName) {
        return Gdx.audio.newSound(Gdx.files.internal("sounds/" + qualifiedName));
    }

    public SoundGroupType getType() {
        return type;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getRandomQualifiedName() {
        return qualifiedSoundMap.get(MathUtils.random(qualifiedSoundMap.size() - 1));
    }

    public float getMinPitch() {
        return minPitch;
    }

    public float getMaxPitch() {
        return maxPitch;
    }

    public float getVolumeMultiplier() {
        return volumeMultiplier;
    }

}
