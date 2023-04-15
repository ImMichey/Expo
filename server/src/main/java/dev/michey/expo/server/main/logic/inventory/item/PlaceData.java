package dev.michey.expo.server.main.logic.inventory.item;

import org.json.JSONObject;

public class PlaceData {

    public PlaceAlignment alignment;
    public PlaceType type;
    public FloorType floorType;

    public PlaceData(JSONObject o) {
        alignment = PlaceAlignment.valueOf(o.getString("alignment"));
        type = PlaceType.valueOf(o.getString("type"));
        floorType = FloorType.valueOf(o.getString("floorType"));
    }

}