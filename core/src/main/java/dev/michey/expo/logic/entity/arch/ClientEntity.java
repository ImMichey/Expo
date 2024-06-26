package dev.michey.expo.logic.entity.arch;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.audio.AudioEngine;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.logic.entity.misc.ClientDamageIndicator;
import dev.michey.expo.logic.entity.misc.ClientHealthBar;
import dev.michey.expo.logic.entity.misc.ClientPuddle;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.logic.world.chunk.ClientChunk;
import dev.michey.expo.logic.world.chunk.ClientChunkGrid;
import dev.michey.expo.logic.world.chunk.ClientDynamicTilePart;
import dev.michey.expo.noise.TileLayerType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.animator.ContactAnimator;
import dev.michey.expo.render.animator.FoliageAnimator;
import dev.michey.expo.render.shadow.ShadowUtils;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.util.TeleportReason;
import dev.michey.expo.util.EntityRemovalReason;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.weather.Weather;

import static dev.michey.expo.render.RenderContext.*;
import static dev.michey.expo.util.ExpoShared.*;

public abstract class ClientEntity {

    /** Passed by the game server. */
    public int entityId;
    public int tileEntityTileArray;
    public float serverHealth;

    /** ClientEntity base fields */
    public float serverPosX;
    public float serverPosY;
    public float clientPosX;
    public float clientPosY;
    public float serverDirX;
    public float serverDirY;
    public float serverMoveDistance;
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
    public float lastDelta;
    public boolean doLerp;

    /** ClientEntity render fields */
    public float depth;
    public float removalFade;
    public boolean flipped;
    public boolean visibleToRenderEngine;
    public boolean disableTextureCentering;
    public float textureOffsetX;                                // The required offset within the texture to the draw start position
    public float textureOffsetY;                                // The required offset within the texture to the draw start position
    public float textureWidth;                                  // Actual width of the texture you want to draw
    public float textureHeight;                                 // Actual height of the texture you want to draw
    public float positionOffsetY;                               // Position offset to sync server->client positions
    public float positionOffsetX;                               // Position offset to sync server->client positions
    public float finalDrawPosX, finalDrawPosY;                  // The world position where to draw the texture at
    public float finalTextureCenterX, finalTextureCenterY;      // The world position where the texture is at center visually
    public float finalTextureStartX, finalTextureStartY;        // The world position where the texture is starting visually
    public float finalTextureRootX, finalTextureRootY;          // The world position where the texture has its root visually (feet)
    public float finalSelectionDrawPosX, finalSelectionDrawPosY;// The world position where to draw the selection texture at
    public boolean drawReflection = false;
    public float blinkDelta;
    public float lastBlink = Float.MAX_VALUE;

    /** ClientEntity base methods */
    public abstract void onCreation();
    public abstract void onDeletion();
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

    public void applyPositionUpdate(float xPos, float yPos, int xDir, int yDir, boolean sprinting, float distance) {
        applyPositionUpdate(xPos, yPos);
        serverDirX = xDir;
        serverDirY = yDir;
        serverMoveDistance = distance;
    }

    public void applyTeleportUpdate(float xPos, float yPos, TeleportReason reason) {
        serverPosX = xPos;
        serverPosY = yPos;
        clientPosX = xPos;
        clientPosY = yPos;
        lastPosX = xPos;
        lastPosY = yPos;
    }

    public boolean syncPositionWithServer() {
        boolean rtn = doLerp;

        if(doLerp) {
            if(ExpoServerBase.get() != null) {
                doLerp = false;
                clientPosX = serverPosX;
                clientPosY = serverPosY;
                lastPosX = clientPosX;
                lastPosY = clientPosY;
            } else {
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

        return rtn;
    }

    public void updateChunkAndTile() {
        clientChunkX = ExpoShared.posToChunk(clientPosX);
        clientChunkY = ExpoShared.posToChunk(clientPosY);
        clientTileX = ExpoShared.posToTile(clientPosX);
        clientTileY = ExpoShared.posToTile(clientPosY);
    }

    public void applyCreationPayload(Object[] payload) {

    }

    public void onDamage(float damage, float newHealth, int damageSourceEntityId) {

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

    public void updateTextureBoundScaled(TextureRegion region, float scaleX, float scaleY) {
        updateTextureBounds(region.getRegionWidth() * scaleX, region.getRegionHeight() * scaleY, 0, 0);
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

    public TextureRegion[] tra(String name, int frames) {
        TextureRegion[] array = new TextureRegion[frames];

        for(int i = 0; i < frames; i++) {
            array[i] = trn(name + "_" + (i + 1));
        }

        return array;
    }

    public TextureRegion trn(String name) {
        return ExpoAssets.get().textureRegionFresh(name);
    }

    public void flip(TextureRegion[]... textureRegions) {
        for(TextureRegion[] regions : textureRegions) {
            for(TextureRegion t : regions) t.flip(true, false);
        }
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

    public void playFootstepSound() {

    }

    public float movementSpeedMultiplicator() {
        boolean deepWater = isInDeepWater();
        if(deepWater) return 0.3f;

        boolean water = isInWater();
        if(water) return 0.6f;

        var layers = getCurrentTileLayers();
        boolean hole = layers[0].emulatingType == TileLayerType.SOIL_HOLE;
        if(hole) return 0.75f;

        return 1.0f;
    }

    public boolean isInDeepWater() {
        return TileLayerType.isDeepWater(getCurrentTileLayers()[2].emulatingType);
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
        int tileArray = relativeTileY * ROW_TILES + relativeTileX;

        TileLayerType t0 = c.dynamicTiles[tileArray][0].emulatingType;
        TileLayerType t1 = c.dynamicTiles[tileArray][1].emulatingType;
        TileLayerType t2 = c.dynamicTiles[tileArray][2].emulatingType;

        if(TileLayerType.isWater(t2)) {
            return "step_water";
        }

        if(t0 == TileLayerType.SOIL_HOLE || t0 == TileLayerType.SOIL_FARMLAND) {
            return isRaining() ? "step_mud" : "step_dirt";
        }

        if(t1 == TileLayerType.OAK_PLANK) {
            return "step_wood";
        }

        if(t1 == TileLayerType.GRASS || t1 == TileLayerType.FOREST) {
            return "step_forest";
        }

        if(t1 == TileLayerType.SAND || t1 == TileLayerType.DESERT) {
            return "step_sand";
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

    public ClientEntity[] getNeighbouringTileEntitiesNESW() {
        return chunkGrid().getChunk(ExpoShared.posToChunk(serverPosX), ExpoShared.posToChunk(serverPosY)).getNeighbouringEntitiesNESW(tileEntityTileArray);
    }

    /*
    public void spawnDamageIndicator(int damage, float posX, float posY, Vector2 dir) {
        ClientDamageIndicator damageIndicator = new ClientDamageIndicator();
        damageIndicator.damageNumber = damage;
        damageIndicator.moveDir = dir == null ? new Vector2(0, 0) : dir;
        damageIndicator.clientPosX = posX;
        damageIndicator.clientPosY = posY;
        ClientEntityManager.get().addClientSideEntity(damageIndicator);
    }
    */

    public void spawnDamageIndicator(float damage, float posX, float posY, ClientEntity e) {
        Vector2 dir = e == null ? null : new Vector2(clientPosX, clientPosY).sub(e.clientPosX, e.clientPosY).nor();

        ClientDamageIndicator damageIndicator = new ClientDamageIndicator();
        damageIndicator.damageNumber = damage;
        damageIndicator.moveDir = dir == null ? new Vector2(0, 0) : dir;
        damageIndicator.clientPosX = posX + damageIndicator.moveDir.x * 8f;
        damageIndicator.clientPosY = posY + damageIndicator.moveDir.y * 8f;
        if(getEntityType() == ClientEntityType.PLAYER) {
            damageIndicator.color = ((ClientPlayer) this).player ? ClientDamageIndicator.PLAYER_COLOR : ClientDamageIndicator.DEFAULT_COLOR;
        } else {
            damageIndicator.color = ClientDamageIndicator.DEFAULT_COLOR;
        }
        ClientEntityManager.get().addClientSideEntity(damageIndicator);
    }

    public void drawShadowIfVisible(TextureRegion texture) {
        RenderContext rc = RenderContext.get();

        Affine2 shadow = ShadowUtils.createSimpleShadowAffine(finalTextureStartX, finalTextureStartY);
        float[] vertices = rc.arraySpriteBatch.obtainShadowVertices(texture, shadow);

        if(rc.verticesInBounds(vertices)) {
            rc.arraySpriteBatch.drawGradient(texture, textureWidth, textureHeight, shadow);
        }
    }

    public void drawShadowIfVisible(TextureRegion texture, float internalOffsetY) {
        RenderContext rc = RenderContext.get();

        Affine2 shadow = ShadowUtils.createSimpleShadowAffineInternalOffset(finalTextureStartX, finalTextureStartY, 0, internalOffsetY);
        float[] vertices = rc.arraySpriteBatch.obtainShadowVertices(texture, shadow);

        if(rc.verticesInBounds(vertices)) {
            rc.arraySpriteBatch.drawGradient(texture, textureWidth, textureHeight, shadow);
        }
    }

    public void drawWindShadowIfVisible(TextureRegion texture, ContactAnimator contactAnimator) {
        RenderContext rc = RenderContext.get();

        Affine2 shadow = ShadowUtils.createSimpleShadowAffine(finalTextureStartX, finalTextureStartY);
        float[] vertices = rc.arraySpriteBatch.obtainShadowVertices(texture, shadow);

        if(rc.verticesInBounds(vertices)) {
            rc.arraySpriteBatch.drawGradientCustomVertices(texture, texture.getRegionWidth(), texture.getRegionHeight(), shadow, contactAnimator.value, contactAnimator.value);
        }
    }

    public void drawWindShadowIfVisible(TextureRegion texture, FoliageAnimator foliageAnimator, ContactAnimator contactAnimator) {
        RenderContext rc = RenderContext.get();

        Affine2 shadow = ShadowUtils.createSimpleShadowAffine(finalTextureStartX, finalTextureStartY);
        float[] vertices = rc.arraySpriteBatch.obtainShadowVertices(texture, shadow);

        if(rc.verticesInBounds(vertices)) {
            rc.arraySpriteBatch.drawGradientCustomVertices(texture, texture.getRegionWidth(), texture.getRegionHeight() * contactAnimator.squish, shadow, foliageAnimator.value + contactAnimator.value, foliageAnimator.value + contactAnimator.value);
        }
    }

    public void calculateReflection() {
        float baseX = clientPosX;
        float baseY = clientPosY;
        int chunkX = ExpoShared.posToChunk(baseX);
        int chunkY = ExpoShared.posToChunk(baseY);
        ClientChunk chunk = chunkGrid().getChunk(chunkX, chunkY);
        if(chunk == null) return;

        int tileX = ExpoShared.posToTile(baseX) - ExpoShared.posToTile(ExpoShared.chunkToPos(chunkX));
        int tileY = ExpoShared.posToTile(baseY) - ExpoShared.posToTile(ExpoShared.chunkToPos(chunkY));

        int baseTileArray = tileY * ROW_TILES + tileX;

        var base = chunk.getTileAt(baseTileArray, 0, 0);
        TileLayerType baseL2 = base.key.dynamicTiles[base.value][2].emulatingType;

        if(TileLayerType.isWater(baseL2)) {
            drawReflection = true;
            return;
        }

        int checkX = (int) (textureWidth / TILE_SIZE) + 1;
        int checkY = (int) (textureHeight / TILE_SIZE) + 1;

        for(int i = 0; i < checkX; i++) {
            for(int j = 0; j < checkY; j++) {
                var candidate = chunk.getTileAt(baseTileArray, i - checkX / 2, -j - 1);
                if(candidate == null) continue;

                TileLayerType l2 = candidate.key.dynamicTiles[candidate.value][2].emulatingType;

                if(TileLayerType.isWater(l2)) {
                    drawReflection = true;
                    return;
                }
            }
        }
    }

    public void spawnPuddle(boolean big) {
        spawnPuddle(big, 0, 0);
    }

    public void spawnPuddle(boolean big, float offsetX, float offsetY) {
        float v = big ? 6 : 5;

        ClientPuddle puddle = new ClientPuddle();
        puddle.small = !big;
        puddle.upperPart = true;
        puddle.clientPosX = clientPosX + offsetX;
        puddle.clientPosY = clientPosY - v + offsetY;
        puddle.updateDepth(v * 2);
        ClientEntityManager.get().addClientSideEntity(puddle);

        ClientPuddle puddle2 = new ClientPuddle();
        puddle2.small = !big;
        puddle2.clientPosX = clientPosX + offsetX;
        puddle2.clientPosY = clientPosY - v + offsetY;
        puddle2.updateDepth(v);
        ClientEntityManager.get().addClientSideEntity(puddle2);
    }

    public boolean isInWater() {
        return TileLayerType.isWater(getCurrentTileLayers()[2].emulatingType);
    }

    public ClientChunk getCurrentChunk() {
        return chunkGrid().getChunk(posToChunk(clientPosX), posToChunk(clientPosY));
    }

    public ClientDynamicTilePart[] getCurrentTileLayers() {
        int cx = posToChunk(clientPosX);
        int cy = posToChunk(clientPosY);
        int tileX = ExpoShared.posToTile(clientPosX) - ExpoShared.posToTile(ExpoShared.chunkToPos(cx));
        int tileY = ExpoShared.posToTile(clientPosY) - ExpoShared.posToTile(ExpoShared.chunkToPos(cy));
        int baseTileArray = tileY * ROW_TILES + tileX;
        return chunkGrid().getChunk(cx, cy).dynamicTiles[baseTileArray];
    }

    public TileLayerType getCurrentTileLayer() {
        var layers = getCurrentTileLayers();
        if(layers[2].emulatingType != TileLayerType.EMPTY) return layers[2].emulatingType;
        if(layers[1].emulatingType != TileLayerType.EMPTY) return layers[1].emulatingType;
        return layers[0].emulatingType;
    }

    public void applyEntityUpdatePayload(Object[] payload) {

    }

    public void playEntitySound(String group, float volumeMultiplier) {
        AudioEngine.get().playSoundGroupManaged(group, new Vector2(finalTextureCenterX, finalTextureRootY), PLAYER_AUDIO_RANGE * volumeMultiplier, false, volumeMultiplier);
    }

    public void playEntitySound(String group) {
        AudioEngine.get().playSoundGroupManaged(group, new Vector2(finalTextureCenterX, finalTextureRootY), PLAYER_AUDIO_RANGE, false);
    }

    public void setSelectionValues() {
        RenderContext rc = RenderContext.get();
        rc.bindAndSetSelection(rc.arraySpriteBatch, 2048, Color.WHITE, false);
    }

    public void setSelectionValues(Color color) {
        RenderContext rc = RenderContext.get();
        rc.bindAndSetSelection(rc.arraySpriteBatch, 2048, color, false);
    }

    public void setSelectionValuesNoOutline() {
        RenderContext rc = RenderContext.get();
        rc.bindAndSetSelection(rc.arraySpriteBatch, 2048, Color.WHITE, true);
    }

    protected void drawAO(RenderContext rc, float scaleX, float scaleY, float offsetX, float offsetY) {
        float fw = rc.aoTexture.getWidth() * scaleX;
        float fh = rc.aoTexture.getHeight() * scaleY;
        rc.aoBatch.draw(rc.aoTexture, clientPosX - fw * 0.5f + offsetX, clientPosY - fh * 0.5f + offsetY, fw, fh);
    }

    public void drawAOAuto100(RenderContext rc) {
        float scale = textureWidth / rc.aoTexture.getWidth();
        if(rc.aoBatch.getPackedColor() != TRANS_100_PACKED) rc.aoBatch.setPackedColor(TRANS_100_PACKED);
        drawAO(rc, scale, scale, 0, 0);
    }

    public void drawAOAuto50(RenderContext rc) {
        float scale = textureWidth / rc.aoTexture.getWidth();
        if(rc.aoBatch.getPackedColor() != TRANS_50_PACKED) rc.aoBatch.setPackedColor(TRANS_50_PACKED);
        drawAO(rc, scale, scale, 0, 0);
    }

    public void drawAOAuto33(RenderContext rc) {
        float scale = textureWidth / rc.aoTexture.getWidth();
        if(rc.aoBatch.getPackedColor() != TRANS_33_PACKED) rc.aoBatch.setPackedColor(TRANS_33_PACKED);
        drawAO(rc, scale, scale, 0, 0);
    }

    public void drawAO100(RenderContext rc, float scaleX, float scaleY, float offsetX, float offsetY) {
        if(rc.aoBatch.getPackedColor() != TRANS_100_PACKED) rc.aoBatch.setPackedColor(TRANS_100_PACKED);
        drawAO(rc, scaleX, scaleY, offsetX, offsetY);
    }

    public void drawAO50(RenderContext rc, float scaleX, float scaleY, float offsetX, float offsetY) {
        if(rc.aoBatch.getPackedColor() != TRANS_50_PACKED) rc.aoBatch.setPackedColor(TRANS_50_PACKED);
        drawAO(rc, scaleX, scaleY, offsetX, offsetY);
    }

    public void drawAO33(RenderContext rc, float scaleX, float scaleY, float offsetX, float offsetY) {
        if(rc.aoBatch.getPackedColor() != TRANS_33_PACKED) rc.aoBatch.setPackedColor(TRANS_33_PACKED);
        drawAO(rc, scaleX, scaleY, offsetX, offsetY);
    }

    public void chooseArrayBatch(RenderContext rc, float intensity) {
        if(blinkDelta > 0) {
            rc.useBlinkShader();
            rc.blinkShader.setUniformf("u_intensity", intensity);
        } else {
            rc.useRegularArrayShader();
        }
    }

    public float tickBlink(float delta, float speed) {
        if(blinkDelta > 0) {
            blinkDelta -= delta * speed;
            if(blinkDelta < 0) blinkDelta = 0;
            return Interpolation.smooth2.apply(1f - blinkDelta);
        }
        return 0f;
    }

    public float tickBlink(float delta) {
        if(blinkDelta > 0) {
            blinkDelta -= delta * 7.5f;
            if(blinkDelta < 0) blinkDelta = 0;
            return Interpolation.smooth2.apply(1f - blinkDelta);
        }
        return 0f;
    }

    public void setBlink() {
        blinkDelta = 1.0f;
        RenderContext r = RenderContext.get();

        float diff = r.deltaTotal - lastBlink;

        if(diff >= 0.25f) {
            lastBlink = r.deltaTotal - 0.25f;
        } else {
            lastBlink = r.deltaTotal;
        }
    }

    public void spawnHealthBar(float damage) {
        var hb = ClientEntityManager.get().getEntitiesByType(ClientEntityType.HEALTH_BAR);
        ClientHealthBar use = null;

        for(ClientEntity e : hb) {
            ClientHealthBar chb = (ClientHealthBar) e;

            if(entityId == chb.parentEntity.entityId) {
                use = chb;
                break;
            }
        }

        if(use == null) {
            use = new ClientHealthBar();
            use.parentEntity = this;
            use.clientPosX = use.parentEntity.clientPosX;
            use.clientPosY = use.parentEntity.clientPosY;
            use.removalDelay = 0.125f;
            ClientEntityManager.get().addClientSideEntity(use);
        } else {
            use.removalDelay = 0f;
        }

        use.damage = damage;
        use.removalDelta = 1.0f;
    }

    public void playEntityAnimation(int animationId) {

    }

}