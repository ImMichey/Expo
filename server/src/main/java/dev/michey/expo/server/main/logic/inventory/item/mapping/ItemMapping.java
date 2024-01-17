package dev.michey.expo.server.main.logic.inventory.item.mapping;

import com.badlogic.gdx.graphics.Color;

public class ItemMapping {

    public String identifier;
    public int id;
    public ItemCategory category;
    public String displayName;
    public String displayNameColor;
    public Color color;
    public ItemRender[] uiRender;
    public ItemRender[] heldRender;
    public ItemRender[] armorRender;
    public ItemRender[] thrownRender;
    public ItemLogic logic;

    public ItemMapping(String identifier, int id, ItemCategory category, String displayName, String displayNameColor, ItemRender[] uiRender, ItemRender[] heldRender, ItemRender[] armorRender, ItemRender[] thrownRender, ItemLogic logic) {
        this.identifier = identifier;
        this.id = id;
        this.category = category;
        this.displayName = displayName;
        this.displayNameColor = displayNameColor;
        this.uiRender = uiRender;
        this.heldRender = heldRender;
        this.armorRender = armorRender;
        this.thrownRender = thrownRender;
        this.logic = logic;
    }

}
