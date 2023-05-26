package dev.michey.expo.server.main.logic.world.chunk;

import dev.michey.expo.noise.TileLayerType;

/** A DynamicTilePart is a tile that is often higher than the regular tile (16x32 or 16x48 instead of 16x16), casts shadows
 * and can be interacted with. */
public class DynamicTilePart {

    public TileLayerType emulatingType;
    public float tileHealth;
    public int[] layerIds;

    public void update(TileLayerType type) {
        this.emulatingType = type;
        generateHealth();
    }

    public void setTileIds(int[] ids) {
        layerIds = ids;
    }

    public boolean hit(float damage) {
        tileHealth -= damage;
        return tileHealth <= 0;
    }

    public void generateHealth() {
        tileHealth = switch (emulatingType) {
            case SOIL -> 30.0f;
            case SAND, DESERT -> 20.0f;
            case FOREST, GRASS -> 20.0f;
            case ROCK -> 50.0f;
            default -> 0.0f;
        };
    }

}
