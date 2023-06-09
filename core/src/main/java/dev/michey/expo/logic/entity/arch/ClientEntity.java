package dev.michey.expo.logic.entity.arch;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.audio.AudioEngine;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.logic.world.chunk.ClientChunk;
import dev.michey.expo.logic.world.chunk.ClientChunkGrid;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.noise.TileLayerType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.server.main.logic.world.chunk.ServerTile;
import dev.michey.expo.util.EntityRemovalReason;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.weather.Weather;

import static dev.michey.expo.util.ExpoShared.PLAYER_AUDIO_RANGE;

public abstract class ClientEntity {

    /** Passed by the game server. */
    public int entityId;

    /** ClientEntity base fields */
    public float serverPosX;
    public float serverPosY;
    public float clientPosX;
    public float clientPosY;
    public float serverDirX;
    public float serverDirY;
    /** Updated only when method called */
    public int clientChunkX;
    public int clientChunkY;
    public int clientTileX;
    public int clientTileY;
    /** Selection fields */
    public boolean selected;
    /** Death reason */
    public EntityRemovalReason removalReason;

    /** Used for networking syncing. */
    public float lastPosX;
    public float lastPosY;
    private float lastDelta;
    private boolean doLerp;

    /** ClientEntity render fields */
    public float depth;
    public boolean flipped;
    public boolean visibleToRenderEngine;
    public boolean disableTextureCentering;
    public float textureOffsetX;    // The required offset within the texture to the draw start position
    public float textureOffsetY;    // The required offset within the texture to the draw start position
    public float textureWidth;      // Actual width of the texture you want to draw
    public float textureHeight;     // Actual height of the texture you want to draw
    public float positionOffsetX;   // Position offset to sync server->client positions
    public float positionOffsetY;   // Position offset to sync server->client positions
    public float finalDrawPosX, finalDrawPosY;                  // The world position where to draw the texture at
    public float finalTextureCenterX, finalTextureCenterY;      // The world position where the texture is at center visually
    public float finalTextureStartX, finalTextureStartY;        // The world position where the texture is starting visually
    public float finalTextureRootX, finalTextureRootY;          // The world position where the texture has its root visually (feet)
    public float finalSelectionDrawPosX, finalSelectionDrawPosY;// The world position where to draw the selection texture at

    /** ClientEntity base methods */
    public abstract void onCreation();
    public abstract void onDeletion();
    public abstract void onDamage(float damage, float newHealth);
    public abstract void tick(float delta);
    public abstract void render(RenderContext rc, float delta);
    public abstract void renderShadow(RenderContext rc, float delta);
    public abstract ClientEntityType getEntityType();

    /** Network methods */
    public void applyPositionUpdate(float xPos, float yPos) {
        serverPosX = xPos;
        serverPosY = yPos;
        // Prepare for lerping
        lastPosX = clientPosX;
        lastPosY = clientPosY;
        lastDelta = RenderContext.get().deltaTotal;
        doLerp = true;
    }

    public void applyPositionUpdate(float xPos, float yPos, int xDir, int yDir, boolean sprinting) {
        applyPositionUpdate(xPos, yPos);
        serverDirX = xDir;
        serverDirY = yDir;
    }

    public void syncPositionWithServer() {
        if(doLerp) {
            float totalDisX = serverPosX - lastPosX;
            float totalDisY = serverPosY - lastPosY;
            float progress = (RenderContext.get().deltaTotal - lastDelta) * ExpoClientContainer.get().getServerTickRate();

            clientPosX = lastPosX + totalDisX * progress;
            clientPosY = lastPosY + totalDisY * progress;

            if(progress >= 1.0f) {
                clientPosX = serverPosX;
                clientPosY = serverPosY;
                lastPosX = clientPosX;
                lastPosY = clientPosY;
                doLerp = false;
            }
        }
    }

    public void updateChunkAndTile() {
        clientChunkX = ExpoShared.posToChunk(clientPosX);
        clientChunkY = ExpoShared.posToChunk(clientPosY);
        clientTileX = ExpoShared.posToTile(clientPosX);
        clientTileY = ExpoShared.posToTile(clientPosY);
    }

    public void applyPacketPayload(Object[] payload) {

    }

    /** Manager methods */
    public ClientEntityManager entityManager() {
        return ClientEntityManager.get();
    }

    public ClientChunkGrid chunkGrid() {
        return ClientChunkGrid.get();
    }

    /** Render methods */
    public void updateDepth() {
        depth = clientPosY;
    }

    public void updateDepth(float offset) {
        depth = clientPosY + offset;
    }

    public void updateTextureBounds(float textureWidth, float textureHeight, float textureOffsetX, float textureOffsetY) {
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.textureOffsetX = textureOffsetX;
        this.textureOffsetY = textureOffsetY;
        if(!disableTextureCentering) {
            positionOffsetX = textureWidth * -0.5f;
            positionOffsetY = 0;
        }
        updateTexturePositionData();
    }

    public void updateTextureBounds(float textureWidth, float textureHeight, float textureOffsetX, float textureOffsetY, float positionOffsetX, float positionOffsetY) {
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.textureOffsetX = textureOffsetX;
        this.textureOffsetY = textureOffsetY;
        this.positionOffsetX = positionOffsetX;
        this.positionOffsetY = positionOffsetY;
        updateTexturePositionData();
    }

    public void updateTexturePositionData() {
        finalDrawPosX = clientPosX - textureOffsetX + positionOffsetX;
        finalDrawPosY = clientPosY - textureOffsetY + positionOffsetY;
        finalSelectionDrawPosX = finalDrawPosX - 1; // Avoid when using Texture classes
        finalSelectionDrawPosY = finalDrawPosY - 1; // Avoid when using Texture classes

        finalTextureStartX = finalDrawPosX + textureOffsetX;
        finalTextureStartY = finalDrawPosY + textureOffsetY;

        finalTextureCenterX = finalTextureStartX + textureWidth * 0.5f;
        finalTextureCenterY = finalTextureStartY + textureHeight * 0.5f;

        finalTextureRootX = finalTextureStartX + textureWidth * 0.5f;
        finalTextureRootY = finalTextureStartY;
    }

    public void updateTextureBounds(TextureRegion region) {
        updateTextureBounds(region.getRegionWidth(), region.getRegionHeight(), 0, 0);
    }

    public float dstRootX(ClientEntity otherEntity) {
        return Math.abs(finalTextureCenterX - otherEntity.finalTextureCenterX);
    }

    public float dstRootY(ClientEntity otherEntity) {
        return Math.abs(finalTextureRootY - otherEntity.finalTextureRootY);
    }

    public Texture t(String name) {
        return ExpoAssets.get().texture(name);
    }

    public TextureRegion tr(String name) {
        return ExpoAssets.get().textureRegion(name);
    }

    public TextureRegion trn(String name) {
        return ExpoAssets.get().textureRegionFresh(name);
    }

    public TextureRegion[] trArrayFromSheet(TextureRegion base, int x, int y, int width, int height, int frames, int cellWidth) {
        TextureRegion[] array = new TextureRegion[frames];

        for(int i = 0; i < array.length; i++) {
            array[i] = new TextureRegion(base, x + (i * cellWidth), y, width, height);
        }

        return array;
    }

    public float[] generateInteractionArray() {
        return new float[] {
                finalTextureStartX, finalTextureStartY,
                finalTextureStartX + textureWidth, finalTextureStartY,
                finalTextureStartX + textureWidth, finalTextureStartY + textureHeight,
                finalTextureStartX, finalTextureStartY + textureHeight,
        };
    }

    public float[] generateInteractionArray(float offset) {
        return new float[] {
                finalTextureStartX + offset, finalTextureStartY + offset,
                finalTextureStartX + textureWidth - offset, finalTextureStartY + offset,
                finalTextureStartX + textureWidth - offset, finalTextureStartY + textureHeight - offset,
                finalTextureStartX + offset, finalTextureStartY + textureHeight - offset,
        };
    }

    public float[] generateInteractionArray(float offsetGeneral, float yOffset) {
        return new float[] {
                finalTextureStartX + offsetGeneral, finalTextureStartY + offsetGeneral,
                finalTextureStartX + textureWidth - offsetGeneral, finalTextureStartY + offsetGeneral,
                finalTextureStartX + textureWidth - offsetGeneral, finalTextureStartY + textureHeight - yOffset,
                finalTextureStartX + offsetGeneral, finalTextureStartY + textureHeight - yOffset,
        };
    }

    /** Util methods */
    public boolean isMoving() {
        return serverDirX != 0 || serverDirY != 0 || doLerp;
    }

    public String getFootstepSound() {

        int chunkX = ExpoShared.posToChunk(clientPosX);
        int chunkY = ExpoShared.posToChunk(clientPosY);
        int tileX = ExpoShared.posToTile(clientPosX);
        int tileY = ExpoShared.posToTile(clientPosY);

        ClientChunk c = chunkGrid().getChunk(chunkX, chunkY);

        int startTileX = ExpoShared.posToTile(ExpoShared.chunkToPos(c.chunkX));
        int startTileY = ExpoShared.posToTile(ExpoShared.chunkToPos(c.chunkY));
        int relativeTileX = tileX - startTileX;
        int relativeTileY = tileY - startTileY;
        int tileArray = relativeTileY * 8 + relativeTileX;

        TileLayerType t0 = c.dynamicTiles[tileArray][0].emulatingType;
        TileLayerType t1 = c.dynamicTiles[tileArray][1].emulatingType;

        if(t0 == TileLayerType.SOIL_HOLE) {
            return isRaining() ? "step_mud" : "step_dirt";
        }

        if(t1 == TileLayerType.GRASS || t1 == TileLayerType.FOREST) {
            return "step_forest";
        }

        if(t1 == TileLayerType.SAND || t1 == TileLayerType.DESERT) {
            return "step_sand";
        }

        if(BiomeType.isWater(c.biomes[tileArray])) {
            return "step_water";
        }

        return isRaining() ? "step_mud" : "step_dirt";
    }

    public TextureRegion generateSelectionTexture(TextureRegion original) {
        return new TextureRegion(original, -1, -1, original.getRegionWidth() + 2, original.getRegionHeight() + 2);
    }

    public boolean isRaining() {
        return ExpoClientContainer.get().getClientWorld().worldWeather == Weather.RAIN.WEATHER_ID;
    }

    public void stealTextureData(ClientEntity other) {
        textureOffsetX = other.textureOffsetX;
        textureOffsetY = other.textureOffsetY;
        finalDrawPosX = other.finalDrawPosX;
        finalDrawPosY = other.finalDrawPosY;
        textureWidth = other.textureWidth;
        textureHeight = other.textureHeight;
        finalTextureRootX = other.finalTextureRootX;
        finalTextureRootY = other.finalTextureRootY;
        finalTextureStartX = other.finalTextureStartX;
        finalTextureStartY = other.finalTextureStartY;
        finalTextureCenterX = other.finalTextureCenterX;
        finalTextureCenterY = other.finalTextureCenterY;
    }

    public void readEntityDataUpdate(Object[] payload) {

    }

    public void playEntitySound(String group) {
        AudioEngine.get().playSoundGroupManaged(group, new Vector2(finalTextureCenterX, finalTextureRootY), PLAYER_AUDIO_RANGE, false);
    }

}
