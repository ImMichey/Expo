package dev.michey.expo.server.main.logic.inventory.item;

import dev.michey.expo.noise.TileLayerType;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import org.json.JSONObject;

public class PlaceData {

    public PlaceAlignment alignment;
    public PlaceType type;
    public FloorType floorType;
    public ServerEntityType entityType;
    public String previewTextureName;
    public float placeAlignmentOffsetX, placeAlignmentOffsetY;
    public TileLayerType floorRequirement;

    public PlaceData(JSONObject o) {
        alignment = PlaceAlignment.valueOf(o.getString("alignment"));
        type = PlaceType.valueOf(o.getString("type"));
        if(o.has("entityType")) entityType = ServerEntityType.valueOf(o.getString("entityType"));
        previewTextureName = o.has("previewTexture") ? o.getString("previewTexture") : "no_tex";

        if(o.has("floorType")) floorType = FloorType.valueOf(o.getString("floorType"));
        if(o.has("placeAlignmentOffsetX")) placeAlignmentOffsetX = o.getFloat("placeAlignmentOffsetX");
        if(o.has("placeAlignmentOffsetY")) placeAlignmentOffsetY = o.getFloat("placeAlignmentOffsetY");
        if(o.has("floorRequirement")) floorRequirement = TileLayerType.valueOf(o.getString("floorRequirement"));
    }

}