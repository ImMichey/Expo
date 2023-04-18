package dev.michey.expo.logic.entity.arch;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.logic.world.chunk.ClientChunk;
import dev.michey.expo.logic.world.chunk.ClientChunkGrid;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.server.main.logic.world.chunk.ServerTile;
import dev.michey.expo.util.EntityRemovalReason;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.weather.Weather;

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
    public float drawOffsetX;
    public float drawOffsetY;
    public float drawWidth;
    public float drawHeight;
    public boolean flipped;
    public float drawCenterX;
    public float drawCenterY;
    public float drawRootX;
    public float drawRootY;
    public boolean visibleToRenderEngine;

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

    public void applyPositionUpdate(float xPos, float yPos, int xDir, int yDir) {
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

    public void updateTexture(float drawOffsetX, float drawOffsetY, float drawWidth, float drawHeight) {
        this.drawOffsetX = drawOffsetX;
        this.drawOffsetY = drawOffsetY;
        this.drawWidth = drawWidth;
        this.drawHeight = drawHeight;
        updateCenterAndRoot();
    }

    public void updateCenterAndRoot() {
        drawCenterX = clientPosX + drawOffsetX + drawWidth * 0.5f;
        drawCenterY = clientPosY + drawOffsetY + drawHeight * 0.5f;
        drawRootX = clientPosX + drawOffsetX + drawWidth * 0.5f;
        drawRootY = clientPosY + drawOffsetY;
    }

    public float dstRootX(ClientEntity otherEntity) {
        return Math.abs(drawRootX - otherEntity.drawRootX);
    }

    public float dstRootY(ClientEntity otherEntity) {
        return Math.abs(drawRootY - otherEntity.drawRootY);
    }

    public float toVisualCenterX() {
        return clientPosX + drawOffsetX + drawWidth * 0.5f;
    }

    public float toVisualCenterY() {
        return clientPosY + drawOffsetY + drawHeight * 0.5f;
    }

    public Array<TextureRegion> ta(String name, int frames) {
        Array<TextureRegion> array = new Array<>(frames);
        for(int i = 0; i < frames; i++) {
            array.add(tr(name + "_" + (i + 1)));
        }
        return array;
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
                clientPosX, clientPosY,
                clientPosX + drawWidth, clientPosY,
                clientPosX + drawWidth, clientPosY + drawHeight,
                clientPosX, clientPosY + drawHeight,
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

        int l0 = c.layer0[tileArray][0];
        int l1 = c.layer1[tileArray][0];

        if(ServerTile.isHoleSoilTile(l0)) {
            return isRaining() ? "step_mud" : "step_dirt";
        }

        if(ServerTile.isGrassTile(l1) || ServerTile.isForestTile(l1)) {
            return "step_forest";
        }

        if(ServerTile.isSandTile(l1) || ServerTile.isDesertTile(l1)) {
            return "step_sand";
        }

        if(BiomeType.isWater(c.biomes[tileArray])) {
            return "step_water";
        }

        return isRaining() ? "step_mud" : "step_dirt";
    }

    public boolean isRaining() {
        return ExpoClientContainer.get().getClientWorld().worldWeather == Weather.RAIN.WEATHER_ID;
    }

    public void readEntityDataUpdate(Object[] payload) {

    }

}
