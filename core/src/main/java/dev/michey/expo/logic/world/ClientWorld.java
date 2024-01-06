package dev.michey.expo.logic.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.dongbat.jbump.Item;
import com.dongbat.jbump.Rect;
import com.dongbat.jbump.World;
import dev.michey.expo.Expo;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.audio.AudioEngine;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityManager;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.arch.SelectableEntity;
import dev.michey.expo.logic.entity.misc.ClientDynamic3DTile;
import dev.michey.expo.logic.entity.misc.ClientPuddle;
import dev.michey.expo.logic.entity.misc.ClientRaindrop;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.logic.world.chunk.ClientChunk;
import dev.michey.expo.logic.world.chunk.ClientChunkGrid;
import dev.michey.expo.logic.world.chunk.ClientDynamicTilePart;
import dev.michey.expo.noise.TileLayerType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.imgui.DebugPoint;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.render.ui.PlayerUI;
import dev.michey.expo.server.main.logic.entity.arch.DamageableEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.bbox.EntityHitbox;
import dev.michey.expo.server.util.EntityMetadataMapper;
import dev.michey.expo.util.*;
import dev.michey.expo.weather.Weather;

import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.ListIterator;

import static dev.michey.expo.util.ExpoShared.*;

public class ClientWorld {

    /** Entity handler */
    private final ClientEntityManager clientEntityManager;
    private final ClientChunkGrid clientChunkGrid;

    /** Client-side physics simulation */
    private final World<ClientEntity> clientPhysicsWorld;

    /** Optimization */
    private final ClientChunk[] drawChunks;
    private final int[] cachedViewport = new int[] {Integer.MAX_VALUE, 0, 0, 0};
    private final LinkedList<QueuedAmbientOcclusion> ambientOcclusionQueue;

    /** Time */
    public float worldTime;
    public static final float MAX_SHADOW_X = 1.75f;
    public static final float MIN_SHADOW_Y = 0.5f;
    public static final float MAX_SHADOW_Y = 1.5f;
    public static final float SHADOW_ALPHA_TRANSITION_DURATION = 2.0f;
    public static final Interpolation SHADOW_ALPHA_INTERPOLATION = Interpolation.fade;
    public float worldSunShadowX = MAX_SHADOW_X;
    public float worldSunShadowY = MAX_SHADOW_Y;
    public float worldSunShadowAlpha = 1.0f;
    public float ambientLightingR = 1.0f;
    public float ambientLightingG = 1.0f;
    public float ambientLightingB = 1.0f;
    public float ambientLightingDarkness = 1.0f;
    // public final Color COLOR_AMBIENT_MIDNIGHT = new Color(24f / 255f, 30f / 255f, 66f / 255f, 1.0f);
    public final Color COLOR_AMBIENT_MIDNIGHT = Color.valueOf("#253676");
    public final Color COLOR_AMBIENT_SUNRISE = new Color(241f / 255f, 241f / 255f, 197f / 255f, 1.0f);
    public final Color COLOR_AMBIENT_SUNSET = new Color(222f / 255f, 177f / 255f, 128f / 255f, 1.0f);
    public final Color COLOR_RAIN = new Color(104f / 255f, 199f / 255f, 219f / 255f, 1.0f);
    private boolean playedSunriseSound;
    private boolean playedSunsetSound;

    /** Weather */
    public int worldWeather;
    public float weatherStrength;
    private float spawnRainDelta = 0.1f;
    private float rainAmbienceVolume = 0f;

    public ClientWorld() {
        clientEntityManager = new ClientEntityManager();
        clientChunkGrid = new ClientChunkGrid();
        drawChunks = new ClientChunk[PLAYER_CHUNK_VIEW_RANGE_X * PLAYER_CHUNK_VIEW_RANGE_Y];
        ambientOcclusionQueue = new LinkedList<>();
        clientPhysicsWorld = new World<>(64) {

            @Override
            public void remove(Item item) {
                // Band-aid fix to stupid JBump :D
                Rect rect = getRect(item);
                if(rect == null) return;
                super.remove(item);
            }

        };
    }

    /** Ticking the game world. */
    public void tickWorld(float delta, float serverDelta) {
        // Tick chunks
        clientChunkGrid.tick(delta);

        // Tick entities
        clientEntityManager.tickEntities(delta);
        //clientChunkGrid.runPostAmbientOcclusion();

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

        float fn = (PlayerUI.get().fadeInDelta / PlayerUI.get().fadeInDuration);
        AudioEngine.get().ambientVolume("ambience_rain", rainAmbienceVolume * fn);
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

            float vx = MathUtils.random(128f, 192f) * normStr;
            float vy = -256f - (groundYBonus / diffY * 128f);

            float rot = normStr * 22.5f + 22.5f;

            raindrop.initRaindrop(x, y, y + groundYBonus, rot, vx, vy);
            raindrop.depth = y + groundYBonus;

            clientEntityManager.addClientSideEntity(raindrop);
        }
    }

    public void setNoiseSeed(int seed) {
        clientChunkGrid.terrainNoiseHeight.setSeed(seed);
        clientChunkGrid.terrainNoiseTemperature.setSeed(seed + 1);
        clientChunkGrid.terrainNoiseMoisture.setSeed(seed + 2);
    }

    private void calculateWorldTime(float delta) {
        float oldWorldTime = worldTime;
        worldTime += delta;
        if(worldTime >= ExpoTime.WORLD_CYCLE_DURATION) worldTime %= ExpoTime.WORLD_CYCLE_DURATION;

        if(worldTime >= ExpoTime.SUNRISE && oldWorldTime < ExpoTime.SUNRISE && !playedSunriseSound) {
            playedSunriseSound = true;
            AudioEngine.get().playSoundGroup("rooster", 0.5f);
            PlayerUI.get().addNotification(ExpoAssets.get().textureRegion("icon_sun"), 5.0f, null, "The sun is rising.");
        }
        if(worldTime >= ExpoTime.SUNSET || worldTime < ExpoTime.SUNRISE) {
            playedSunriseSound = false;
        }
        if(worldTime >= ExpoTime.SUNSET && oldWorldTime < ExpoTime.SUNSET && !playedSunsetSound) {
            playedSunsetSound = true;
            PlayerUI.get().addNotification(ExpoAssets.get().textureRegion("icon_moon"), 5.0f, "notification", "The sun is setting.");
        }
        if(worldTime < ExpoTime.SUNSET) {
            playedSunsetSound = false;
        }

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
            float fn = (PlayerUI.get().fadeInDelta / PlayerUI.get().fadeInDuration);
            AudioEngine.get().ambientVolume("ambience_day", fn);
            AudioEngine.get().ambientVolume("ambience_night", 0.0f);
        } else if(worldTime >= ExpoTime.SUNRISE && worldTime < ExpoTime.DAY) {
            // 6:00 - 8:00
            float secondsPassed = worldTime - ExpoTime.SUNRISE;
            float normalized = secondsPassed / ExpoTime.worldDurationHours(2);

            if(secondsPassed <= SHADOW_ALPHA_TRANSITION_DURATION) {
                worldSunShadowAlpha = SHADOW_ALPHA_INTERPOLATION.apply(secondsPassed / SHADOW_ALPHA_TRANSITION_DURATION);
            } else {
                worldSunShadowAlpha = 1.0f;
            }

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

            float fn = (PlayerUI.get().fadeInDelta / PlayerUI.get().fadeInDuration);
            AudioEngine.get().ambientVolume("ambience_day", normalized * fn);
            AudioEngine.get().ambientVolume("ambience_night", (1f - normalized) * fn);
        } else if(worldTime >= ExpoTime.SUNSET && worldTime < ExpoTime.NIGHT) {
            // 20:00 - 22:00
            float secondsPassed = worldTime - ExpoTime.SUNSET;
            float normalized = secondsPassed / ExpoTime.worldDurationHours(2);

            float startAlphaTimer = ExpoTime.worldDurationHours(2) - SHADOW_ALPHA_TRANSITION_DURATION;

            if(secondsPassed >= startAlphaTimer) {
                worldSunShadowAlpha = SHADOW_ALPHA_INTERPOLATION.apply(1.0f - (secondsPassed - startAlphaTimer) / SHADOW_ALPHA_TRANSITION_DURATION);
            } else {
                worldSunShadowAlpha = 1.0f;
            }

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

            float fn = (PlayerUI.get().fadeInDelta / PlayerUI.get().fadeInDuration);
            AudioEngine.get().ambientVolume("ambience_day", (1f - normalized) * fn);
            AudioEngine.get().ambientVolume("ambience_night", normalized * fn);
        } else if(worldTime >= ExpoTime.NIGHT || worldTime < ExpoTime.SUNRISE) {
            // 22:00 - 6:00
            float secondsPassed = (worldTime < ExpoTime.SUNRISE ? (worldTime + ExpoTime.worldDurationHours(2)) : (worldTime - ExpoTime.NIGHT));
            //float normalized = secondsPassed / ExpoTime.worldDurationHours(8);
            setAmbient(COLOR_AMBIENT_MIDNIGHT.r, COLOR_AMBIENT_MIDNIGHT.g, COLOR_AMBIENT_MIDNIGHT.b);

            float startAlphaTimer = ExpoTime.worldDurationHours(8) - SHADOW_ALPHA_TRANSITION_DURATION;

            if(secondsPassed <= SHADOW_ALPHA_TRANSITION_DURATION) {
                worldSunShadowAlpha = SHADOW_ALPHA_INTERPOLATION.apply(secondsPassed / SHADOW_ALPHA_TRANSITION_DURATION);
            } else if(secondsPassed >= startAlphaTimer) {
                worldSunShadowAlpha = SHADOW_ALPHA_INTERPOLATION.apply(1.0f - (secondsPassed - startAlphaTimer) / SHADOW_ALPHA_TRANSITION_DURATION);
            } else {
                worldSunShadowAlpha = 1.0f;
            }

            float fn = (PlayerUI.get().fadeInDelta / PlayerUI.get().fadeInDuration);
            AudioEngine.get().ambientVolume("ambience_day", 0.0f);
            AudioEngine.get().ambientVolume("ambience_night", fn);
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

    private void drawShadowFbo(RenderContext r, ShaderProgram shader, Texture lookupTexture) {
        r.batch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);

        if(shader != null) {
            shader.bind();

            Gdx.gl.glActiveTexture(Gdx.gl.GL_TEXTURE1);
            lookupTexture.bind(1);
            shader.setUniformi("u_lookup", 1);

            Gdx.gl.glActiveTexture(Gdx.gl.GL_TEXTURE0);
        }

        r.batch.setColor(1.0f, 1.0f, 1.0f, 0.4f * worldSunShadowAlpha);
        drawFboTexture(r.shadowFbo, shader);
        r.batch.setColor(Color.WHITE);
    }

    public void renderWorld() {
        RenderContext r = RenderContext.get();
        offset = 0;

        // Update camera.
        r.expoCamera.update();
        r.batch.setShader(r.DEFAULT_GLES3_SHADER);

        {
            // Draw shadows to shadow FBO.
            r.shadowFbo.begin();
                transparentScreen();
                clientEntityManager.renderEntityShadows(r.delta);
            r.shadowFbo.end();
        }

        {
            // Draw non-water tiles to tiles FBO.
            r.tilesFbo.begin();
                transparentScreen();
                renderChunkTiles(false);
                drawShadowFbo(r, r.simplePassthroughShader, r.tilesFbo.getColorBufferTexture());
            r.tilesFbo.end();
        }

        {
            r.waterEntityFbo.begin();
                transparentScreen();
                renderEntityReflections();
            r.waterEntityFbo.end();

            // Draw water tiles, reflections and shadows to waterTilesFbo.
            r.waterTilesFbo.begin();
                transparentScreen();
                renderWaterTiles();
                r.batch.setColor(0.666f, 0.875f, 1.0f, 0.5f);
                drawFboTexture(r.waterEntityFbo, null);
                r.batch.setColor(Color.WHITE);
                drawShadowFbo(r, r.simplePassthroughShader, r.waterTilesFbo.getColorBufferTexture());
            r.waterTilesFbo.end();
        }

        {
            // Draw waterTilesFbo to waterReflectionFbo with shader.
            r.waterReflectionFbo.begin();
                transparentScreen();

                r.waterDistortionShader.bind();
                r.waterDistortionShader.setUniformf("u_time", r.deltaTotal * r.waterReflectionSpeed);
                r.waterDistortionShader.setUniformf("u_screenSize", Gdx.graphics.getWidth() * r.expoCamera.camera.zoom, Gdx.graphics.getHeight() * r.expoCamera.camera.zoom);
                r.waterDistortionShader.setUniformf("u_cameraPos", r.expoCamera.camera.position.x, r.expoCamera.camera.position.y);
                r.waterDistortionShader.setUniformf("u_waterSkewX", r.waterSkewX);
                r.waterDistortionShader.setUniformf("u_waterSkewY", r.waterSkewY);

                drawFboTexture(r.waterTilesFbo, r.waterDistortionShader);
            r.waterReflectionFbo.end();
        }

        { // Draw tiles to main FBO.
            r.mainFbo.begin();
                transparentScreen();
                r.batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
                drawFboTexture(r.waterReflectionFbo, null);
                drawFboTexture(r.tilesFbo, null);
                r.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA); //default blend mode
            r.mainFbo.end();
        }

        { // Draw water overlay tiles to tiles FBO and re-draw one final time.
            r.tilesFbo.begin();
                transparentScreen();
                renderChunkTiles(true);

                drawShadowFbo(r, r.simplePassthroughShader, r.tilesFbo.getColorBufferTexture());
            r.tilesFbo.end();

            r.mainFbo.begin();
                drawFboTexture(r.tilesFbo, null);
            r.mainFbo.end();
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
                r.batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
                drawFboTexture(r.entityFbo, null);
                r.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA); //default blend mode
            r.mainFbo.end();
        }

        {
            boolean displayBlur = GameSettings.get().enableBlur && (r.blurActive || r.blurStrength > 0);
            float BLUR_SPEED = 4.0f;
            float MAX_BLUR = 1.0f;
            float blurSign = r.blurActive ? 1.0f : -1.0f;

            if(displayBlur) {
                r.blurDelta += r.delta * BLUR_SPEED * blurSign;
                r.blurDelta = MathUtils.clamp(r.blurDelta, 0.0f, 1.0f);

                r.blurStrength = Interpolation.smooth2.apply(r.blurDelta) * MAX_BLUR;
            }

            // Draw final FBO with vignette shader.
            {
                r.vignetteShader.bind();
                float blinkDelta = ClientPlayer.getLocalPlayer() == null ? 0 : ClientPlayer.getLocalPlayer().blinkDelta;
                r.vignetteShader.setUniformf("u_damageIntensity", blinkDelta != 0 ? Interpolation.smooth2.apply(blinkDelta) : blinkDelta);
            }

            var ote = clientEntityManager.getEntitiesByTypeSorted(ClientEntityType.HEALTH_BAR,
                    ClientEntityType.DAMAGE_INDICATOR,
                    ClientEntityType.SIGN,
                    ClientEntityType.SELECTOR,
                    ClientEntityType.PLAYER,
                    ClientEntityType.PICKUP_LINE
                    );

            if(displayBlur) {
                blurPass(r.mainFbo);
                drawFboTexture(r.blurTargetBFbo, r.vignetteShader);
            } else {
                drawFboTexture(r.mainFbo, r.vignetteShader);
            }

            // Draw light engine.
            if(!isFullDay()) {
                r.lightEngine.setLighting(ambientLightingR, ambientLightingG, ambientLightingB, ambientLightingDarkness);
                r.lightEngine.render();
            }

            if(!ote.isEmpty()) {
                r.entityFbo.begin();
                transparentScreen();
                clientEntityManager.renderTopEntities(ote);
                r.entityFbo.end();

                ote.clear();

                r.batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);

                if(displayBlur) {
                    blurPass(r.entityFbo);
                    drawFboTexture(r.blurTargetBFbo, r.vignetteShader);
                } else {
                    drawFboTexture(r.entityFbo, r.vignetteShader);
                }

                r.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            }
        }

        {
            // Draw debug info.
            if(r.drawTileInfo) {
                r.chunkRenderer.begin(ShapeRenderer.ShapeType.Line);
                r.chunkRenderer.setColor(Color.WHITE);
                r.chunkRenderer.rect(r.mouseWorldGridX, r.mouseWorldGridY, TILE_SIZE * 0.5f, TILE_SIZE * 0.5f);
                r.chunkRenderer.rect(r.mouseWorldGridX, r.mouseWorldGridY, TILE_SIZE, TILE_SIZE * 0.5f);
                r.chunkRenderer.rect(r.mouseWorldGridX, r.mouseWorldGridY, TILE_SIZE * 0.5f, TILE_SIZE);
                r.chunkRenderer.rect(r.mouseWorldGridX, r.mouseWorldGridY, TILE_SIZE, TILE_SIZE);
                r.chunkRenderer.end();
            }

            // Render debug shapes
            if(Expo.get().getImGuiExpo() != null) {
                r.chunkRenderer.begin(ShapeRenderer.ShapeType.Filled);

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

                        r.chunkRenderer.setColor(Color.WHITE);
                    }

                    r.chunkRenderer.end();
                    r.chunkRenderer.begin(ShapeRenderer.ShapeType.Line);
                    r.chunkRenderer.setColor(Color.YELLOW);
                    var world = ExpoClientContainer.get().getClientWorld().getClientPhysicsWorld();

                    try {
                        for(var o : world.getRects()) {
                            r.chunkRenderer.rect(o.x, o.y, o.w, o.h);
                        }
                    } catch (ConcurrentModificationException e) {
                        // ignore it because it happens due to different tick rates between client->localserver
                        // and JBump doesn't offer any ability to get the physic bodies thread-safe
                    }

                    r.chunkRenderer.setColor(Color.WHITE);
                }

                if(Expo.get().getImGuiExpo().renderChunkBorders.get()) {
                    r.chunkRenderer.end();
                    r.chunkRenderer.begin(ShapeRenderer.ShapeType.Line);
                    r.chunkRenderer.setColor(Color.WHITE);

                    for(var c : drawChunks) {
                        r.chunkRenderer.rect(c.chunkDrawBeginX, c.chunkDrawBeginY, CHUNK_SIZE, CHUNK_SIZE);
                    }
                }

                try {
                    if(Expo.get().getImGuiExpo().renderHitbox.get()) {
                        if(ServerWorld.get() != null) {
                            for(ServerEntity all : ServerWorld.get().getMainDimension().getEntityManager().getAllEntities()) {
                                if(all instanceof DamageableEntity de) {
                                    if(de.getEntityHitbox() == null) continue;
                                    EntityHitbox hitbox = de.getEntityHitbox();
                                    float[] verts = hitbox.toWorld(all.posX, all.posY);

                                    r.chunkRenderer.end();
                                    r.chunkRenderer.begin(ShapeRenderer.ShapeType.Line);
                                    r.chunkRenderer.setColor(Color.GREEN);

                                    r.chunkRenderer.rect(verts[0], verts[1], hitbox.width, hitbox.height);
                                }
                            }
                        }
                    }
                } catch (ConcurrentModificationException ignored) { }

                if(Expo.get().getImGuiExpo().renderDebugPoints.get()) {
                    for(DebugPoint dp : Expo.get().getImGuiExpo().drawPoints) {
                        r.chunkRenderer.setColor(dp.color);
                        r.chunkRenderer.circle(dp.x, dp.y, 0.5f, 8);
                    }

                    r.chunkRenderer.end();
                    r.hudBatch.begin();
                    for(DebugPoint dp : Expo.get().getImGuiExpo().drawPoints) {
                        if(dp.marker == null) continue;
                        Vector2 pos = ClientUtils.entityPosToHudPos(dp.x, dp.y);
                        r.m5x7_border_all[0].setColor(dp.color);
                        r.m5x7_border_all[0].draw(r.hudBatch, dp.marker, pos.x, pos.y);
                        r.m5x7_border_all[0].setColor(Color.WHITE);
                    }
                    r.hudBatch.end();
                    r.chunkRenderer.begin(ShapeRenderer.ShapeType.Filled);
                }

                Expo.get().getImGuiExpo().drawPoints.clear();

                for(ClientEntity all : clientEntityManager.allEntities()) {
                    if(all.visibleToRenderEngine) {
                        float dpx = all.finalDrawPosX;
                        float dpy = all.finalDrawPosY;

                        float tpx = all.finalTextureStartX;
                        float tpy = all.finalTextureStartY;


                        if(all instanceof ClientPlayer cp && Expo.get().getImGuiExpo().renderPunchData.get()) {
                            r.chunkRenderer.setColor(Color.RED);

                            if(cp.holdingItemSprites != null) {
                                for(Sprite sp : cp.holdingItemSprites) {
                                    r.chunkRenderer.circle(sp.getX(), sp.getY(), 0.25f, 8);
                                }
                            }

                            /*
                            for(int i = 0; i < cp.vl.length; i += 2) {
                                r.chunkRenderer.setColor(1f - i * 0.1f, 1f - i * 0.1f, 0f, 1.0f);
                                r.chunkRenderer.circle(cp.vl[i], cp.vl[i + 1], 0.4f, 16);
                            }
                            */
                        }

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

                            if(all instanceof ClientDynamic3DTile cd3d) {
                                r.chunkRenderer.setColor(Color.RED);
                                r.chunkRenderer.end();

                                r.chunkRenderer.begin(ShapeRenderer.ShapeType.Line);
                                r.chunkRenderer.rect(
                                        cd3d.finalDrawPosX - cd3d.squishAnimator2D.squishX1,
                                        cd3d.finalDrawPosY + cd3d.squishAnimator2D.squishY1,
                                        cd3d.created.getRegionWidth() + cd3d.squishAnimator2D.squishX2,
                                        cd3d.created.getRegionHeight() - 32 + cd3d.squishAnimator2D.squishY2);

                                r.chunkRenderer.end();
                                r.chunkRenderer.begin(ShapeRenderer.ShapeType.Filled);
                            }
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
                            if(all instanceof ClientDynamic3DTile tile) {
                                int[] cpy = new int[tile.layerIds.length];
                                for(int i = 0; i < tile.layerIds.length; i++) {
                                    cpy[i] = tile.layerIds[i] - tile.emulatingType.TILE_ID_DATA[0];
                                }
                                r.m5x7_border_all[0].draw(r.hudBatch, Arrays.toString(cpy), p.x, p.y + 12);
                                r.m5x7_border_all[0].draw(r.hudBatch, tile.created.toString(), p.x, p.y + 24);
                            }
                            r.hudBatch.end();

                            r.chunkRenderer.begin(ShapeRenderer.ShapeType.Line);
                        }

                        if(Expo.get().getImGuiExpo().renderEntityBbox.get()) {
                            if(ServerWorld.get() != null) {
                                var t = EntityMetadataMapper.get().getFor(all.getEntityType().ENTITY_SERVER_TYPE);


                                if(t != null) {
                                    var type = t.getPopulationBbox();

                                    if(type != null) {
                                        float[] pos = type.toWorld(all.serverPosX, all.serverPosY);
                                        r.chunkRenderer.end();
                                        r.chunkRenderer.begin(ShapeRenderer.ShapeType.Line);
                                        r.chunkRenderer.setColor(Color.PURPLE);
                                        r.chunkRenderer.rect(pos[0], pos[1], pos[2] - pos[0], pos[3] - pos[1]);
                                    }
                                }
                            }
                        }
                    }
                }

                r.chunkRenderer.end();
            }
        }

        { // Draw HUD
            r.batch.setColor(Color.WHITE);
            r.batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
            if(r.drawHUD) {
                drawFboTexture(r.hudFbo, null);
            }
            r.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA); //default blend mode
        }
    }

    public void updateChunksToDraw() {
        ClientPlayer player = ClientPlayer.getLocalPlayer();
        if(player == null) return;

        int[] viewport = player.clientViewport;
        boolean sameViewport = sameViewport(viewport);
        int c = 0;

        for(ClientChunk existing : drawChunks) {
            if(existing == null) continue;
            existing.updateVisibility(false, true);
        }

        for(int i = 0; i < PLAYER_CHUNK_VIEW_RANGE_X; i++) {
            for(int j = 0; j < PLAYER_CHUNK_VIEW_RANGE_Y; j++) {
                int cx = viewport[0] + i; // CHUNK X
                int cy = viewport[2] + j; // CHUNK Y

                ClientChunk chunk = clientChunkGrid.getChunk(cx, cy);

                if(chunk != null) {
                    drawChunks[c] = chunk;
                    chunk.updateVisibility(true, false);
                    c++;
                }
            }
        }

        if(!sameViewport && (cachedViewport[0] != Integer.MAX_VALUE)) {
            int xDir = viewport[0] - cachedViewport[0];

            if(xDir > 0) {
                // moved right
                int cx = viewport[1];
                int cy = viewport[2];

                for(int i = 0; i < PLAYER_CHUNK_VIEW_RANGE_Y; i++) {
                    int _cy = cy + i;
                    int[] indexes = new int[ROW_TILES];

                    for(int ta = 0; ta < ROW_TILES; ta++) {
                        indexes[ta] = ROW_TILES - 1 + ta * ROW_TILES;
                    }

                    addQueuedAO(indexes, cx, _cy, cx + 1, _cy, cx - 1, _cy, cx, _cy);
                }
            } else if(xDir < 0) {
                // moved left
                int cx = viewport[0];
                int cy = viewport[2];

                for(int i = 0; i < PLAYER_CHUNK_VIEW_RANGE_Y; i++) {
                    int _cy = cy + i;
                    int[] indexes = new int[ROW_TILES];

                    for(int ta = 0; ta < ROW_TILES; ta++) {
                        indexes[ta] = ta * ROW_TILES;
                    }

                    addQueuedAO(indexes, cx, _cy, cx - 1, _cy, cx + 1, _cy, cx, _cy);
                }
            }

            int yDir = viewport[2] - cachedViewport[2];

            if(yDir > 0) {
                // moved up
                int cx = viewport[0];
                int cy = viewport[3];

                for(int i = 0; i < PLAYER_CHUNK_VIEW_RANGE_X; i++) {
                    int _cx = cx + i;
                    int[] indexes = new int[ROW_TILES];

                    for(int ta = 0; ta < ROW_TILES; ta++) {
                        indexes[ta] = ta + ((ROW_TILES - 1) * ROW_TILES);
                    }

                    addQueuedAO(indexes, _cx, cy, _cx, cy + 1, _cx, cy - 1, _cx, cy);
                }
            } else if(yDir < 0) {
                // moved down
                int cx = viewport[0];
                int cy = viewport[2];

                for(int i = 0; i < PLAYER_CHUNK_VIEW_RANGE_X; i++) {
                    int _cx = cx + i;
                    int[] indexes = new int[ROW_TILES];

                    for(int ta = 0; ta < ROW_TILES; ta++) {
                        indexes[ta] = ta;
                    }

                    addQueuedAO(indexes, _cx, cy, _cx, cy - 1, _cx, cy + 1, _cx, cy);
                }
            }
        }

        ListIterator<QueuedAmbientOcclusion> iterator = ambientOcclusionQueue.listIterator();

        while(iterator.hasNext()) {
            QueuedAmbientOcclusion next = iterator.next();

            // Out of bounds check
            boolean inBoundsRequire = next.requiresChunkX >= viewport[0] && next.requiresChunkX <= viewport[1] && next.requiresChunkY >= viewport[2] && next.requiresChunkY <= viewport[3];
            boolean inBoundsUpdate = next.updateChunkX >= viewport[0] && next.updateChunkX <= viewport[1] && next.updateChunkY >= viewport[2] && next.updateChunkY <= viewport[3];

            if(!inBoundsRequire && !inBoundsUpdate) {
                iterator.remove();
                continue;
            }

            ClientChunk requireChunk = clientChunkGrid.getChunk(next.requiresChunkX, next.requiresChunkY);

            if(requireChunk != null && (requireChunk.ranAmbientOcclusion || (requireChunk.getInitializationTileCount() == 0 && !requireChunk.ranAmbientOcclusion))) {
                ClientChunk wantToUpdateChunk = clientChunkGrid.getChunk(next.updateChunkX, next.updateChunkY);

                if(wantToUpdateChunk != null) {
                    iterator.remove();

                    for(int index : next.updateTileArray) {
                        wantToUpdateChunk.updateAmbientOcclusion(index, false, true);
                    }
                }
            }
        }

        cachedViewport[0] = viewport[0];
        cachedViewport[1] = viewport[1];
        cachedViewport[2] = viewport[2];
        cachedViewport[3] = viewport[3];

        /*
        if(!sameViewport && (cachedViewport[0] != Integer.MAX_VALUE)) {
            for(ClientChunk current : clientChunkGrid.clientChunkMap.values()) {
                int middleX = viewport[0] + PLAYER_CHUNK_VIEW_RANGE_DIR_X;
                int middleY = viewport[2] + PLAYER_CHUNK_VIEW_RANGE_DIR_Y;

                int dstX = Math.abs(middleX - current.chunkX);
                int dstY = Math.abs(middleY - current.chunkY);

                boolean outOfRangeX = dstX >= PLAYER_CHUNK_VIEW_RANGE_X;
                boolean outOfRangeY = dstY >= PLAYER_CHUNK_VIEW_RANGE_Y;

                boolean empty = current.tileEntityCount == 0;
                boolean visibleLogic = (current.chunkX >= viewport[0] && current.chunkX <= viewport[1] && current.chunkY >= viewport[2] && current.chunkY <= viewport[3]);

                if(!visibleLogic && empty && (outOfRangeX || outOfRangeY)) {
                    String key = current.chunkX + "," + current.chunkY;
                    clientChunkGrid.clientChunkMap.remove(key);
                }
            }
        }

        ClientUtils.log("Size: " + clientChunkGrid.clientChunkMap.size() + "/" + (ServerPlayer.getLocalPlayer().hasSeenChunks.size()), Input.Keys.X);
        */
    }

    private void addQueuedAO(int[] indexes, int x0, int y0, int x1, int y1, int x2, int y2, int x3, int y3) {
        QueuedAmbientOcclusion qao = new QueuedAmbientOcclusion();
        qao.updateChunkX = x0;
        qao.updateChunkY = y0;
        qao.requiresChunkX = x1;
        qao.requiresChunkY = y1;
        qao.updateTileArray = indexes;
        ambientOcclusionQueue.add(qao);

        QueuedAmbientOcclusion qao2 = new QueuedAmbientOcclusion();
        qao2.updateChunkX = x2;
        qao2.updateChunkY = y2;
        qao2.requiresChunkX = x3;
        qao2.requiresChunkY = y3;
        qao2.updateTileArray = indexes;
        ambientOcclusionQueue.add(qao2);
    }

    private boolean sameViewport(int[] newViewport) {
        for(int i = 0; i < newViewport.length; i++) {
            if(cachedViewport[i] != newViewport[i]) return false;
        }
        return true;
    }

    private void blurPass(FrameBuffer drawBuffer) {
        RenderContext r = RenderContext.get();

        r.blurShader.bind();
        r.blurShader.setUniformf("u_radius", r.blurStrength);
        r.blurShader.setUniformf("u_dir", 1.0f, 0.0f);
        r.blurShader.setUniformf("u_resolution", Gdx.graphics.getWidth());

        {
            r.blurTargetAFbo.begin();
            transparentScreen();
            drawFboTexture(drawBuffer, r.blurShader); // r.mainFbo
            r.blurTargetAFbo.end();
        }

        r.blurShader.bind();
        r.blurShader.setUniformf("u_dir", 0.0f, 1.0f);
        r.blurShader.setUniformf("u_resolution", Gdx.graphics.getHeight());

        {
            r.blurTargetBFbo.begin();
            transparentScreen();
            drawFboTexture(r.blurTargetAFbo, r.blurShader);
            r.blurTargetBFbo.end();
        }
    }

    public void transparentScreen() {
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

    private void drawWaterRemake() {
        RenderContext r = RenderContext.get();

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
        float uvAdjusted = 16.0f / textureSize;

        for(ClientChunk chunk : drawChunks) {
            if(chunk != null && chunk.visibleRender && chunk.chunkContainsWater) {
                for(int i = 0; i < chunk.dynamicTiles.length; i++) {
                    ClientDynamicTilePart[] tiles = chunk.dynamicTiles[i];
                    ClientDynamicTilePart l2 = tiles[2];
                    boolean waterTile = TileLayerType.isWater(l2.emulatingType);
                    if(!waterTile) continue;

                    int tx = i % ROW_TILES;
                    int ty = i / ROW_TILES;
                    float wx = chunk.chunkDrawBeginX + ExpoShared.tileToPos(tx);
                    float wy = chunk.chunkDrawBeginY + ExpoShared.tileToPos(ty);

                    float normX = (wx % textureSize) / textureSize;
                    float normY = (wy % textureSize) / textureSize;

                    r.batch.draw(r.waterNoiseTexture, wx, wy, TILE_SIZE, TILE_SIZE, normX, normY, normX + uvAdjusted, normY + uvAdjusted);
                }
            }
        }

        r.batch.end();
        r.batch.setShader(r.DEFAULT_GLES3_SHADER);
        r.batch.setColor(Color.WHITE);
    }

    private void drawWater() {
        RenderContext r = RenderContext.get();

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

        int HALF_TILE_SIZE = TILE_SIZE / 2;
        float uvAdjusted = 16.0f / textureSize;
        float uvAdjustedHalf = uvAdjusted * 0.5f;
        float halfTileInverse = 2.0f / 16.0f;

        for(ClientChunk chunk : drawChunks) {
            if(chunk != null && chunk.visibleRender) {
                for(int k = 0; k < chunk.biomes.length; k++) {
                    boolean isWaterTile = TileLayerType.isWater(chunk.dynamicTiles[k][2].emulatingType);

                    if(isWaterTile) {
                        int tx = k % ROW_TILES;
                        int ty = k / ROW_TILES;
                        float wx = chunk.chunkDrawBeginX + ExpoShared.tileToPos(tx);
                        float wy = chunk.chunkDrawBeginY + ExpoShared.tileToPos(ty);

                        float normX = (wx % textureSize) / textureSize;
                        float normY = (wy % textureSize) / textureSize;

                        if(chunk.waterDisplacement[k] != null && chunk.dynamicTiles[k][2].texture.length > 1) {
                            float interpolated = clientChunkGrid.interpolation;

                            var values = chunk.waterDisplacement[k];

                            {
                                float x0 = interpolated * (int) values[0].key;
                                float y0 = interpolated * (int) values[0].value;
                                float _normX = ((wx + x0) % textureSize) / textureSize;
                                float _normY = ((wy + y0) % textureSize) / textureSize;

                                r.batch.draw(r.waterNoiseTexture, wx + x0, wy + y0,
                                        HALF_TILE_SIZE - x0, HALF_TILE_SIZE - y0,
                                        _normX, _normY,
                                        _normX + uvAdjustedHalf - x0 * uvAdjustedHalf * halfTileInverse,
                                        _normY + uvAdjustedHalf - y0 * uvAdjustedHalf * halfTileInverse);
                            }


                            {
                                float x1 = interpolated * (int) values[1].key;
                                float y1 = interpolated * (int) values[1].value;

                                float _normX = ((wx + 8) % textureSize) / textureSize;

                                r.batch.draw(r.waterNoiseTexture, wx + 8, wy + y1,
                                        HALF_TILE_SIZE + x1, HALF_TILE_SIZE - y1,
                                        _normX,
                                        normY + y1 * uvAdjustedHalf * halfTileInverse,
                                        _normX + uvAdjustedHalf + x1 * uvAdjustedHalf * halfTileInverse,
                                        normY + uvAdjustedHalf);
                            }

                            {
                                float x2 = interpolated * (int) values[2].key;
                                float y2 = interpolated * (int) values[2].value;

                                float _normX = ((wx + x2) % textureSize) / textureSize;
                                float _normY = ((wy + 8) % textureSize) / textureSize;

                                r.batch.draw(r.waterNoiseTexture, wx + x2, wy + 8,
                                        HALF_TILE_SIZE - x2, HALF_TILE_SIZE + y2,
                                        _normX, _normY,
                                        _normX + uvAdjustedHalf - x2 * uvAdjustedHalf * halfTileInverse,
                                        _normY + uvAdjustedHalf + y2 * uvAdjustedHalf * halfTileInverse);
                            }

                            {
                                float x3 = interpolated * (int) values[3].key;
                                float y3 = interpolated * (int) values[3].value;

                                float _normX = ((wx + 8) % textureSize) / textureSize;
                                float _normY = ((wy + 8) % textureSize) / textureSize;

                                r.batch.draw(r.waterNoiseTexture, wx + 8, wy + 8,
                                        HALF_TILE_SIZE + x3, HALF_TILE_SIZE + y3,
                                        _normX, _normY,
                                        _normX + uvAdjustedHalf + x3 * uvAdjustedHalf * halfTileInverse,
                                        _normY + uvAdjustedHalf + y3 * uvAdjustedHalf * halfTileInverse);
                            }
                        } else {
                            r.batch.draw(r.waterNoiseTexture, wx, wy, TILE_SIZE, TILE_SIZE, normX, normY, normX + uvAdjusted, normY + uvAdjusted);
                        }
                    }
                }
            }
        }

        r.batch.end();
        r.batch.setShader(r.DEFAULT_GLES3_SHADER);
        r.batch.setColor(Color.WHITE);
    }

    private void renderEntityReflections() {
        if(ClientPlayer.getLocalPlayer() == null) return;
        RenderContext rc = RenderContext.get();

        // Draw reflections
        rc.arraySpriteBatch.begin();
        rc.arraySpriteBatch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);

        for(ClientEntity entity : clientEntityManager.getDepthEntityList()) {
            // TODO: Possible optimization, check for visibleForRender && chunk.visible before drawing
            if(!entity.drawReflection) continue;
            if(!(entity instanceof ReflectableEntity reflectableEntity)) continue;
            reflectableEntity.renderReflection(rc, rc.delta);
        }

        for(ClientEntity puddles : clientEntityManager.getEntitiesByType(ClientEntityType.PUDDLE)) {
            ((ClientPuddle) puddles).renderReflection(rc, rc.delta);
        }

        rc.arraySpriteBatch.setColor(Color.WHITE);
        rc.arraySpriteBatch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        rc.arraySpriteBatch.end();
        rc.batch.begin();
    }

    private void renderWaterTiles() {
        if(ClientPlayer.getLocalPlayer() == null) return;
        RenderContext rc = RenderContext.get();

        rc.batch.setShader(rc.DEFAULT_GLES3_SHADER);
        rc.batch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);

        for(ClientChunk chunk : drawChunks) {
            if(chunk == null || !chunk.visibleRender) continue;

            for(int i = 0; i < chunk.dynamicTiles.length; i++) {
                ClientDynamicTilePart[] tiles = chunk.dynamicTiles[i];
                ClientDynamicTilePart l2 = tiles[2];
                boolean waterTile = TileLayerType.isWater(l2.emulatingType);
                if(!waterTile) continue;

                ClientDynamicTilePart l0 = tiles[0];
                ClientDynamicTilePart l1 = tiles[1];

                if(!l1.isFullTile()) {
                    l0.drawSimple();
                }

                if(!l2.isFullTile() ||
                        l1.emulatingType == TileLayerType.SAND_WATERLOGGED ||
                        l1.emulatingType == TileLayerType.SOIL_DEEP_WATERLOGGED ||
                        l1.emulatingType == TileLayerType.SOIL_WATERLOGGED) {
                    l1.drawSimple();
                }

                // We skip layer2, because water overlay tiles are rendered in the regular tile fbo
            }
        }

        rc.batch.end();
        drawWaterRemake();
    }

    private void renderChunkTiles(boolean waterOverlayOnly) {
        if(ClientPlayer.getLocalPlayer() != null) {
            RenderContext rc = RenderContext.get();

            rc.polygonTileBatch.begin();
            rc.polygonTileBatch.setShader(rc.grassShader);
            rc.polygonTileBatch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);

            for(ClientChunk chunk : drawChunks) {
                if(chunk != null && chunk.visibleRender) {
                    if(waterOverlayOnly) {
                        for(int i = 0; i < chunk.dynamicTiles.length; i++) {
                            ClientDynamicTilePart l2 = chunk.dynamicTiles[i][2];

                            if(TileLayerType.isWater(l2.emulatingType) && !l2.isFullTile()) {
                                l2.draw(rc, chunk.ambientOcclusion[i]);
                            }
                        }
                    } else {
                        for(int i = 0; i < chunk.dynamicTiles.length; i++) {
                            ClientDynamicTilePart[] tiles = chunk.dynamicTiles[i];
                            ClientDynamicTilePart l2 = tiles[2];
                            boolean waterTile = TileLayerType.isWater(l2.emulatingType);

                            if(waterTile) {
                                // Skip because water tiles are drawn to a separate buffer
                                continue;
                            }

                            ClientDynamicTilePart l0 = tiles[0];
                            ClientDynamicTilePart l1 = tiles[1];

                            if(!l1.isFullTile() && !l2.isFullTile()) {
                                l0.draw(rc, chunk.ambientOcclusion[i]);
                            }

                            if(!l2.isFullTile()) {
                                l1.draw(rc, chunk.ambientOcclusion[i]);
                            }

                            l2.draw(rc, chunk.ambientOcclusion[i]);
                        }
                    }
                }
            }

            rc.polygonTileBatch.end();
        }
    }

    public ClientChunkGrid getClientChunkGrid() {
        return clientChunkGrid;
    }

    public World<ClientEntity> getClientPhysicsWorld() {
        return clientPhysicsWorld;
    }

}