package dev.michey.expo.audio;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Vector2;

public class TrackedSoundData {

    // LibGDX sound instance
    public Sound sound;
    // LibGDX sound id
    public long id;
    // Sound name
    public String qualifiedName;
    // Sound Type
    public SoundGroupType type;
    // Base volume
    public float baseVolume;
    // Type volume (options)
    public float typeVolume;
    // If the sound has a multiplier or the group
    public float specificVolumeMultiplier;
    // Begin timestamp
    public long beginTimestamp;
    // Loop data
    public boolean loop;
    // Dead flag
    public boolean dead;
    // Ambient flag
    public String ambientGroup;
    public boolean ambient;
    // Music flag
    public boolean music;
    // Fade flag
    public float fadeDuration;
    public float fadeDelta;

    // Position specific
    public boolean dynamic = false;
    public Vector2 worldPosition = null;
    public float audibleRange = -1f;
    public float postCalcVolume = -1f;
    public float postCalcPan = -1f;

}
