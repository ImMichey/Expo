package dev.michey.expo.server.main.logic.inventory.item.mapping;

import com.badlogic.gdx.graphics.Color;

public class ItemMapping {

    public String identifier;
    public int id;
    public String displayName;
    public String displayNameColor;
    public Color color;
    public ItemRender[] uiRender;
    public ItemRender[] heldRender;
    public ItemRender[] armorRender;
    public ItemLogic logic;

    public ItemMapping(String identifier, int id, String displayName, String displayNameColor, ItemRender[] uiRender, ItemRender[] heldRender, ItemRender[] armorRender, ItemLogic logic) {
        this.identifier = identifier;
        this.id = id;
        this.displayName = displayName;
        this.displayNameColor = displayNameColor;
        this.uiRender = uiRender;
        this.heldRender = heldRender;
        this.armorRender = armorRender;
        this.logic = logic;
    }

}
