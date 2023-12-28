package dev.michey.expo.server.main.logic.inventory.item.mapping;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

import static dev.michey.expo.log.ExpoLogger.log;

public class ItemMapper {

    private static ItemMapper INSTANCE;

    private final HashMap<String, ItemMapping> itemMappings;
    private final HashMap<Integer, ItemMapping> itemMappingsId;
    private final List<ItemRender> dynamicAnimationList;
    private final List<ItemRender> dynamicParticleEmitterList;

    public ItemMapper(boolean client, boolean reload) {
        itemMappings = new HashMap<>();
        itemMappingsId = new HashMap<>();
        dynamicAnimationList = new LinkedList<>();
        dynamicParticleEmitterList = new LinkedList<>();

        FileHandle fh = reload ? Gdx.files.absolute("C:\\IDEAProjects\\Expo\\assets_shared\\items.json") : Gdx.files.internal("items.json");
        JSONArray db = new JSONObject(fh.readString()).getJSONArray("database");

        for(int i = 0; i < db.length(); i++) {
            JSONObject entry = db.getJSONObject(i);

            // keys.
            String identifier = entry.getString("identifier");
            String displayName = entry.getString("displayName");
            String displayNameColor = entry.getString("displayNameColor");
            ItemCategory category = ItemCategory.valueOf(entry.getString("category"));
            int id = entry.getInt("id");

            ItemLogic logic = new ItemLogic(entry.getJSONObject("logic"));
            ItemRender[] uiRender;
            ItemRender[] heldRender;
            ItemRender[] armorRender;

            if(!client) {
                // it's a dedicated server with no client presence, skip ItemRender
                uiRender = null;
                heldRender = null;
                armorRender = null;
            } else {
                JSONObject renderData = entry.getJSONObject("render");

                if(renderData.has("universal")) {
                    ItemRender[] ir = convertToRenderArray(renderData.get("universal"));
                    uiRender = ir;
                    heldRender = ir;
                    armorRender = ir;
                } else {
                    uiRender = convertToRenderArray(renderData.get("ui"));
                    heldRender = convertToRenderArray(renderData.get("held"));
                    if(renderData.has("armor")) {
                        armorRender = convertToRenderArray(renderData.get("armor"));
                    } else {
                        armorRender = null;
                    }
                }
            }

            ItemMapping mapping = new ItemMapping(identifier, id, category, displayName, displayNameColor, uiRender, heldRender, armorRender, logic);
            itemMappings.put(identifier, mapping);
            itemMappingsId.put(id, mapping);

            if(uiRender != null) {
                for(ItemRender ir : uiRender) {
                    if(ir.animationFrames > 0) {
                        dynamicAnimationList.add(ir);
                    }
                    if(ir.particleEmitter != null) {
                        dynamicParticleEmitterList.add(ir);
                    }
                }
            }
            if(heldRender != null) {
                for(ItemRender ir : heldRender) {
                    if(ir.animationFrames > 0) {
                        dynamicAnimationList.add(ir);
                    }
                    if(ir.particleEmitter != null) {
                        dynamicParticleEmitterList.add(ir);
                    }
                }
            }
            if(armorRender != null) {
                for(ItemRender ir : armorRender) {
                    if(ir.animationFrames > 0) {
                        dynamicAnimationList.add(ir);
                    }
                    if(ir.particleEmitter != null) {
                        dynamicParticleEmitterList.add(ir);
                    }
                }
            }
        }

        log("Added " + itemMappings.size() + " item mappings.");
        INSTANCE = this;
    }

    private ItemRender[] convertToRenderArray(Object object) {
        ItemRender[] ir;

        if(object instanceof JSONArray ja) {
            ir = new ItemRender[ja.length()];

            for(int j = 0; j < ja.length(); j++) {
                JSONObject _o = ja.getJSONObject(j);
                ir[j] = new ItemRender(_o);
            }
        } else {
            ItemRender universalRender = new ItemRender((JSONObject) object);
            ir = new ItemRender[] {universalRender};
        }

        return ir;
    }

    public ItemMapping randomMapping() {
        return itemMappingsId.values().stream().skip(new Random().nextInt(itemMappings.size())).findFirst().orElse(null);
    }

    public List<ItemRender> getDynamicAnimationList() {
        return dynamicAnimationList;
    }

    public List<ItemRender> getDynamicParticleEmitterList() {
        return dynamicParticleEmitterList;
    }

    public Collection<ItemMapping> getItemMappings() {
        return itemMappings.values();
    }

    public ItemMapping getMapping(String identifier) {
        return itemMappings.get(identifier);
    }

    public ItemMapping getMapping(int id) {
        return itemMappingsId.get(id);
    }

    public static ItemMapper get() {
        return INSTANCE;
    }

}