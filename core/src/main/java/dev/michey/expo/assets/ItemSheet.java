package dev.michey.expo.assets;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.HashMap;

public class ItemSheet {

    private final HashMap<String, TextureRegion> itemTextureMap;
    private final TextureRegion sheet;
    private final int tilesPerRow;

    public ItemSheet(TextureRegion sheet) {
        itemTextureMap = new HashMap<>();
        this.sheet = sheet;
        tilesPerRow = sheet.getRegionWidth() / 16;

        // Add textures to map
        add("item_blueberry", 0, 12, 10);
        add("item_grassfiber", 1, 15, 14);
        add("item_stick", 2, 14, 14);
        add("item_flint_pickaxe", 3, 16, 16);
        add("item_flint_shovel", 4, 15, 15);
        add("item_flint_axe", 5, 17, 13);
        add("item_sand", 7, 10, 9);
        add("item_leaf_helmet", 8, 11, 7);
        add("item_iron_pickaxe", 9, 16, 16);
        add("item_iron_axe", 10, 18, 14);
        add("item_floor_grass", 12, 10, 9);
        add("item_dirt", 13, 10, 9);
        add("item_op_shovel", 14, 15, 15);
        add("item_flint", 15, 8, 7);
        add("item_worm", 16, 13, 10);
        add("item_cord", 17, 12, 13);
        add("item_rock", 18, 11, 10);
        add("item_oak_log", 19, 15, 15);
        add("item_acorn", 20, 12, 12);
        add("item_mushroom_red", 21, 8, 9);
        add("item_mushroom_brown", 22, 8, 9);
        add("item_oak_plank", 23, 14, 11);
        add("item_mushroom_glowing", 24, 8, 9);
        add("item_flint_scythe", 25, 16, 15);
        add("item_wheat", 26, 13, 12);
        add("item_wheat_seeds", 27, 9, 8);
        add("item_maggot", 28, 13, 10);
        add("item_wood_mask", 29, 9, 9);
    }

    public TextureRegion get(String name) {
        return itemTextureMap.get(name);
    }

    private void add(String name, int tile, int w, int h) {
        int x = tile % tilesPerRow;
        int y = tile / tilesPerRow;

        int xPos = x * 16;
        int yPos = y * 16 + (16 - h);

        itemTextureMap.put(name, new TextureRegion(sheet, xPos, yPos, w, h));
    }

}
