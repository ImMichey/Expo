package dev.michey.expo.server.main.logic.inventory.item.mapping;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.logic.inventory.item.mapping.client.ItemRender;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Random;

import static dev.michey.expo.log.ExpoLogger.log;

public class ItemMapper {

    private static ItemMapper INSTANCE;

    private final HashMap<String, ItemMapping> itemMappings;
    private final HashMap<Integer, ItemMapping> itemMappingsId;

    public ItemMapper(boolean client) {
        itemMappings = new HashMap<>();
        itemMappingsId = new HashMap<>();

        FileHandle fh = Gdx.files.internal("items.json");
        JSONArray db = new JSONObject(fh.readString()).getJSONArray("database");

        for(int i = 0; i < db.length(); i++) {
            JSONObject entry = db.getJSONObject(i);

            // keys.
            String identifier = entry.getString("identifier");
            String displayName = entry.getString("displayName");
            String displayNameColor = entry.getString("displayNameColor");
            int id = entry.getInt("id");

            ItemLogic logic = new ItemLogic(entry.getJSONObject("logic"));
            ItemRender uiRender;
            ItemRender heldRender;
            ItemRender armorRender;

            if(!client) {
                // it's a dedicated server with no client presence, skip ItemRender
                uiRender = null;
                heldRender = null;
                armorRender = null;
            } else {
                if(entry.getJSONObject("render").has("universal")) {
                    uiRender = new ItemRender(entry.getJSONObject("render").getJSONObject("universal"));
                    heldRender = new ItemRender(entry.getJSONObject("render").getJSONObject("universal"));
                    armorRender = new ItemRender(entry.getJSONObject("render").getJSONObject("universal"));
                } else {
                    uiRender = new ItemRender(entry.getJSONObject("render").getJSONObject("ui"));
                    heldRender = new ItemRender(entry.getJSONObject("render").getJSONObject("held"));
                    armorRender = new ItemRender(entry.getJSONObject("render").getJSONObject("armor"));
                }
            }

            ItemMapping mapping = new ItemMapping(identifier, id, displayName, displayNameColor, uiRender, heldRender, armorRender, logic);
            itemMappings.put(identifier, mapping);
            itemMappingsId.put(id, mapping);

            log("Added item mapping [" + id + ", " + identifier + ", itemRenderPresent=" + (uiRender != null) + "]");
        }

        INSTANCE = this;
    }

    public ItemMapping randomMapping() {
        return itemMappingsId.values().stream().skip(new Random().nextInt(itemMappings.size())).findFirst().orElse(null);
    }

    public Collection<ItemMapping> getItemMappings() {
        return itemMappings.values();
    }

    public ItemMapping getMapping(String identifier) {
        return itemMappings.get(identifier);
    }

    public ItemMapping getMapping(int id) {
        var test = itemMappingsId.get(id);
        if(test == null) {
            log("NULL MAPPING " + id);
        }
        return test;
    }

    public static ItemMapper get() {
        return INSTANCE;
    }

}