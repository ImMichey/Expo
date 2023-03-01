package dev.michey.expo.logic.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.audio.AudioEngine;
import dev.michey.expo.devhud.DevHUD;
import dev.michey.expo.localserver.ExpoServerLocal;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityManager;
import dev.michey.expo.logic.entity.ClientPlayer;
import dev.michey.expo.logic.world.chunk.ClientChunk;
import dev.michey.expo.logic.world.chunk.ClientChunkGrid;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.util.ExpoTime;

import java.util.zip.Deflater;

import static dev.michey.expo.log.ExpoLogger.log;
import static dev.michey.expo.util.ExpoShared.PLAYER_CHUNK_VIEW_RANGE;
import static dev.michey.expo.util.ExpoShared.TILE_SIZE;

public class ClientWorld {

    /** Entity handler */
    private final ClientEntityManager clientEntityManager;
    private final ClientChunkGrid clientChunkGrid;
    /** Optimization */
    private final ClientChunk[] drawChunks;

    /** Time */
    public float worldTime;
    public int worldWeather;
    public final float MAX_SHADOW_X = 2.2f;
    public final float MAX_SHADOW_Y = 1.7f;
    public float worldSunShadowX = MAX_SHADOW_X;
    public float worldSunShadowY = MAX_SHADOW_Y;
    public float worldSunShadowAlpha = 1.0f;
    public float ambientLightingR = 1.0f;
    public float ambientLightingG = 1.0f;
    public float ambientLightingB = 1.0f;
    public float ambientLightingDarkness = 1.0f;
    public final Color COLOR_AMBIENT_MIDNIGHT = new Color(24f / 255f, 30f / 255f, 66f / 255f, 1.0f);
    public final Color COLOR_AMBIENT_SUNRISE = new Color(241f / 255f, 241f / 255f, 197f / 255f, 1.0f);
    public final Color COLOR_AMBIENT_SUNSET = new Color(222f / 255f, 177f / 255f, 128f / 255f, 1.0f);

    public ClientWorld() {
        clientEntityManager = new ClientEntityManager();
        clientChunkGrid = new ClientChunkGrid();
        drawChunks = new ClientChunk[PLAYER_CHUNK_VIEW_RANGE * PLAYER_CHUNK_VIEW_RANGE];
    }

    /** Ticking the game world. */
    public void tickWorld(float delta, float serverDelta) {
        // Tick entities
        clientEntityManager.tickEntities(delta);

        // Tick camera
        RenderContext.get().expoCamera.tick();

        // Calculate world time
        calculateWorldTime(delta);
    }

    private void calculateWorldTime(float delta) {
        worldTime += delta;
        if(worldTime >= ExpoTime.WORLD_CYCLE_DURATION) worldTime %= ExpoTime.WORLD_CYCLE_DURATION;

        // Calculate shadow x and y
        if(worldTime < ExpoTime.SUNRISE || worldTime > ExpoTime.NIGHT) {
            // Nighttime
            float secondsPassed = (worldTime < ExpoTime.SUNRISE ? (worldTime + ExpoTime.worldDurationHours(2)) : (worldTime - ExpoTime.NIGHT));
            float normalized = secondsPassed / ExpoTime.worldDurationHours(8);
            calculateShadows(normalized);
        } else if(worldTime >= ExpoTime.SUNRISE && worldTime <= ExpoTime.NIGHT) {
            //Night->Day, Day, Day->Night time
            float secondsPassed = worldTime - ExpoTime.SUNRISE;
            float normalized = secondsPassed / ExpoTime.worldDurationHours(16);
            calculateShadows(normalized);
        }

        // Calculate shadow alpha
        if(worldTime >= ExpoTime.DAY && worldTime < ExpoTime.SUNSET) {
            worldSunShadowAlpha = 1.0f;
            setAmbient(1, 1, 1);
            AudioEngine.get().ambientVolume("ambience_day", 1.0f);
            AudioEngine.get().ambientVolume("ambience_night", 0.0f);
        } else if(worldTime >= ExpoTime.SUNRISE && worldTime < ExpoTime.DAY) {
            // 6:00 - 8:00
            float secondsPassed = worldTime - ExpoTime.SUNRISE;
            float normalized = secondsPassed / ExpoTime.worldDurationHours(2);
            worldSunShadowAlpha = normalized;

            if(normalized < 0.5f) {
                float _n = normalized * 2;

                float diffR = COLOR_AMBIENT_SUNRISE.r - COLOR_AMBIENT_MIDNIGHT.r;
                float diffG = COLOR_AMBIENT_SUNRISE.g - COLOR_AMBIENT_MIDNIGHT.g;
                float diffB = COLOR_AMBIENT_SUNRISE.b - COLOR_AMBIENT_MIDNIGHT.b;

                setAmbient(
                        COLOR_AMBIENT_MIDNIGHT.r + diffR * _n,
                        COLOR_AMBIENT_MIDNIGHT.g + diffG * _n,
                        COLOR_AMBIENT_MIDNIGHT.b + diffB * _n
                );
            } else {
                float _n = (normalized - 0.5f) * 2;

                float diffR = 1.0f - COLOR_AMBIENT_SUNRISE.r;
                float diffG = 1.0f - COLOR_AMBIENT_SUNRISE.g;
                float diffB = 1.0f - COLOR_AMBIENT_SUNRISE.b;

                setAmbient(
                        COLOR_AMBIENT_SUNRISE.r + diffR * _n,
                        COLOR_AMBIENT_SUNRISE.g + diffG * _n,
                        COLOR_AMBIENT_SUNRISE.b + diffB * _n
                );
            }

            AudioEngine.get().ambientVolume("ambience_day", normalized);
            AudioEngine.get().ambientVolume("ambience_night", 1f - normalized);
        } else if(worldTime >= ExpoTime.SUNSET && worldTime < ExpoTime.NIGHT) {
            // 20:00 - 22:00
            float secondsPassed = worldTime - ExpoTime.SUNSET;
            float normalized = secondsPassed / ExpoTime.worldDurationHours(2);
            worldSunShadowAlpha = 1f - normalized;

            if(normalized < 0.5f) {
                float _n = normalized * 2;

                float diffR = 1.0f - COLOR_AMBIENT_SUNSET.r;
                float diffG = 1.0f - COLOR_AMBIENT_SUNSET.g;
                float diffB = 1.0f - COLOR_AMBIENT_SUNSET.b;

                setAmbient(
                        1.0f - diffR * _n,
                        1.0f - diffG * _n,
                        1.0f - diffB * _n
                );
            } else {
                float _n = (normalized - 0.5f) * 2;

                float diffR = COLOR_AMBIENT_SUNSET.r - COLOR_AMBIENT_MIDNIGHT.r;
                float diffG = COLOR_AMBIENT_SUNSET.g - COLOR_AMBIENT_MIDNIGHT.g;
                float diffB = COLOR_AMBIENT_SUNSET.b - COLOR_AMBIENT_MIDNIGHT.b;

                setAmbient(
                        COLOR_AMBIENT_SUNSET.r - diffR * _n,
                        COLOR_AMBIENT_SUNSET.g - diffG * _n,
                        COLOR_AMBIENT_SUNSET.b - diffB * _n
                );
            }

            AudioEngine.get().ambientVolume("ambience_day", 1f - normalized);
            AudioEngine.get().ambientVolume("ambience_night", normalized);
        } else if(worldTime >= ExpoTime.NIGHT || worldTime < ExpoTime.SUNRISE) {
            // 22:00 - 6:00
            float secondsPassed = (worldTime < ExpoTime.SUNRISE ? (worldTime + ExpoTime.worldDurationHours(2)) : (worldTime - ExpoTime.NIGHT));
            float normalized = secondsPassed / ExpoTime.worldDurationHours(8);
            setAmbient(COLOR_AMBIENT_MIDNIGHT.r, COLOR_AMBIENT_MIDNIGHT.g, COLOR_AMBIENT_MIDNIGHT.b);

            if(normalized < 0.125f) {
                // One hour
                worldSunShadowAlpha = normalized * 8;
            } else if(normalized < 0.875f) {
                worldSunShadowAlpha = 1.0f;
            } else {
                worldSunShadowAlpha = 1f - (normalized - 0.875f) * 8;
            }

            AudioEngine.get().ambientVolume("ambience_day", 0.0f);
            AudioEngine.get().ambientVolume("ambience_night", 1.0f);
        }
    }

    private void setAmbient(float r, float g, float b) {
        ambientLightingR = r;
        ambientLightingG = g;
        ambientLightingB = b;
    }

    private void calculateShadows(float normalized) {
        if(normalized < 0.5) {
            worldSunShadowX = MAX_SHADOW_X * (normalized * 2 - 1.0f);
            worldSunShadowY = MAX_SHADOW_Y - (MAX_SHADOW_Y * normalized * 2);
        } else {
            worldSunShadowX = MAX_SHADOW_X * (normalized - 0.5f);
            worldSunShadowY = MAX_SHADOW_Y * (normalized - 0.5f) * 2;
        }
    }

    /** Rendering the game world. */
    public void renderWorld() {
        RenderContext r = RenderContext.get();
        offset = 0;

        // Update camera
        r.expoCamera.update();

        { // Draw water tiles to water FBO
            r.waterTilesFbo.begin();
            transparentScreen();
            renderWater();
            r.waterTilesFbo.end();
        }

        {
            // Draw tiles to main FBO
            r.mainFbo.begin();
            transparentScreen();

            drawFboTexture(r.waterTilesFbo, r.DEFAULT_GLES3_SHADER);

            // Render chunks
            renderChunkTiles();
            r.mainFbo.end();
        }

        {
            // Draw shadows to shadow FBO
            r.shadowFbo.begin();
            transparentScreen();

            // Render shadows
            clientEntityManager.renderEntityShadows(r.delta);
            r.shadowFbo.end();
        }

        {
            // Draw shadow FBO to main FBO
            r.mainFbo.begin();
            r.batch.setColor(1.0f, 1.0f, 1.0f, 0.4f * worldSunShadowAlpha);
            drawFboTexture(r.shadowFbo, null);
            r.batch.setColor(Color.WHITE);

            // Render entities
            clientEntityManager.renderEntities(r.delta);
            r.mainFbo.end();
        }

        {
            // Draw final FBO with vignette shader
            drawFboTexture(r.mainFbo, r.vignetteShader);
        }

        // Draw light engine
        if(!isFullDay()) {
            r.lightEngine.setLighting(ambientLightingR, ambientLightingG, ambientLightingB, ambientLightingDarkness);
            r.lightEngine.render();
        }

        // Render tile debug data
        if(r.drawTileInfo) {
            r.chunkRenderer.begin(ShapeRenderer.ShapeType.Line);
            r.chunkRenderer.rect(r.mouseWorldGridX, r.mouseWorldGridY, TILE_SIZE, TILE_SIZE);
            r.chunkRenderer.end();
        }

        // Render debug shapes
        if(r.drawShapes) {
            r.chunkRenderer.begin(ShapeRenderer.ShapeType.Filled);

            for(ClientEntity all : clientEntityManager.allEntities()) {
                if(all.drawnLastFrame) {
                    float x = all.clientPosX;
                    float y = all.clientPosY;

                    float vx = x + all.drawOffsetX;
                    float vy = y + all.drawOffsetY;

                    if(all.drawWidth != 0 || all.drawHeight != 0) {
                        r.chunkRenderer.end();
                        r.chunkRenderer.begin(ShapeRenderer.ShapeType.Line);
                        r.chunkRenderer.setColor(Color.WHITE);

                        r.chunkRenderer.rect(vx, vy, all.drawWidth, all.drawHeight);
                        if(!(x == vx && y == vy)) {
                            r.chunkRenderer.line(x, y, vx, vy);
                        }

                        r.chunkRenderer.end();
                        r.chunkRenderer.begin(ShapeRenderer.ShapeType.Filled);
                    }

                    r.chunkRenderer.setColor(Color.CYAN);
                    r.chunkRenderer.circle(all.clientPosX, all.clientPosY, 1.0f, 8);
                    r.chunkRenderer.setColor(Color.CORAL);
                    r.chunkRenderer.circle(all.serverPosX, all.serverPosY, 0.65f, 8);
                    if(!(x == vx && y == vy)) {
                        r.chunkRenderer.setColor(Color.GREEN);
                        r.chunkRenderer.circle(vx, vy, 0.33f, 8);
                    }
                    r.chunkRenderer.setColor(Color.RED);
                    r.chunkRenderer.circle(all.drawRootX, all.drawRootY, 0.33f, 8);
                    r.chunkRenderer.setColor(Color.YELLOW);
                    r.chunkRenderer.circle(all.toVisualCenterX(), all.toVisualCenterY(), 0.33f, 8);
                }
            }

            r.chunkRenderer.end();

            r.hudBatch.begin();
            cursorText("clientPos", Color.CYAN);
            cursorText("serverPos", Color.CORAL);
            cursorText("drawPos", Color.GREEN);
            cursorText("drawRoot", Color.RED);
            cursorText("visualCenter", Color.YELLOW);
            r.hudBatch.end();
        }
    }

    private void transparentScreen() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }

    private void screencap(String name) {
        if(Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
            PixmapIO.writePNG(Gdx.files.external("TEST/" + System.currentTimeMillis() + "_" + name + ".png"), pixmap, Deflater.DEFAULT_COMPRESSION, true);
            pixmap.dispose();
        }
    }

    private void drawFboTexture(FrameBuffer fbo, ShaderProgram shader) {
        RenderContext r = RenderContext.get();

        float x = r.expoCamera.camera.position.x - Gdx.graphics.getWidth() * 0.5f;
        float y = r.expoCamera.camera.position.y - Gdx.graphics.getHeight() * 0.5f;
        TextureRegion fboTex = new TextureRegion(fbo.getColorBufferTexture());
        fboTex.flip(false, true);

        float newWidth = fboTex.getRegionWidth() * r.expoCamera.camera.zoom;
        float newHeight = fboTex.getRegionHeight() * r.expoCamera.camera.zoom;

        float diffWidth = (fboTex.getRegionWidth() - newWidth) * 0.5f;
        float diffHeight = (fboTex.getRegionHeight() - newHeight) * 0.5f;

        r.batch.begin();
        r.batch.setShader(shader);
        r.batch.draw(fboTex, x + diffWidth, y + diffHeight, newWidth, newHeight);
        r.batch.setShader(r.DEFAULT_GLES3_SHADER);
        r.batch.end();
    }

    private int offset = 0;

    private void cursorText(String text) {
        RenderContext r = RenderContext.get();
        r.m6x11_bordered.draw(r.hudBatch, text, r.mouseX, r.mouseY - 32 - offset);
        offset += 16;
    }

    private void cursorText(String text, Color color) {
        RenderContext r = RenderContext.get();
        Color prevColor = r.m6x11_bordered.getColor();
        r.m6x11_bordered.setColor(color);
        r.m6x11_bordered.draw(r.hudBatch, text, r.mouseX, r.mouseY - 32 - offset);
        r.m6x11_bordered.setColor(prevColor);
        offset += 16;
    }

    private boolean isFullDay() {
        return worldTime >= ExpoTime.DAY && worldTime < ExpoTime.SUNSET;
    }

    private void renderWater() {
        ClientPlayer p = ClientPlayer.getLocalPlayer();
        if(p == null) return;

        RenderContext r = RenderContext.get();
        int[] viewport = p.clientViewport;

        r.batch.setColor(Color.WHITE);

        r.waterTileShader.bind();
        r.waterTileShader.setUniformf("u_time", r.deltaTotal * r.waterSpeed);

        r.batch.setShader(r.waterTileShader);
        r.batch.begin();
        r.batch.setColor(r.waterColor[0], r.waterColor[1], r.waterColor[2], 1.0f);

        // Draw water
        int c = 0;

        for(int i = 0; i < PLAYER_CHUNK_VIEW_RANGE; i++) {
            for(int j = 0; j < PLAYER_CHUNK_VIEW_RANGE; j++) {
                int cx = viewport[0] + i; // CHUNK X
                int cy = viewport[2] + j; // CHUNK Y
                int px = ExpoShared.chunkToPos(cx); // CHUNK WORLD X
                int py = ExpoShared.chunkToPos(cy); // CHUNK WORLD Y

                ClientChunk chunk = clientChunkGrid.getChunk(cx, cy);

                if(chunk != null) {
                    if(chunk.chunkContainsWater) {
                        if(r.inDrawBounds(chunk)) {
                            for(int k = 0; k < chunk.biomeData.length; k++) {
                                if(BiomeType.isWater(chunk.biomeData[k]) || chunk.waterLoggedData[k]) {
                                    int tx = k % 8;
                                    int ty = k / 8;
                                    float wx = px + ExpoShared.tileToPos(tx);
                                    float wy = py + ExpoShared.tileToPos(ty);
                                    r.batch.draw(r.waterTexture, wx, wy);
                                }
                            }
                        }
                    }

                    drawChunks[c] = chunk;
                    c++;
                }
            }
        }

        r.batch.setColor(Color.WHITE);
        r.batch.end();
    }

    private void renderChunkTiles() {
        if(ClientPlayer.getLocalPlayer() != null) {
            RenderContext rc = RenderContext.get();

            rc.batch.setShader(rc.DEFAULT_GLES3_SHADER);
            rc.batch.begin();

            for(ClientChunk chunk : drawChunks) {
                if(chunk != null) {
                    if(rc.inDrawBounds(chunk)) {
                        int px = ExpoShared.chunkToPos(chunk.chunkX); // CHUNK WORLD X
                        int py = ExpoShared.chunkToPos(chunk.chunkY); // CHUNK WORLD Y

                        for(int k = 0; k < chunk.biomeData.length; k++) {
                            int tx = k % 8;
                            int ty = k / 8;
                            float wx = px + ExpoShared.tileToPos(tx);
                            float wy = py + ExpoShared.tileToPos(ty);

                            int tileIndex = chunk.tileTextureData[k];

                            if(tileIndex != 15 && !BiomeType.isWater(chunk.biomeData[k])) {
                                if(!chunk.waterLoggedData[k]) {
                                    // Soil.
                                    rc.batch.draw(ExpoAssets.get().soil, wx, wy);
                                }
                            }

                            TextureRegion tile = chunk.tileTextureRegionData[k];

                            if(tile != null) {
                                rc.batch.draw(tile, wx, wy);
                            }

                            if(chunk.waterLoggedData[k] && chunk.tileShadowData[k] != null) {
                                rc.batch.draw(chunk.tileShadowData[k], wx, wy - TILE_SIZE + chunk.tileShadowDataOffset[k]);
                            }
                        }
                    }
                }
            }

            rc.batch.end();
        }
    }

}
