package dev.michey.expo.server.main.logic.inventory.item.mapping;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import dev.michey.expo.server.util.JsonConverter;
import dev.michey.expo.util.ExpoShared;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static dev.michey.expo.log.ExpoLogger.log;

public class ItemMapper {

    private static ItemMapper INSTANCE;

    private final HashMap<String, ItemMapping> itemMappings;
    private final HashMap<Integer, ItemMapping> itemMappingsId;
    private final HashMap<String, ItemMapping> aliasMappings;
    private final List<ItemRender> dynamicAnimationList;
    private final List<ItemRender> dynamicParticleEmitterList;

    public ItemMapper(boolean client, boolean reload) {
        itemMappings = new HashMap<>();
        itemMappingsId = new HashMap<>();
        aliasMappings = new HashMap<>();
        dynamicAnimationList = new LinkedList<>();
        dynamicParticleEmitterList = new LinkedList<>();

        FileHandle fh = reload ? Gdx.files.absolute("C:\\IDEAProjects\\Expo\\assets_shared\\items.json") : Gdx.files.internal("items.json");
        if(!fh.exists()) return;

        JSONArray db = new JSONObject(fh.readString()).getJSONArray("database");

        for(int i = 0; i < db.length(); i++) {
            JSONObject entry = db.getJSONObject(i);

            // keys.
            String identifier = entry.getString("identifier");
            String displayName = entry.getString("displayName");
            String displayNameColor = entry.getString("displayNameColor");
            String[] aliases = entry.has("aliases") ? JsonConverter.pullStrings(entry.getJSONArray("aliases")) : null;
            ItemCategory category = ItemCategory.valueOf(entry.getString("category"));
            int id = entry.getInt("id");

            ItemLogic logic = new ItemLogic(entry.getJSONObject("logic"));
            ItemRender[] uiRender;
            ItemRender[] heldRender;
            ItemRender[] thrownRender;
            ArmorRender armorRender;

            if(!client) {
                // it's a dedicated server with no client presence, skip ItemRender
                uiRender = null;
                heldRender = null;
                thrownRender = null;
                armorRender = null;
            } else {
                JSONObject renderData = entry.getJSONObject("render");

                if(renderData.has("universal")) {
                    ItemRender[] ir = convertToRenderArray(renderData.get("universal"));
                    uiRender = ir;
                    heldRender = ir;
                    thrownRender = ir;
                    armorRender = null;
                } else {
                    uiRender = convertToRenderArray(renderData.get("ui"));
                    heldRender = convertToRenderArray(renderData.get("held"));

                    if(renderData.has("armor")) {
                        armorRender = new ArmorRender(renderData.getJSONObject("armor"));
                    } else {
                        armorRender = null;
                    }

                    if(renderData.has("thrown")) {
                        thrownRender = convertToRenderArray(renderData.get("thrown"));
                    } else {
                        thrownRender = null;
                    }
                }
            }

            ItemMapping mapping = new ItemMapping(identifier, id, category, aliases, displayName, displayNameColor, uiRender, heldRender, armorRender, thrownRender, logic);
            itemMappings.put(identifier, mapping);
            itemMappingsId.put(id, mapping);

            if(aliases != null) {
                for(String alias : aliases) {
                    aliasMappings.put(alias, mapping);
                }
            }

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
            if(thrownRender != null) {
                for(ItemRender ir : thrownRender) {
                    if(ir.animationFrames > 0) {
                        dynamicAnimationList.add(ir);
                    }
                    if(ir.particleEmitter != null) {
                        dynamicParticleEmitterList.add(ir);
                    }
                }
            }
        }

        log("Added " + itemMappings.size() + " item mappings and " + aliasMappings.size() + " item aliases.");
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
        return itemMappingsId.values().stream().skip(ExpoShared.RANDOM.nextInt(itemMappings.size())).findFirst().orElse(null);
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

    public ItemMapping getMappingByAlias(String alias) {
        return aliasMappings.get(alias);
    }

    public static ItemMapper get() {
        return INSTANCE;
    }

}