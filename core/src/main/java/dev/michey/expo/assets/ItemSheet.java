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
        tilesPerRow = sheet.getRegionWidth() / 32;

        // Add textures to map
        add("item_blueberry", 0, 12, 10);
        add("item_grassfiber", 1, 15, 14);
        add("item_stick", 2, 14, 13);
        add("item_flint_pickaxe", 3, 16, 16);
        add("item_flint_shovel", 4, 15, 15);
        add("item_flint_axe", 5, 17, 13);
        add("item_sand", 6, 11, 11);
        add("item_leaf_helmet", 7, 11, 7);
        add("item_iron_pickaxe", 8, 16, 16);
        add("item_iron_axe", 9, 18, 14);
        add("item_floor_grass", 10, 13, 9);
        add("item_dirt", 11, 11, 11);
        add("item_op_shovel", 12, 15, 15);
        add("item_flint", 13, 8, 8);
        add("item_worm", 14, 14, 11);
        add("item_cord", 15, 11, 12);
        add("item_rock", 16, 8, 7);
        add("item_oak_log", 17, 15, 15);
        add("item_acorn", 18, 12, 12);
        add("item_mushroom_red", 19, 11, 12);
        add("item_mushroom_brown", 20, 11, 12);
        add("item_oak_plank", 21, 14, 11);
        add("item_mushroom_glowing", 22, 11, 12);
        add("item_flint_scythe", 23, 16, 15);
        add("item_wheat", 24, 13, 12);
        add("item_wheat_seeds", 25, 9, 8);
        add("item_maggot", 26, 10, 14);
        add("item_wood_mask", 27, 9, 9);
        add("item_fence_stick", 28, 12, 15);
        add("item_oak_plank_floor", 29, 13, 8);
        add("item_birch_log", 30, 15, 15);
        add("item_oak_plank_wall", 31, 12, 13);
        add("item_crate", 32, 14, 16);
        add("item_carrot", 33, 14, 15);
        add("item_coal", 34, 10, 10);
        add("item_dirt_floor", 35, 13, 8);
        add("item_sand_floor", 36, 13, 8);
        add("item_torch", 37, 12, 15);
        add("item_rock_wall", 38, 12, 13);
        add("item_dirt_wall", 39, 12, 13);
        add("item_insect_net", 40, 14, 16);
        add("item_firefly", 41, 9, 6);
        add("item_crab_meat_raw", 42, 12, 9);
        add("item_sign", 44, 16, 17);
        add("item_wood_club", 43, 16, 16);
        add("item_flint_dagger", 45, 11, 11);
        add("item_campfire", 46, 15, 17);
        add("item_hedge_wall", 47, 12, 13);
        add("item_aloe_vera", 48, 10, 10);
        add("item_bandage", 49, 15, 10);
        add("item_aloe_vera_seeds", 50, 9, 8);
        add("item_lilypad", 51, 11, 8);
        add("item_bomb", 52, 11, 14);
    }

    public TextureRegion get(String name) {
        return itemTextureMap.get(name);
    }

    private void add(String name, int tile, int w, int h) {
        int x = tile % tilesPerRow;
        int y = tile / tilesPerRow;

        int xPos = x * 32;
        int yPos = y * 32 + (32 - h);

        itemTextureMap.put(name, new TextureRegion(sheet, xPos, yPos, w, h));
    }

}
