package dev.michey.expo.render.imgui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import dev.michey.expo.audio.AudioEngine;
import dev.michey.expo.audio.TrackedSoundData;
import dev.michey.expo.localserver.ExpoServerLocal;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.logic.entity.arch.ClientEntityManager;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.logic.inventory.PlayerInventory;
import dev.michey.expo.logic.world.ClientWorld;
import dev.michey.expo.logic.world.chunk.ClientChunk;
import dev.michey.expo.logic.world.chunk.ClientChunkGrid;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.light.ExpoLightEngine;
import dev.michey.expo.render.ui.InteractableUIElement;
import dev.michey.expo.render.ui.PlayerUI;
import dev.michey.expo.render.ui.container.UIContainerInventory;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.logic.ExpoServerContainer;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.chunk.ServerChunk;
import dev.michey.expo.server.main.logic.world.dimension.ServerDimension;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.util.ExpoTime;
import dev.michey.expo.util.GameSettings;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.type.ImBoolean;

import java.util.Arrays;

public class ImGuiExpo {

    private final int[] worldTimeSlider = new int[1];
    private final float[] worldDelta = new float[1];
    private final float[] nightColor = new float[3];
    private final float[] sunriseColor = new float[3];
    private final float[] sunsetColor = new float[3];
    private final float[] currentColor = new float[3];
    private final ImBoolean checkPrecisePositionData = new ImBoolean(false);
    private final ImBoolean checkSlotIndices = new ImBoolean(false);
    // ===== WATER =====
    private final float[] waterReflectionSpeed = new float[1];
    private final float[] waterSkewX = new float[1];
    private final float[] waterSkewY = new float[1];
    private final float[] waterSpeed = new float[1];
    private final float[] waterAlpha = new float[1];
    private final float[] contrast = new float[1];
    private final float[] brightness = new float[1];
    private final float[] waterColor = new float[4];
    private final int[] uiScale = new int[3];
    private final float[] playerHealth = new float[1];
    private final float[] playerHunger = new float[1];
    private final float[] blurStrength = new float[1];
    public final ImBoolean renderInteractionPoints = new ImBoolean(false);
    public final ImBoolean renderClientPos = new ImBoolean(false);
    public final ImBoolean renderServerPos = new ImBoolean(false);
    public final ImBoolean renderVisualCenter = new ImBoolean(false);
    public final ImBoolean renderDrawRoot = new ImBoolean(false);
    public final ImBoolean renderDrawPos = new ImBoolean(false);
    public final ImBoolean renderJBump = new ImBoolean(false);
    public final ImBoolean renderChunkBorders = new ImBoolean(false);
    public final ImBoolean renderEntityId = new ImBoolean(false);
    public final ImBoolean renderEntityBbox = new ImBoolean(false);
    public final ImBoolean renderPunchData = new ImBoolean(false);
    public final ImBoolean renderHitbox = new ImBoolean(false);
    public final ImBoolean entityBrainStates = new ImBoolean(false);
    private final float[] speed = new float[1];
    private final float[] minStrength = new float[1];
    private final float[] maxStrength = new float[1];
    private final float[] strengthScale = new float[1];
    private final float[] interval = new float[1];
    private final float[] detail = new float[1];
    private final float[] distortion = new float[1];
    private final float[] heightOffset = new float[1];
    private final float[] offset = new float[1];
    private final float[] skew = new float[1];
    private final float[] rainColor = new float[3];
    private final float[] constantLight = new float[1];
    private final float[] linearLight = new float[1];
    private final float[] quadraticLight = new float[1];
    private final float[] distanceLight = new float[1];
    private final float[] colorLight = new float[3];
    private final float[] gradientStartOffset = new float[1];
    private final float[] gradientMultiplier = new float[1];

    public void draw() {
        drawExpoWindow();
    }

    private void drawExpoWindow() {
        RenderContext r = RenderContext.get();

        // Master Expo window
        ImGui.begin("Expo");
        boolean clientPresent = ExpoClientContainer.get() != null;

        // General info
        ImGui.textColored(0.0f, 1.0f, 1.0f, 1.0f, "FPS: " + Gdx.graphics.getFramesPerSecond());
        ImGui.separator();

        if(clientPresent) {
            ClientWorld w = ExpoClientContainer.get().getClientWorld();
            worldTimeSlider[0] = (int) w.worldTime;
            worldDelta[0] = r.deltaMultiplier;

            nightColor[0] = w.COLOR_AMBIENT_MIDNIGHT.r;
            nightColor[1] = w.COLOR_AMBIENT_MIDNIGHT.g;
            nightColor[2] = w.COLOR_AMBIENT_MIDNIGHT.b;

            sunsetColor[0] = w.COLOR_AMBIENT_SUNSET.r;
            sunsetColor[1] = w.COLOR_AMBIENT_SUNSET.g;
            sunsetColor[2] = w.COLOR_AMBIENT_SUNSET.b;

            sunriseColor[0] = w.COLOR_AMBIENT_SUNRISE.r;
            sunriseColor[1] = w.COLOR_AMBIENT_SUNRISE.g;
            sunriseColor[2] = w.COLOR_AMBIENT_SUNRISE.b;

            waterReflectionSpeed[0] = r.waterReflectionSpeed;
            waterSkewX[0] = r.waterSkewX;
            waterSkewY[0] = r.waterSkewY;

            waterColor[0] = r.waterColor[0];
            waterColor[1] = r.waterColor[1];
            waterColor[2] = r.waterColor[2];
            waterAlpha[0] = r.waterAlpha;

            waterSpeed[0] = r.waterSpeed;
            brightness[0] = r.brightness;
            contrast[0] = r.contrast;

            currentColor[0] = w.ambientLightingR;
            currentColor[1] = w.ambientLightingG;
            currentColor[2] = w.ambientLightingB;

            speed[0] = r.speed;
            minStrength[0] = r.minStrength;
            maxStrength[0] = r.maxStrength;
            strengthScale[0] = r.strengthScale;
            interval[0] = r.interval;
            detail[0] = r.detail;
            distortion[0] = r.distortion;
            heightOffset[0] = r.heightOffset;
            offset[0] = r.offset;
            skew[0] = r.skew;

            rainColor[0] = w.COLOR_RAIN.r;
            rainColor[1] = w.COLOR_RAIN.g;
            rainColor[2] = w.COLOR_RAIN.b;

            gradientStartOffset[0] = RenderContext.get().gradientStartOffset;
            gradientMultiplier[0] = RenderContext.get().gradientMultiplier;

            constantLight[0] = ExpoLightEngine.CONSTANT_LIGHT_VALUE;
            linearLight[0] = ExpoLightEngine.LINEAR_LIGHT_VALUE;
            quadraticLight[0] = ExpoLightEngine.QUADRATIC_LIGHT_VALUE;
            distanceLight[0] = ExpoLightEngine.DISTANCE_LIGHT_VALUE;
            colorLight[0] = ExpoLightEngine.COLOR_LIGHT_VALUE.r;
            colorLight[1] = ExpoLightEngine.COLOR_LIGHT_VALUE.g;
            colorLight[2] = ExpoLightEngine.COLOR_LIGHT_VALUE.b;

            ClientPlayer player = ClientPlayer.getLocalPlayer();

            if(player != null) {
                playerHealth[0] = player.playerHealth;
                playerHunger[0] = player.playerHunger;
            }

            blurStrength[0] = r.blurStrength;

            uiScale[0] = (int) ExpoClientContainer.get().getPlayerUI().uiScale;

            // Client World
            ImGui.pushItemWidth(190f);

            if(ImGui.sliderFloat("worldDelta", worldDelta, 0.1f, 10.0f)) {
                r.deltaMultiplier = worldDelta[0];
            }

            if(ImGui.sliderInt("worldTime", worldTimeSlider, 0, 1440)) {
                setTime(worldTimeSlider[0]);
            }

            ImGui.sameLine();
            ImGui.text(" (" + ExpoTime.worldTimeString(worldTimeSlider[0]) + ")");

            if(ImGui.button("SUNRISE")) setTime(ExpoTime.SUNRISE); ImGui.sameLine();
            if(ImGui.button("DAY")) setTime(ExpoTime.DAY); ImGui.sameLine();
            if(ImGui.button("SUNSET")) setTime(ExpoTime.SUNSET); ImGui.sameLine();
            if(ImGui.button("NIGHT")) setTime(ExpoTime.NIGHT); ImGui.sameLine();
            if(ImGui.button("MIDNIGHT")) setTime(ExpoTime.MIDNIGHT);

            ImGui.separator();

            if(ImGui.treeNode("Transition colors")) {
                if(ImGui.treeNode("Night color")) {
                    if(ImGui.colorPicker3("nightColor", nightColor)) setNightColor(nightColor);
                    ImGui.treePop();
                }

                if(ImGui.treeNode("Sunrise color")) {
                    if(ImGui.colorPicker3("sunriseColor", sunriseColor)) setSunriseColor(sunriseColor);
                    ImGui.treePop();
                }

                if(ImGui.treeNode("Sunset color")) {
                    if(ImGui.colorPicker3("sunsetColor", sunsetColor)) setSunsetColor(sunsetColor);
                    ImGui.treePop();
                }

                ImGui.colorEdit3("Current color", currentColor);

                if(ImGui.treeNode("Rain color")) {
                    if(ImGui.colorPicker3("rainColor", rainColor)) setRainColor(rainColor);
                    ImGui.treePop();
                }

                ImGui.treePop();
            }

            ImGui.separator();

            if(ImGui.treeNode("Render data")) {
                coloredBulletText(0.0f, 1.0f, 0.5f, "batch: " + r.batch.totalRenderCalls);
                coloredBulletText(0.0f, 1.0f, 0.5f, "arraySpriteBatch: " + r.arraySpriteBatch.totalRenderCalls);
                coloredBulletText(0.0f, 1.0f, 0.5f, "hudBatch: " + r.hudBatch.totalRenderCalls);
                coloredBulletText(0.0f, 1.0f, 0.5f, "aoBatch: " + r.aoBatch.totalRenderCalls);
                coloredBulletText(0.0f, 1.0f, 0.5f, "polygonTileBatch: " + r.polygonTileBatch.totalRenderCalls);
                ImGui.separator();

                if(ImGui.treeNode(500, "Light engine")) {
                    if(ImGui.sliderFloat("gradientStartOffset", gradientStartOffset, 0.0f, 1.0f)) {
                        r.gradientStartOffset = gradientStartOffset[0];
                    }

                    if(ImGui.sliderFloat("gradientMultiplier", gradientMultiplier, 0.0f, 1.0f)) {
                        r.gradientMultiplier = gradientMultiplier[0];
                    }

                    if(ImGui.sliderFloat("Linear", linearLight, 0.0f, 2.0f)) {
                        ExpoLightEngine.LINEAR_LIGHT_VALUE = linearLight[0];
                    }

                    if(ImGui.sliderFloat("Constant", constantLight, 0.0f, 2.0f)) {
                        ExpoLightEngine.CONSTANT_LIGHT_VALUE = constantLight[0];
                    }

                    if(ImGui.sliderFloat("Quadratic", quadraticLight, 0.0f, 2.0f)) {
                        ExpoLightEngine.QUADRATIC_LIGHT_VALUE = quadraticLight[0];
                    }

                    if(ImGui.sliderFloat("Distance", distanceLight, 16.0f, 512.0f)) {
                        ExpoLightEngine.DISTANCE_LIGHT_VALUE = distanceLight[0];
                    }

                    if(ImGui.colorPicker3("Color", colorLight)) {
                        ExpoLightEngine.COLOR_LIGHT_VALUE.set(colorLight[0], colorLight[1], colorLight[2], 1.0f);
                    }

                    ImGui.treePop();
                }

                ImGui.treePop();
            }

            ImGui.separator();

            if(ImGui.treeNode("Position data")) {
                ImGui.checkbox("Precise", checkPrecisePositionData);
                ImGui.separator();

                if(player != null) {
                    positionColored(1.0f, 1.0f, 0.0f, "Player Pos", player.clientPosX, player.clientPosY);
                }

                OrthographicCamera cam = r.expoCamera.camera;
                positionColored(1.0f, 165f/255f, 0.0f, "Camera Pos", cam.position.x, cam.position.y);
                ImGui.sameLine();
                ImGui.textColored(1.0f, 165f/255f, 0.0f, 1.0f, "@ " + cam.zoom);
                positionColoredR(1.0f, 127f/255f, 80f/255f, "Draw viewport range x", r.drawStartX, r.drawEndX);
                positionColoredR(1.0f, 127f/255f, 80f/255f, "Draw viewport range y", r.drawStartY, r.drawEndY);

                ImGui.separator();
                coloredBulletText(0.0f, 1.0f, 1.0f, "Mouse Rot: " + (checkPrecisePositionData.get() ? r.mouseRotation : (int) r.mouseRotation));
                coloredBulletText(0.0f, 1.0f, 1.0f, "Mouse Dir: " + r.mouseDirection);
                positionColored(0.0f, 1.0f, 1.0f, "Mouse (Screen) Pos", r.mouseX, r.mouseY);
                positionColored(0.0f, 1.0f, 0.0f, "Mouse (World) Pos", r.mouseWorldX, r.mouseWorldY);
                positionColored(0.0f, 1.0f, 0.0f, "Mouse (Chunk) Pos", r.mouseChunkX, r.mouseChunkY);
                positionColored(0.0f, 1.0f, 0.0f, "Mouse (Tile) Pos", r.mouseTileX, r.mouseTileY);

                positionColored(250f/255f, 128f/255f, 114f/255f, "Mouse Tile (World) Pos", r.mouseWorldGridX, r.mouseWorldGridY);
                positionColored(250f/255f, 128f/255f, 114f/255f, "Mouse Tile (Relative) Pos", r.mouseRelativeTileX, r.mouseRelativeTileY);
                coloredBulletText(250f/255f, 128f/255f, 114f/255f, "Mouse Tile (Array) Pos: " + r.mouseTileArray);

                // Biome conversion
                ClientChunk chunk = ClientChunkGrid.get().getChunk(r.mouseChunkX, r.mouseChunkY);

                if(chunk != null) {
                    BiomeType t = chunk.biomes[r.mouseTileArray];
                    int[] l0 = chunk.dynamicTiles[r.mouseTileArray][0].layerIds;
                    int[] l1 = chunk.dynamicTiles[r.mouseTileArray][1].layerIds;
                    int[] l2 = chunk.dynamicTiles[r.mouseTileArray][2].layerIds;
                    coloredBulletText(255f/255f,215f/255f,0f/255f, "Mouse Biome: " + t.name());
                    coloredBulletText(255f/255f,215f/255f,0f/255f, "Layer0: " + Arrays.toString(l0));
                    coloredBulletText(255f/255f,215f/255f,0f/255f, "Layer1: " + Arrays.toString(l1));
                    coloredBulletText(255f/255f,215f/255f,0f/255f, "Layer2: " + Arrays.toString(l2));
                    coloredBulletText(255f/255f,215f/255f,0f/255f, "Layer0Tex: " + Arrays.toString(chunk.dynamicTiles[r.mouseTileArray][0].texture));
                    coloredBulletText(255f/255f,215f/255f,0f/255f, "Layer1Tex: " + Arrays.toString(chunk.dynamicTiles[r.mouseTileArray][1].texture));
                    coloredBulletText(255f/255f,215f/255f,0f/255f, "Layer2Tex: " + Arrays.toString(chunk.dynamicTiles[r.mouseTileArray][2].texture));

                    coloredBulletText(1.0f, 1.0f, 1.0f, "ambientOcclusion " + Arrays.toString(chunk.ambientOcclusion[r.mouseTileArray]));
                    coloredBulletText(1.0f, 1.0f, 1.0f, "tileEntityId (CLIENT) " + (chunk.tileEntityGrid == null ? "EMPTY" : chunk.tileEntityGrid[r.mouseTileArray] + ""));

                    if(ServerWorld.get() != null) {
                        ServerChunk ch = ServerWorld.get().getDimension("overworld").getChunkHandler().getChunkSafe(chunk.chunkX, chunk.chunkY);

                        coloredBulletText(1.0f, 1.0f, 1.0f, "tileEntityId (SERVER) " + (ch.hasTileBasedEntities() ? ch.getTileBasedEntityIdGrid()[r.mouseTileArray] : "EMPTY"));
                    }
                }

                ImGui.treePop();
            }

            ImGui.separator();

            if(ImGui.treeNode("Client/Server data")) {
                ExpoServerLocal local = ExpoServerLocal.get() == null ? null : (ExpoServerLocal) ExpoServerBase.get();
                ImGui.indent();

                if(local != null) {
                    ImGui.textColored(32f/255f, 178f/255f, 170f/255f, 1.0f, "SERVER");

                    ImGui.indent();

                    ImGui.text("TPS: " + local.getTicksPerSecond());
                    ImGui.text("Performance Metrics:");

                    // / 1_000_000.0d
                    double total = ExpoServerContainer.get().totalTickDuration / 1_000_000d;
                    double packets = ExpoServerContainer.get().packetTickDuration / 1_000_000d;
                    double world = ExpoServerContainer.get().worldTickDuration / 1_000_000d;
                    double max = 1f / (double) ExpoShared.DEFAULT_LOCAL_TICK_RATE;
                    coloredBulletText(0f, 1f, 1f, "Total: " + total + " (" + (Math.round(total / max * 100d) / 100d) + "%)");
                    coloredBulletText(0f, 1f, 1f, "Packets: " + packets + " (" + (Math.round(packets / max * 100d) / 100d) + "%)");
                    coloredBulletText(0f, 1f, 1f, "World: " + world + " (" + (Math.round(world / max * 100d) / 100d) + "%)");

                    for(ServerDimension dimension : ServerWorld.get().getDimensions()) {
                        ImGui.pushStyleColor(ImGuiCol.Text, 0f, 1f, 0f, 1f);

                        if(ImGui.treeNode(dimension.getDimensionName())) {
                            ImGui.popStyleColor();

                            ImGui.bulletText("Time: " + dimension.dimensionTime);
                            ImGui.bulletText("SpawnX: " + dimension.getDimensionSpawnX());
                            ImGui.bulletText("SpawnY: " + dimension.getDimensionSpawnY());

                            if(ImGui.treeNode(200, "Entities: " + dimension.getEntityManager().entityCount())) {
                                for(ServerEntityType type : dimension.getEntityManager().getExistingEntityTypes()) {
                                    int s = dimension.getEntityManager().getEntitiesOf(type).size();
                                    if(s > 0) ImGui.bulletText(type.name() + ": " + s);
                                }

                                ImGui.treePop();
                            }

                            ImGui.bulletText("Weather: " + dimension.dimensionWeather);
                            ImGui.bulletText("WeatherDuration: " + dimension.dimensionWeatherDuration);
                            ImGui.bulletText("WeatherStrength: " + dimension.dimensionWeatherStrength);

                            ImGui.treePop();
                        } else {
                            ImGui.popStyleColor();
                        }
                    }

                    ImGui.unindent();
                }

                ImGui.textColored(32f/255f, 178f/255f, 170f/255f, 1.0f, "CLIENT");
                ImGui.indent();
                if(local == null) {
                    ImGui.text("Ping: " + ExpoClientContainer.get().getClient().getKryoClient().getReturnTripTime());
                    ImGui.text("Incoming packets queued: " + ExpoClientContainer.get().getClient().getPacketListener().getQueuedPacketAmount());
                }
                if(ImGui.treeNode(300, "Client entities: " + ClientEntityManager.get().entityCount())) {
                    ClientEntityManager m = ClientEntityManager.get();

                    for(ClientEntityType type : m.getExistingEntityTypes()) {
                        int s = m.getEntitiesByType(type).size();
                        if(s > 0) ImGui.bulletText(type.name() + ": " + s);
                    }

                    ImGui.treePop();
                }
                ImGui.unindent();

                ImGui.treePop();
            }

            ImGui.separator();

            if(ImGui.treeNode("Sound Engine")) {
                AudioEngine audio = AudioEngine.get();
                var map = audio.getSoundData();

                ImGui.indent();
                ImGui.textColored(0.0f, 1.0f, 1.0f, 1.0f, "Master volume: " + audio.getMasterVolume());
                ImGui.textColored(0.0f, 1.0f, 1.0f, 1.0f, "Tracked sound effects: " + map.size());

                for(TrackedSoundData data : map.values()) {
                    String volPanData = data.dynamic ? (" @ " + data.postCalcVolume + " & " + data.postCalcPan) : "";
                    ImGui.bulletText(data.id + " = " + data.qualifiedName + volPanData);
                }

                ImGui.unindent();

                ImGui.treePop();
            }

            ImGui.separator();

            if(ImGui.treeNode("Water")) {
                if(ImGui.sliderFloat("Water Reflection Speed", waterReflectionSpeed, 1.0f, 128.0f)) r.waterReflectionSpeed = waterReflectionSpeed[0];
                if(ImGui.sliderFloat("Water Reflection Skew X", waterSkewX, 0.5f, 10.0f)) r.waterSkewX = waterSkewX[0];
                if(ImGui.sliderFloat("Water Reflection Skew Y", waterSkewY, 0.5f, 10.0f)) r.waterSkewY = waterSkewY[0];

                if(ImGui.sliderFloat("Water Alpha", waterAlpha, 0.0f, 1.0f)) {
                    r.waterAlpha = waterAlpha[0];
                }

                if(ImGui.sliderFloat("Flow speed", waterSpeed, 0.0f, 2.0f)) {
                    r.waterSpeed = waterSpeed[0];
                }

                if(ImGui.colorPicker3("Color", waterColor)) {
                    r.waterColor = waterColor;
                }

                if(ImGui.sliderFloat("Contrast", contrast, 0.0f, 2.0f)) {
                    r.contrast = contrast[0];
                }

                if(ImGui.sliderFloat("Brightness", brightness, 0.0f, 2.0f)) {
                    r.brightness = brightness[0];
                }

                ImGui.treePop();
            }

            ImGui.separator();

            if(ImGui.treeNode("UI")) {
                if(ImGui.sliderInt("uiScale", uiScale, 1, 5)) {
                    PlayerUI ui = ExpoClientContainer.get().getPlayerUI();
                    GameSettings.get().uiScale = uiScale[0];
                    RenderContext.get().updatePreferredFonts(uiScale[0]);
                    ui.changeUiScale();
                }

                if(player != null) {
                    if(ImGui.sliderFloat("Blur strength", blurStrength, 0.0f, 16.0f)) {
                        r.blurStrength = blurStrength[0];
                    }

                    if(ImGui.sliderFloat("playerHealth", playerHealth, 0f, 100f)) {
                        player.playerHealth = playerHealth[0];
                    }

                    if(ImGui.sliderFloat("playerHunger", playerHunger, 0f, 100f)) {
                        player.playerHunger = playerHunger[0];
                    }

                    ImGui.text("selectedSlot: " + PlayerInventory.LOCAL_INVENTORY.selectedSlot);
                    ImGui.text("inventoryOpen: " + UIContainerInventory.PLAYER_INVENTORY_CONTAINER.visible);

                    InteractableUIElement iis = ExpoClientContainer.get().getPlayerUI().hoveredSlot;

                    if(iis != null) {
                        ImGui.text("hoveredUiElement: " + iis.inventorySlotId + " " + iis.hovered + " " + iis.selected);
                    }
                }

                ImGui.checkbox("Show slot indices", checkSlotIndices);

                ImGui.treePop();
            }

            ImGui.separator();

            if(ImGui.treeNode("Debug Renderer")) {
                ImGui.checkbox("Interaction Points", renderInteractionPoints);
                ImGui.checkbox("Client Pos", renderClientPos);
                ImGui.checkbox("Server Pos", renderServerPos);
                ImGui.checkbox("Visual Center", renderVisualCenter);
                ImGui.checkbox("Draw Root", renderDrawRoot);
                ImGui.checkbox("Draw Pos", renderDrawPos);
                ImGui.checkbox("J Bump", renderJBump);
                ImGui.checkbox("Chunk Borders", renderChunkBorders);
                ImGui.checkbox("Entity Id", renderEntityId);
                ImGui.checkbox("Entity Generation Box", renderEntityBbox);
                ImGui.checkbox("Player punch data", renderPunchData);
                ImGui.checkbox("Entity hitbox", renderHitbox);

                if(ImGui.sliderFloat("speed", speed, 0.0f, 10.0f)) r.speed = speed[0];
                if(ImGui.sliderFloat("minStrength", minStrength, 0.0f, 1.0f)) r.minStrength = minStrength[0];
                if(ImGui.sliderFloat("maxStrength", maxStrength, 0.0f, 1.0f)) r.maxStrength = maxStrength[0];
                //if(ImGui.sliderFloat("strengthScale", strengthScale, 0.0f, 500.0f)) r.strengthScale = strengthScale[0];
                if(ImGui.sliderFloat("interval", interval, 0.0f, 10.0f)) r.interval = interval[0];
                if(ImGui.sliderFloat("detail", detail, 0.0f, 5.0f)) r.detail = detail[0];
                //if(ImGui.sliderFloat("distortion", distortion, 0.0f, 1.0f)) r.distortion = distortion[0];
                //if(ImGui.sliderFloat("heightOffset", heightOffset, 0.0f, 1.0f)) r.heightOffset = heightOffset[0];
                if(ImGui.sliderFloat("offset", offset, 0.0f, 10.0f)) r.offset = offset[0];
                //if(ImGui.sliderFloat("skew", skew, -500.0f, 500.0f)) r.skew = skew[0];

                ImGui.treePop();
            }

            ImGui.separator();

            if(ImGui.treeNode("AI")) {
                ImGui.checkbox("EntityBrain states", entityBrainStates);

                ImGui.treePop();
            }

            ImGui.separator();
        }

        ImGui.end();
    }

    private void positionColored(float r, float g, float b, String text, float x, float y) {
        String _x = !checkPrecisePositionData.get() ? "" + (int) x : "" + x;
        String _y = !checkPrecisePositionData.get() ? "" + (int) y : "" + y;
        coloredBulletText(r, g, b, text + ": " + _x + ", " + _y);
    }

    private void positionColored(float r, float g, float b, String text, int x, int y) {
        coloredBulletText(r, g, b, text + ": " + x + ", " + y);
    }

    private void positionColoredR(float r, float g, float b, String text, float x, float y) {
        String _x = !checkPrecisePositionData.get() ? "" + (int) x : "" + x;
        String _y = !checkPrecisePositionData.get() ? "" + (int) y : "" + y;
        coloredBulletText(r, g, b, text + " [" + _x + " -> " + _y + "]");
    }

    private void coloredBulletText(float r, float g, float b, String text) {
        ImGui.bullet();
        ImGui.sameLine();
        ImGui.pushStyleColor(ImGuiCol.Text, r, g, b, 1.0f);
        ImGui.text(text);
        ImGui.popStyleColor();
    }

    private void setNightColor(float[] nightColor) {
        ExpoClientContainer.get().getClientWorld().COLOR_AMBIENT_MIDNIGHT.set(nightColor[0], nightColor[1], nightColor[2], 1.0f);
    }

    private void setSunriseColor(float[] sunriseColor) {
        ExpoClientContainer.get().getClientWorld().COLOR_AMBIENT_SUNRISE.set(sunriseColor[0], sunriseColor[1], sunriseColor[2], 1.0f);
    }

    private void setSunsetColor(float[] sunsetColor) {
        ExpoClientContainer.get().getClientWorld().COLOR_AMBIENT_SUNSET.set(sunsetColor[0], sunsetColor[1], sunsetColor[2], 1.0f);
    }

    private void setRainColor(float[] rainColor) {
        ExpoClientContainer.get().getClientWorld().COLOR_RAIN.set(rainColor[0], rainColor[1], rainColor[2], 1.0f);
    }

    private void setTime(int time) {
        ExpoClientContainer.get().getClientWorld().worldTime = time;

        if(ExpoServerLocal.get() != null) {
            ServerWorld.get().getMainDimension().dimensionTime = time;
        }
    }

    public boolean shouldDrawSlotIndices() {
        return checkSlotIndices.get();
    }

}
