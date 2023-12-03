package dev.michey.expo.server.main.logic.inventory.item.mapping;

import org.json.JSONObject;

import static dev.michey.expo.util.ExpoShared.PLAYER_AUDIO_RANGE;

public class ItemRenderSoundEmitter {

    public String soundGroup;
    public boolean persistent;
    public float volumeRange;
    public float volumeMultiplier;

    public ItemRenderSoundEmitter(JSONObject parse) {
        soundGroup = parse.getString("soundGroup");
        persistent = parse.has("persistent") && parse.getBoolean("persistent");
        volumeRange = parse.has("volumeRange") ? parse.getFloat("volumeRange") : PLAYER_AUDIO_RANGE;
        volumeMultiplier = parse.has("volumeMultiplier") ? parse.getFloat("volumeMultiplier") : 1.0f;
    }

}