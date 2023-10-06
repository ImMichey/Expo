package dev.michey.expo.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.render.RenderContext;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static dev.michey.expo.log.ExpoLogger.log;

public class AudioEngine {

    /** Singleton */
    private static AudioEngine instance;
    private float masterVolume = 1.0f;

    /** Storage maps */
    private final HashMap<String, SoundGroupDatabase> soundGroupMap;
    private final HashMap<String, String> qualifiedNameMap;
    private final HashMap<SoundGroupType, Float> soundTypeVolumeMap;

    /** Tracked audio */
    private final ConcurrentHashMap<Long, TrackedSoundData> soundData;
    private final List<TrackedSoundData> clearList;
    private final HashMap<String, TrackedSoundData> ambienceTrackMap;

    private float dynamicDelta;

    public AudioEngine() {
        soundGroupMap = new HashMap<>();
        qualifiedNameMap = new HashMap<>();
        soundTypeVolumeMap = new HashMap<>();
        soundData = new ConcurrentHashMap<>();
        clearList = new LinkedList<>();
        ambienceTrackMap = new HashMap<>();
        loadMaps();
    }

    private void loadMaps() {
        String manifest = Gdx.files.internal("soundcatalog.txt").readString();
        String[] lines = manifest.split("\\r\\n");
        SoundGroupDatabase database = null;

        for(String line : lines) {
            if(line.isEmpty()) continue;
            if(line.startsWith("*")) continue;
            boolean isHeader = line.startsWith("#");

            if(isHeader) {
                float minRange;
                float maxRange;

                String[] headerArgs = line.substring(1).split(";");
                String[] ranges = headerArgs[2].split("-");
                minRange = Float.parseFloat(ranges[0]);
                maxRange = Float.parseFloat(ranges[1]);
                SoundGroupType type = SoundGroupType.valueOf(headerArgs[3]);
                float volumeMultiplier = Float.parseFloat(headerArgs[4]);
                if(minRange < 0.5) minRange = 0.5f;
                if(maxRange > 2.0) maxRange = 2.0f;

                database = new SoundGroupDatabase(type, headerArgs[0], minRange, maxRange, volumeMultiplier);
                soundGroupMap.put(headerArgs[0], database);
            } else {
                float volume = -1.0f;
                String qualifiedName = line;

                boolean hasVolume = line.contains(";");

                if(hasVolume) {
                    String[] data = line.split(";");
                    qualifiedName = data[0];
                    volume = Float.parseFloat(data[1]);
                }

                qualifiedNameMap.put(qualifiedName, database.getGroupName());
                database.addSound(qualifiedName, volume);
            }
        }

        for(SoundGroupType group : SoundGroupType.values()) {
            soundTypeVolumeMap.put(group, 1.0f);
        }

        log("Added " + qualifiedNameMap.size() + " sound entries in " + soundGroupMap.size() + " sound groups.");
    }

    public void setMasterVolume(float masterVolume) {
        this.masterVolume = masterVolume;
    }

    public float getMasterVolume() {
        return masterVolume;
    }

    /** Kills all sounds. **/
    public void killAll() {
        for(long l : soundData.keySet()) {
            soundData.get(l).sound.stop(l);
            soundData.get(l).dead = true;
        }
    }

    /** Pauses all sounds. **/
    public void pauseAll() {
        for(TrackedSoundData data : soundData.values()) data.sound.pause(data.id);
    }

    /** Resumes all sounds. **/
    public void resumeAll() {
        for(TrackedSoundData data : soundData.values()) data.sound.resume(data.id);
    }

    /** Kills a specific sound. **/
    public void killSound(long id) {
        soundData.get(id).sound.stop(id);
        soundData.get(id).dead = true;
    }

    /** The SoundEngine tick method. **/
    public void tick() {
        long now = System.currentTimeMillis();
        dynamicDelta += RenderContext.get().delta;
        boolean dynamicTick = false;

        if(dynamicDelta >= 0.1f) {
            dynamicDelta = 0;
            dynamicTick = true;
        }

        for(TrackedSoundData data : soundData.values()) {
            if(data.dead) {
                clearList.add(data);
                continue;
            }

            boolean adjust = true;

            if(!data.loop) {
                float duration = getSoundDuration(data.qualifiedName);
                long convertedDuration = (long) (duration * 1000f);

                if(now >= (data.beginTimestamp + convertedDuration)) {
                    // Sound finished, clear/un-track it.
                    clearList.add(data);
                    adjust = false;
                }
            }

            if(adjust) {
                // Adjust volumes, etc.
                if(data.dynamic) {
                    // Dynamically change volume & panning
                    if(dynamicTick) {
                        dynamicCalculate(data);
                    }
                } else {
                    float currentTypeMultiplier = volumeOf(data.type);

                    if(currentTypeMultiplier != data.typeVolume || data.ambient || data.music) {
                        // Update volume of sound.
                        data.sound.setVolume(data.id, currentTypeMultiplier * data.baseVolume * data.specificVolumeMultiplier);
                    }
                }
            }
        }

        // Un-track.
        for(TrackedSoundData clear : clearList) {
            soundData.remove(clear.id);

            if(clear.ambient) {
                ambienceTrackMap.remove(clear.ambientGroup);
            }
        }

        clearList.clear();
    }

    private void dynamicCalculate(TrackedSoundData data) {
        float volume = volumeOf(data.type) * data.baseVolume * data.specificVolumeMultiplier * masterVolume;
        volume *= getDynamicSoundVolume(data.worldPosition, data.audibleRange);
        float pan = getDynamicSoundPan(data.worldPosition, data.audibleRange);
        data.sound.setPan(data.id, pan, volume);
        data.postCalcVolume = volume;
        data.postCalcPan = pan;
    }

    /** Plays a managed sound of a certain group without panning and default volume. **/
    public TrackedSoundData playSoundGroup(String groupName) {
        return playSoundGroup(groupName, 1.0f);
    }

    /** Plays a managed sound of a certain group with a specific panning and volume. **/
    public TrackedSoundData playSoundGroup(String groupName, float volume) {
        TrackedSoundData data = soundGroupMap.get(groupName).playSound(volume, false);
        soundData.put(data.id, data);
        return data;
    }

    /** Plays a manged sound of a specific name without panning and default volume. **/
    public TrackedSoundData playSoundSpecific(String soundName) {
        return playSoundSpecific(soundName, 1.0f, false);
    }

    /** Plays a manged sound of a specific name without panning and default volume. **/
    public TrackedSoundData playSoundSpecific(String soundName, float volume, boolean loop) {
        if(!soundName.endsWith(".wav")) soundName += ".wav";
        String group = qualifiedNameMap.get(soundName);
        TrackedSoundData data = soundGroupMap.get(group).playSoundSpecific(soundName, volume, loop);
        soundData.put(data.id, data);
        return data;
    }

    public TrackedSoundData playSoundGroupManaged(String groupName, Vector2 soundOrigin, float maxAudibleRange, boolean loop) {
        return playSoundGroupManaged(groupName, soundOrigin, maxAudibleRange, loop, 1.0f);
    }

    /** Plays a managed sound of a certain group that dynamically changes its panning and volume depending on the player's distance to the origin. **/
    public TrackedSoundData playSoundGroupManaged(String groupName, Vector2 soundOrigin, float maxAudibleRange, boolean loop, float multiplier) {
        TrackedSoundData data = soundGroupMap.get(groupName).playSound(multiplier, loop);
        data.dynamic = true;
        data.worldPosition = soundOrigin;
        data.audibleRange = maxAudibleRange;
        dynamicCalculate(data);
        soundData.put(data.id, data);
        return data;
    }

    /** Plays a managed sound of a certain group that dynamically changes its panning and volume depending on the player's distance to the origin. **/
    public TrackedSoundData playSoundGroupManaged(String groupName, Vector2 soundOrigin) {
        return playSoundGroupManaged(groupName, soundOrigin, 256.0f, false);
    }

    /** Sets the volume for a certain ambience sound. **/
    public void ambientVolume(String groupName, float volume) {
        // groupName = e.g. nightambience
        if(!ambienceTrackMap.containsKey(groupName)) {
            if(volume <= 0.0f) return;
            // Add.
            String sound = soundGroupMap.get(groupName).getRandomQualifiedName();
            TrackedSoundData data = playSoundSpecific(sound, volume, true);
            data.ambient = true;
            data.ambientGroup = groupName;
            ambienceTrackMap.put(groupName, data);
            return;
        }

        if(volume <= 0.0f) {
            killSound(ambienceTrackMap.get(groupName).id);
        } else {
            // Update base volume.
            ambienceTrackMap.get(groupName).baseVolume = volume;
        }
    }

    /** Returns the volume multiplier for a certain SoundGroupType. **/
    public float volumeOf(SoundGroupType type) {
        return soundTypeVolumeMap.get(type);
    }

    /** Returns the duration of a qualified sound name. **/
    public float getSoundDuration(String soundName) {
        String group = qualifiedNameMap.get(soundName);
        return soundGroupMap.get(group).getSoundDuration(soundName);
    }

    /** Returns an instance of SoundEngine. **/
    public static AudioEngine get() {
        if(instance == null) instance = new AudioEngine();
        return instance;
    }

    /** Sets the volume multiplier for a certain SoundGroupType. **/
    public void setSoundTypeMultiplier(SoundGroupType type, float volume) {
        soundTypeVolumeMap.put(type, volume);
    }

    /** Returns the map with all tracked & managed sounds. **/
    public ConcurrentHashMap<Long, TrackedSoundData> getSoundData() {
        return soundData;
    }

    private float dstPlayer(Vector2 v) {
        ClientPlayer p = ClientPlayer.getLocalPlayer();
        if(p == null) return 0f;

        Vector2 c = new Vector2(p.finalTextureCenterX, p.finalTextureRootY);
        return Vector2.dst(v.x, v.y, c.x, c.y);
    }

    private float getDynamicSoundVolume(Vector2 v, float maxAudibleRange) {
        float d = dstPlayer(v);
        if(d > maxAudibleRange) return 0;

        return 1f - ((d * d) / (maxAudibleRange * maxAudibleRange));
    }

    private float getDynamicSoundPan(Vector2 v, float maxAudibleRange) {
        ClientPlayer p = ClientPlayer.getLocalPlayer();
        if(p == null) return 0f;

        float px = p.finalTextureCenterX;
        float dx = v.x - px;
        float MAX_PAN = 0.5f;
        return dx / maxAudibleRange * MAX_PAN;
    }

}