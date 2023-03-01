package dev.michey.expo.render.imgui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import dev.michey.expo.audio.AudioEngine;
import dev.michey.expo.audio.TrackedSoundData;
import dev.michey.expo.localserver.ExpoServerLocal;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.logic.entity.ClientPlayer;
import dev.michey.expo.logic.entity.arch.ClientEntityManager;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.inventory.PlayerInventory;
import dev.michey.expo.logic.world.ClientWorld;
import dev.michey.expo.logic.world.chunk.ClientChunk;
import dev.michey.expo.logic.world.chunk.ClientChunkGrid;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.ui.InteractableItemSlot;
import dev.michey.expo.render.ui.PlayerUI;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.dimension.ServerDimension;
import dev.michey.expo.util.ExpoTime;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.type.ImBoolean;

public class ImGuiExpo {

    private final int[] worldTimeSlider = new int[1];
    private final float[] worldDelta = new float[1];
    private final float[] nightColor = new float[3];
    private final float[] sunriseColor = new float[3];
    private final float[] sunsetColor = new float[3];
    private final float[] currentColor = new float[3];
    private final ImBoolean checkPrecisePositionData = new ImBoolean(false);
    private final ImBoolean checkSlotIndices = new ImBoolean(false);
    private final float[] waterSpeed = new float[1];
    private final float[] waterColor = new float[3];
    private final int[] uiScale = new int[3];
    private final float[] playerHealth = new float[1];
    private final float[] playerHunger = new float[1];

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

            waterColor[0] = r.waterColor[0];
            waterColor[1] = r.waterColor[1];
            waterColor[2] = r.waterColor[2];

            waterSpeed[0] = r.waterSpeed;

            currentColor[0] = w.ambientLightingR;
            currentColor[1] = w.ambientLightingG;
            currentColor[2] = w.ambientLightingB;

            ClientPlayer player = ClientPlayer.getLocalPlayer();

            if(player != null) {
                playerHealth[0] = player.playerHealth;
                playerHunger[0] = player.playerHunger;
            }

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

                ImGui.treePop();
            }

            ImGui.separator();

            if(ImGui.treeNode("Render data")) {
                coloredBulletText(0.0f, 1.0f, 0.5f, "batch: " + r.batch.totalRenderCalls);
                coloredBulletText(0.0f, 1.0f, 0.5f, "arraySpriteBatch: " + r.arraySpriteBatch.totalRenderCalls);
                coloredBulletText(0.0f, 1.0f, 0.5f, "hudBatch: " + r.hudBatch.totalRenderCalls);

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
                    BiomeType t = chunk.biomeData[r.mouseTileArray];
                    coloredBulletText(255f/255f,215f/255f,0f/255f, "Mouse Biome: " + t.name());
                    coloredBulletText(255f/255f,215f/255f,0f/255f, "Mouse Waterlogged: " + chunk.waterLoggedData[r.mouseTileArray]);
                    coloredBulletText(255f/255f,215f/255f,0f/255f, "Mouse Texture Index: " + chunk.tileTextureData[r.mouseTileArray]);
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
                if(ImGui.treeNode(300, "Visible entities: " + ClientEntityManager.get().entityCount())) {
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
                if(ImGui.sliderFloat("Flow speed", waterSpeed, 0.0f, 2.0f)) {
                    r.waterSpeed = waterSpeed[0];
                }

                if(ImGui.colorPicker3("Color", waterColor)) {
                    r.waterColor = waterColor;
                }

                ImGui.treePop();
            }

            ImGui.separator();

            if(ImGui.treeNode("UI")) {
                if(ImGui.sliderInt("uiScale", uiScale, 1, 5)) {
                    PlayerUI ui = ExpoClientContainer.get().getPlayerUI();
                    ui.changeUiScale(uiScale[0]);
                }

                if(player != null) {
                    if(ImGui.sliderFloat("playerHealth", playerHealth, 0f, 100f)) {
                        player.playerHealth = playerHealth[0];
                    }

                    if(ImGui.sliderFloat("playerHunger", playerHunger, 0f, 100f)) {
                        player.playerHunger = playerHunger[0];
                    }

                    ImGui.text("selectedSlot: " + PlayerInventory.LOCAL_INVENTORY.selectedSlot);
                    ImGui.text("inventoryOpen: " + ClientPlayer.getLocalPlayer().inventoryOpen);

                    InteractableItemSlot iis = ExpoClientContainer.get().getPlayerUI().hoveredSlot;

                    if(iis != null) {
                        ImGui.text("hoveredSlot: " + iis.inventorySlotId + " " + iis.hovered + " " + iis.selected);
                    }
                }

                ImGui.checkbox("Show slot indices", checkSlotIndices);

                ImGui.treePop();
            }

            ImGui.separator();

            if(ImGui.treeNode("Debug")) {
                if(player != null) {
                    ImGui.text("currentPunchAngle: " + player.currentPunchAngle);
                    ImGui.text("startPunchAngle: " + player.punchStartAngle);
                    ImGui.text("endPunchAngle: " + player.punchEndAngle);
                }

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
