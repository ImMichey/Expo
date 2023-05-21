package dev.michey.expo.logic.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.Expo;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.audio.AudioEngine;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.logic.entity.misc.ClientRaindrop;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityManager;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.logic.world.chunk.ClientChunk;
import dev.michey.expo.logic.world.chunk.ClientChunkGrid;
import dev.michey.expo.noise.TileLayerType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.gen.EntityPopulationBounds;
import dev.michey.expo.util.*;
import dev.michey.expo.weather.Weather;

import java.util.ConcurrentModificationException;

import static dev.michey.expo.util.ExpoShared.*;

public class ClientWorld {

    /** Entity handler */
    private final ClientEntityManager clientEntityManager;
    private final ClientChunkGrid clientChunkGrid;
    /** Optimization */
    private final ClientChunk[] drawChunks;

    /** Time */
    public float worldTime;
    public final float MAX_SHADOW_X = 1.8f;
    public final float MIN_SHADOW_Y = 0.6f;
    public final float MAX_SHADOW_Y = 1.5f;
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
    public final Color COLOR_RAIN = new Color(104f / 255f, 199f / 255f, 219f / 255f, 1.0f);

    /** Weather */
    public int worldWeather;
    public float weatherStrength;
    private float spawnRainDelta = 0.1f;
    private float rainAmbienceVolume = 0f;

    public ClientWorld() {
        clientEntityManager = new ClientEntityManager();
        clientChunkGrid = new ClientChunkGrid();
        drawChunks = new ClientChunk[PLAYER_CHUNK_VIEW_RANGE * PLAYER_CHUNK_VIEW_RANGE];
    }

    /** Ticking the game world. */
    public void tickWorld(float delta, float serverDelta) {
        // Tick chunks
        clientChunkGrid.tick(delta);

        // Tick entities
        clientEntityManager.tickEntities(delta);

        // Tick camera
        RenderContext.get().expoCamera.tick();

        // Calculate world time
        calculateWorldTime(delta);

        // Weather tick
        if(worldWeather == Weather.RAIN.WEATHER_ID) {
            spawnRainDelta -= delta;

            if(spawnRainDelta <= 0) {
                spawnRainDelta += 0.1f;
                spawnRain();
            }
        }

        if(worldWeather == Weather.RAIN.WEATHER_ID) {
            if(rainAmbienceVolume < 1f) {
                rainAmbienceVolume += delta / 3;
                if(rainAmbienceVolume > 1) rainAmbienceVolume = 1;
            }
        } else {
            if(rainAmbienceVolume > 0f) {
                rainAmbienceVolume -= delta / 3;
                if(rainAmbienceVolume < 0) rainAmbienceVolume = 0;
            }
        }

        AudioEngine.get().ambientVolume("ambience_rain", rainAmbienceVolume);
    }

    private void spawnRain() {
        float zoom = RenderContext.get().expoCamera.camera.zoom;
        if(zoom > 0.75f) zoom = 0.75f;

        float mul = zoom / (1f / 3f) + 1f;
        int amount = (int) (weatherStrength * mul);

        Vector2 basePos = InputUtils.topLeftRainCorner(0, 0);
        Vector2 rightBottomCorner = InputUtils.topLeftRainCorner(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        float baseDiffX = rightBottomCorner.x - basePos.x;
        float baseDiffY = rightBottomCorner.y - basePos.y;

        if(mul != 1) {
            float newMul = mul - 1;
            float additionX = baseDiffX * newMul * 0.5f;
            float additionY = baseDiffY * newMul * 0.5f;

            rightBottomCorner.x += additionX;
            basePos.x -= additionX;

            rightBottomCorner.y += additionY;
            basePos.y -= additionY;
        }

        float diffX = (rightBottomCorner.x - basePos.x);
        float diffY = (rightBottomCorner.y - basePos.y);

        float minStr = Weather.RAIN.WEATHER_DATA[2];
        float maxStr = Weather.RAIN.WEATHER_DATA[3];
        float normStr = (weatherStrength - minStr) / (maxStr - minStr);

        for(int i = 0; i < amount; i++) {
            ClientRaindrop raindrop = new ClientRaindrop();
            float groundYBonus = diffY * MathUtils.random();

            float x = basePos.x + MathUtils.random(diffX);
            float y = basePos.y + diffY * 0.25f - diffY * MathUtils.random();

            float vx = MathUtils.random(100f, 150f) * normStr;
            float vy = -256f - (groundYBonus / diffY * 128f);

            float rot = normStr * 45f;

            raindrop.initRaindrop(x, y, y + groundYBonus, rot, vx, vy);
            raindrop.depth = y + groundYBonus;

            clientEntityManager.addClientSideEntity(raindrop);
        }
    }

    public void setNoiseSeed(int seed) {
        clientChunkGrid.terrainNoiseHeight.setSeed(seed);
        clientChunkGrid.terrainNoiseTemperature.setSeed(seed + 1);
        clientChunkGrid.terrainNoiseMoisture.setSeed(seed + 2);
        clientChunkGrid.riverNoise.setSeed(seed);
        for(var x : clientChunkGrid.noisePostProcessorMap.values()) x.value.setSeed(seed);
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
            worldSunShadowY = MIN_SHADOW_Y + (MAX_SHADOW_Y - MIN_SHADOW_Y) - ((MAX_SHADOW_Y - MIN_SHADOW_Y) * normalized * 2);
        } else {
            float adjusted = (normalized - 0.5f) * 2;

            worldSunShadowX = MAX_SHADOW_X * adjusted;
            worldSunShadowY = MIN_SHADOW_Y + (MAX_SHADOW_Y - MIN_SHADOW_Y) * adjusted;
        }
    }

    public void renderWorld() {
        RenderContext r = RenderContext.get();
        offset = 0;

        // Update camera.
        r.expoCamera.update();
        r.batch.setShader(r.DEFAULT_GLES3_SHADER);

        {
            // Draw tiles to main FBO.
            r.mainFbo.begin();
                transparentScreen();
                updateChunksToDraw();
                renderChunkTiles();
            r.mainFbo.end();
        }

        {
            // Draw shadows to shadow FBO.
            r.shadowFbo.begin();
                transparentScreen();
                clientEntityManager.renderEntityShadows(r.delta);
            r.shadowFbo.end();
        }

        {
            // Draw entities to entity FBO.
            r.entityFbo.begin();
                transparentScreen();
                clientEntityManager.renderEntities(r.delta);
            r.entityFbo.end();
        }

        {
            // Draw shadow FBO to main FBO.
            r.mainFbo.begin();
                r.batch.setColor(1.0f, 1.0f, 1.0f, 0.4f * worldSunShadowAlpha);
                drawFboTexture(r.shadowFbo, null);
                r.batch.setColor(Color.WHITE);
                r.batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
                drawFboTexture(r.entityFbo, null);
                r.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA); //default blend mode
            r.mainFbo.end();
        }

        {
            boolean displayBlur = r.blurActive || r.blurStrength > 0;
            float BLUR_SPEED = 4.0f;
            float MAX_BLUR = 1.0f;
            float blurSign = r.blurActive ? 1.0f : -1.0f;

            if(displayBlur) {
                r.blurDelta += r.delta * BLUR_SPEED * blurSign;
                r.blurDelta = MathUtils.clamp(r.blurDelta, 0.0f, 1.0f);

                r.blurStrength = Interpolation.smooth2.apply(r.blurDelta) * MAX_BLUR;
            }

            // Draw final FBO with vignette shader.
            if(displayBlur) {
                blurPass();
                drawFboTexture(r.blurTargetBFbo, r.vignetteShader);
            } else {
                drawFboTexture(r.mainFbo, r.vignetteShader);
            }
        }

        {
            // Draw light engine.
            if(!isFullDay()) {
                r.lightEngine.setLighting(ambientLightingR, ambientLightingG, ambientLightingB, ambientLightingDarkness);
                r.lightEngine.render();
            }
        }

        {
            // Draw debug info.
            if(r.drawTileInfo) {
                r.chunkRenderer.begin(ShapeRenderer.ShapeType.Line);
                r.chunkRenderer.setColor(Color.WHITE);
                r.chunkRenderer.rect(r.mouseWorldGridX, r.mouseWorldGridY, TILE_SIZE, TILE_SIZE);
                r.chunkRenderer.end();

                var tileMap = ExpoAssets.get().getTileSheet().getTilesetTextureMap();
                int amount = tileMap.size();

                int breakLine = 0;
                int lines = 0;
                int space = 2;
                int drawPerLine = 16;
                int tileSize = 32;
                int totalLines = amount / drawPerLine + 1;

                r.hudBatch.begin();
                r.hudBatch.setColor(Color.BLACK);
                r.hudBatch.draw(ExpoAssets.get().textureRegion("square16x16"),
                        r.mouseX + 50 - 4,
                        r.mouseY + 50 - 4 - (totalLines - 1) * tileSize + ((totalLines - 1) * space) - 8,
                        (tileSize + space) * 16 + 8,
                        totalLines * tileSize + ((totalLines - 1) * space) + 8);
                r.hudBatch.setColor(Color.WHITE);

                for(int i = 0; i < amount; i++) {
                    float tileX = r.mouseX + 50 + (tileSize + space) * i - lines * (tileSize + space) * drawPerLine;
                    float tileY = r.mouseY + 50 - lines * (tileSize + space);

                    r.hudBatch.draw(tileMap.get(i), tileX, tileY, tileSize, tileSize);

                    breakLine++;

                    if(breakLine == drawPerLine) {
                        breakLine = 0;
                        lines++;
                    }
                }

                breakLine = 0;
                lines = 0;

                for(int i = 0; i < amount; i++) {
                    float tileX = r.mouseX + 50 + (tileSize + space) * i - lines * (tileSize + space) * drawPerLine;
                    float tileY = r.mouseY + 50 - lines * (tileSize + space);

                    r.m5x7_border_all[1].draw(r.hudBatch, String.valueOf(i), tileX + 2, tileY + 14 + 9);

                    breakLine++;

                    if(breakLine == drawPerLine) {
                        breakLine = 0;
                        lines++;
                    }
                }

                r.hudBatch.end();
            }

            // Render debug shapes
            if(Expo.get().getImGuiExpo() != null) {
                r.chunkRenderer.begin(ShapeRenderer.ShapeType.Filled);

                for(ClientEntity all : clientEntityManager.allEntities()) {
                    if(all.visibleToRenderEngine) {
                        float dpx = all.finalDrawPosX;
                        float dpy = all.finalDrawPosY;

                        float tpx = all.finalTextureStartX;
                        float tpy = all.finalTextureStartY;

                        if(Expo.get().getImGuiExpo().renderDrawPos.get()) {
                            r.chunkRenderer.setColor(Color.GREEN);
                            r.chunkRenderer.circle(dpx, dpy, 0.4f, 8);
                            r.chunkRenderer.setColor(Color.BLACK);
                            r.chunkRenderer.circle(tpx, tpy, 0.3f, 8);

                            if(all.textureWidth != 0 || all.textureHeight != 0) {
                                r.chunkRenderer.end();

                                r.chunkRenderer.begin(ShapeRenderer.ShapeType.Line);
                                r.chunkRenderer.setColor(Color.WHITE);
                                r.chunkRenderer.rect(tpx, tpy, all.textureWidth, all.textureHeight);

                                r.chunkRenderer.end();
                                r.chunkRenderer.begin(ShapeRenderer.ShapeType.Filled);
                            }
                        }

                        if(Expo.get().getImGuiExpo().renderClientPos.get()) {
                            r.chunkRenderer.setColor(Color.CYAN);
                            r.chunkRenderer.circle(all.clientPosX, all.clientPosY, 0.8f, 8);
                        }

                        if(Expo.get().getImGuiExpo().renderServerPos.get()) {
                            r.chunkRenderer.setColor(Color.CORAL);
                            r.chunkRenderer.circle(all.serverPosX, all.serverPosY, 0.6f, 8);
                        }

                        if(Expo.get().getImGuiExpo().renderDrawRoot.get()) {
                            r.chunkRenderer.setColor(Color.RED);
                            r.chunkRenderer.circle(all.finalTextureCenterX, all.finalTextureRootY, 0.33f, 8);
                        }

                        if(Expo.get().getImGuiExpo().renderVisualCenter.get()) {
                            r.chunkRenderer.setColor(Color.YELLOW);
                            r.chunkRenderer.circle(all.finalTextureCenterX, all.finalTextureCenterY, 0.33f, 8);
                        }

                        if(Expo.get().getImGuiExpo().renderInteractionPoints.get()) {
                            if(all instanceof SelectableEntity sel) {
                                r.chunkRenderer.setColor(Color.PURPLE);
                                float[] points = sel.interactionPoints();

                                for(int i = 0; i < points.length; i += 2) {
                                    r.chunkRenderer.circle(points[i], points[i + 1], 0.5f, 8);
                                }
                            }

                            if(all instanceof ClientPlayer cp) {
                                r.chunkRenderer.setColor(Color.PINK);
                                r.chunkRenderer.circle(cp.playerReachCenterX, cp.playerReachCenterY, 0.5f, 8);

                                if(ClientPlayer.getLocalPlayer() == cp) {
                                    ClientEntity sel = cp.entityManager().selectedEntity;

                                    if(sel != null) {
                                        r.chunkRenderer.end();
                                        r.chunkRenderer.begin(ShapeRenderer.ShapeType.Line);
                                        r.chunkRenderer.line(cp.playerReachCenterX, cp.playerReachCenterY, sel.clientPosX, sel.clientPosY);
                                        r.chunkRenderer.end();
                                        r.chunkRenderer.begin(ShapeRenderer.ShapeType.Filled);
                                    }
                                }
                            }
                        }

                        if(Expo.get().getImGuiExpo().renderEntityId.get()) {
                            r.chunkRenderer.end();

                            r.hudBatch.begin();
                            Vector2 p = ClientUtils.entityPosToHudPos(all.clientPosX, all.clientPosY);
                            r.m5x7_border_all[0].draw(r.hudBatch, String.valueOf(all.entityId), p.x, p.y);
                            r.hudBatch.end();

                            r.chunkRenderer.begin(ShapeRenderer.ShapeType.Line);
                        }

                        if(Expo.get().getImGuiExpo().renderEntityBbox.get()) {
                            if(ServerWorld.get() != null) {
                                var type = EntityPopulationBounds.get().getFor(all.getEntityType().ENTITY_SERVER_TYPE);

                                if(type != null) {
                                    float[] pos = type.toWorld(all.serverPosX, all.serverPosY);
                                    r.chunkRenderer.end();
                                    r.chunkRenderer.begin(ShapeRenderer.ShapeType.Line);
                                    r.chunkRenderer.setColor(Color.PURPLE);
                                    r.chunkRenderer.rect(pos[0], pos[1], pos[2] - pos[0], pos[3] - pos[1]);
                                }
                            }
                        }

                        if(Expo.get().getImGuiExpo().renderJBump.get()) {
                            if(ServerWorld.get() != null) {
                                r.chunkRenderer.end();
                                r.chunkRenderer.begin(ShapeRenderer.ShapeType.Line);
                                r.chunkRenderer.setColor(Color.PINK);
                                var world = ServerWorld.get().getMainDimension().getPhysicsWorld();

                                try {
                                    for(var o : world.getRects()) {
                                        r.chunkRenderer.rect(o.x, o.y, o.w, o.h);
                                    }
                                } catch (ConcurrentModificationException e) {
                                    // ignore it because it happens due to different tick rates between client->localserver
                                    // and JBump doesn't offer any ability to get the physic bodies thread-safe
                                }
                            }
                        }

                        if(Expo.get().getImGuiExpo().renderChunkBorders.get()) {
                            r.chunkRenderer.end();
                            r.chunkRenderer.begin(ShapeRenderer.ShapeType.Line);

                            for(var c : drawChunks) {
                                r.chunkRenderer.rect(c.chunkDrawBeginX, c.chunkDrawBeginY, CHUNK_SIZE, CHUNK_SIZE);
                            }
                        }
                    }
                }

                r.chunkRenderer.end();
            }
        }
    }

    private void updateChunksToDraw() {
        ClientPlayer player = ClientPlayer.getLocalPlayer();
        if(player == null) return;

        int[] viewport = player.clientViewport;
        int c = 0;

        for(int i = 0; i < PLAYER_CHUNK_VIEW_RANGE; i++) {
            for(int j = 0; j < PLAYER_CHUNK_VIEW_RANGE; j++) {
                int cx = viewport[0] + i; // CHUNK X
                int cy = viewport[2] + j; // CHUNK Y

                ClientChunk chunk = clientChunkGrid.getChunk(cx, cy);

                if(chunk != null) {
                    drawChunks[c] = chunk;
                    drawChunks[c].updateVisibility();
                    c++;
                }
            }
        }
    }

    private void blurPass() {
        RenderContext r = RenderContext.get();

        r.blurShader.bind();
        r.blurShader.setUniformf("u_radius", r.blurStrength);
        r.blurShader.setUniformf("u_dir", 1.0f, 0.0f);
        r.blurShader.setUniformf("u_resolution", Gdx.graphics.getWidth());

        {
            r.blurTargetAFbo.begin();
            drawFboTexture(r.mainFbo, r.blurShader);
            r.blurTargetAFbo.end();
        }

        r.blurShader.bind();
        r.blurShader.setUniformf("u_dir", 0.0f, 1.0f);
        r.blurShader.setUniformf("u_resolution", Gdx.graphics.getHeight());

        {
            r.blurTargetBFbo.begin();
            drawFboTexture(r.blurTargetAFbo, r.blurShader);
            r.blurTargetBFbo.end();
        }
    }

    private void transparentScreen() {
        Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
        Gdx.gl.glClear(GL30.GL_COLOR_BUFFER_BIT);
    }

    private void drawFboTexture(Texture texture, ShaderProgram shader) {
        RenderContext r = RenderContext.get();

        float x = r.expoCamera.camera.position.x - Gdx.graphics.getWidth() * 0.5f;
        float y = r.expoCamera.camera.position.y - Gdx.graphics.getHeight() * 0.5f;
        TextureRegion fboTex = new TextureRegion(texture);
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

    private void drawFboTexture(FrameBuffer fbo, ShaderProgram shader) {
        drawFboTexture(fbo.getColorBufferTexture(), shader);
    }

    private int offset = 0;

    private void cursorText(String text) {
        RenderContext r = RenderContext.get();
        r.m6x11_bordered.draw(r.hudBatch, text, r.mouseX, r.mouseY - 32 - offset);
        offset += 16;
    }

    private void cursorText(String text, Color color) {
        RenderContext r = RenderContext.get();
        Color prevColor = r.m5x7_border_all[1].getColor();
        r.m5x7_border_all[1].setColor(color);
        r.m5x7_border_all[1].draw(r.hudBatch, text, r.mouseX, r.mouseY - 32 - offset);
        r.m5x7_border_all[1].setColor(prevColor);
        offset += 16;
    }

    private boolean isFullDay() {
        return worldTime >= ExpoTime.DAY && worldTime < ExpoTime.SUNSET;
    }

    private void drawWater() {
        RenderContext r = RenderContext.get();

        r.batch.end();

        r.waterDelta += r.delta * r.waterSpeed;

        r.waterShader.bind();
        r.waterShader.setUniformf("u_time", r.waterDelta);

        Gdx.gl.glActiveTexture(Gdx.gl.GL_TEXTURE1);
        r.displacementTexture.bind(1);
        r.waterShader.setUniformi("u_displacement", 1);

        Gdx.gl.glActiveTexture(Gdx.gl.GL_TEXTURE0);

        r.batch.begin();
        r.batch.setShader(r.waterShader);
        r.batch.setColor(r.waterColor[0], r.waterColor[1], r.waterColor[2], r.waterAlpha);

        r.batch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);

        float textureSize = r.waterNoiseTexture.getWidth();

        for(ClientChunk chunk : drawChunks) {
            if(chunk != null && chunk.visible) {
                for(int k = 0; k < chunk.biomes.length; k++) {
                    boolean isWaterTile = TileLayerType.isWater(chunk.layerTypes[k][1]);

                    if(isWaterTile) {
                        int tx = k % 8;
                        int ty = k / 8;
                        float wx = chunk.chunkDrawBeginX + ExpoShared.tileToPos(tx);
                        float wy = chunk.chunkDrawBeginY + ExpoShared.tileToPos(ty);

                        float normX = (wx % textureSize) / textureSize;
                        float normY = (wy % textureSize) / textureSize;

                        float uv = 16.0f / textureSize;
                        r.batch.draw(r.waterNoiseTexture, wx, wy, TILE_SIZE, TILE_SIZE, normX, normY, normX + uv, normY + uv);
                    }
                }
            }
        }

        r.batch.end();
        r.batch.setShader(r.DEFAULT_GLES3_SHADER);
        r.batch.setColor(Color.WHITE);
    }

    private void drawLayer(int k, TextureRegion[] layer, RenderContext rc, float wx, float wy, Pair[][] displacementPairs) {
        Pair[] displacement = displacementPairs != null ? displacementPairs[k] : null;

        if(layer.length == 1) {
            TextureRegion t = layer[0];
            if(t == null) return;
            rc.batch.draw(t, wx, wy);
        } else {
            if(displacement == null) {
                rc.batch.draw(layer[0], wx, wy);
                rc.batch.draw(layer[1], wx + 8, wy);
                rc.batch.draw(layer[2], wx, wy + 8);
                rc.batch.draw(layer[3], wx + 8, wy + 8);
            } else {
                float val = clientChunkGrid.interpolation;
                rc.batch.draw(layer[0], wx + val * (int) displacement[0].key, wy + val * (int) displacement[0].value);
                rc.batch.draw(layer[1], wx + 8 + val * (int) displacement[1].key, wy + val * (int) displacement[1].value);
                rc.batch.draw(layer[2], wx + val * (int) displacement[2].key, wy + 8 + val * (int) displacement[2].value);
                rc.batch.draw(layer[3], wx + 8 + val * (int) displacement[3].key, wy + 8 + val * (int) displacement[3].value);
            }
        }
    }

    private void renderChunkTiles() {
        if(ClientPlayer.getLocalPlayer() != null) {
            RenderContext rc = RenderContext.get();

            rc.batch.begin();

            for(ClientChunk chunk : drawChunks) {
                if(chunk != null && chunk.visible) {
                    for(int k = 0; k < chunk.biomes.length; k++) {
                        int tx = k % 8;
                        int ty = k / 8;
                        float wx = chunk.chunkDrawBeginX + ExpoShared.tileToPos(tx);
                        float wy = chunk.chunkDrawBeginY + ExpoShared.tileToPos(ty);

                        if(chunk.layer1Tex[k].length == 1) {
                            // check if full tile or not.
                            if(!ExpoAssets.get().getTileSheet().isFullTile(chunk.layer1[k][0])) {
                                drawLayer(k, chunk.layer0Tex[k], rc, wx, wy, null);
                            }
                        } else {
                            drawLayer(k, chunk.layer0Tex[k], rc, wx, wy, null);
                        }
                        drawLayer(k, chunk.layer1Tex[k], rc, wx, wy, chunk.layer1Displacement);
                        drawLayer(k, chunk.layer2Tex[k], rc, wx, wy, null);
                    }
                }
            }

            // Draw water here (debug)
            drawWater();
        }
    }

    public ClientChunkGrid getClientChunkGrid() {
        return clientChunkGrid;
    }

}