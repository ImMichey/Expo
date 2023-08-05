package dev.michey.expo.server.main.logic.inventory.item;

import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import org.json.JSONObject;

public class PlaceData {

    public PlaceAlignment alignment;
    public PlaceType type;
    public FloorType floorType;
    public ServerEntityType entityType;

    public PlaceData(JSONObject o) {
        alignment = PlaceAlignment.valueOf(o.getString("alignment"));
        type = PlaceType.valueOf(o.getString("type"));
        if(o.has("floorType")) floorType = FloorType.valueOf(o.getString("floorType"));
        if(o.has("entityType")) entityType = ServerEntityType.valueOf(o.getString("entityType"));
    }

}