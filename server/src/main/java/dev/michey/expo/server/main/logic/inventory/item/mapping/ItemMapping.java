package dev.michey.expo.server.main.logic.inventory.item.mapping;

import com.badlogic.gdx.graphics.Color;

public class ItemMapping {

    public String identifier;
    public int id;
    public ItemCategory category;
    public String[] aliases;
    public String displayName = "<MISSING STR>";
    public String displayNameColor;
    public Color color;
    public ItemRender[] uiRender;
    public ItemRender[] heldRender;
    public ItemRender[] thrownRender;
    public ArmorRender armorRender;
    public ItemLogic logic;

    public ItemMapping(String identifier, int id, ItemCategory category, String[] aliases, String displayNameColor, ItemRender[] uiRender, ItemRender[] heldRender, ArmorRender armorRender, ItemRender[] thrownRender, ItemLogic logic) {
        this.identifier = identifier;
        this.id = id;
        this.category = category;
        this.aliases = aliases;
        this.displayNameColor = displayNameColor;
        this.uiRender = uiRender;
        this.heldRender = heldRender;
        this.armorRender = armorRender;
        this.thrownRender = thrownRender;
        this.logic = logic;
    }

}
