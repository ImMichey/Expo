package dev.michey.expo.render.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityManager;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.world.chunk.ClientChunkGrid;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.util.ExpoTime;
import dev.michey.expo.util.Pair;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class PlayerMinimap {

    /** Parent */
    private final PlayerUI ui;

    private final TextureRegion minimap;
    private final TextureRegion minimapArrow;
    private final TextureRegion minimapPlayer;
    private float minimapW, minimapH;
    private float minimapArrowW, minimapArrowH;
    private float minimapPlayerW, minimapPlayerH;

    private final Pixmap pixmap;
    private final Texture pixmapTexture;
    private final HashMap<BiomeType, List<int[]>> biomeMinimap;
    private final HashMap<ClientPlayer, Pair<Float, Float>> drawUsers;

    private int centerTileX, centerTileY;

    public PlayerMinimap(PlayerUI ui, TextureRegion minimap, TextureRegion minimapArrow, TextureRegion minimapPlayer) {
        this.ui = ui;
        this.minimap = minimap;
        this.minimapArrow = minimapArrow;
        this.minimapPlayer = minimapPlayer;

        biomeMinimap = new HashMap<>();
        pixmap = new Pixmap(100, 100, Pixmap.Format.RGBA8888);
        pixmapTexture = new Texture(pixmap);
        pixmap.setBlending(Pixmap.Blending.None);
        drawUsers = new HashMap<>();
    }

    public void updateMinimap() {
        ClientPlayer player = ClientPlayer.getLocalPlayer();
        if(player == null) return;

        int newTileX = ExpoShared.posToTile(player.clientPosX);
        int newTileY = ExpoShared.posToTile(player.clientPosY);

        if((newTileX != centerTileX) || (newTileY != centerTileY)) {
            centerTileX = newTileX;
            centerTileY = newTileY;
            biomeMinimap.clear();

            int startX = centerTileX - 50;
            int startY = centerTileY - 50;

            if(ExpoServerBase.get() != null) {
                for(int i = 0; i < 100; i++) {
                    for(int j = 0; j < 100; j++) {
                        int tx = startX + i;
                        int ty = startY + j;
                        String key = tx + "," + ty;

                        BiomeType type = ServerWorld.get().getMainDimension().getChunkHandler().getBiome(tx, ty, key);

                        if(!biomeMinimap.containsKey(type)) biomeMinimap.put(type, new LinkedList<>());
                        biomeMinimap.get(type).add(new int[] {i, j});
                    }
                }
            } else {
                for(int i = 0; i < 100; i++) {
                    for(int j = 0; j < 100; j++) {
                        int tx = startX + i;
                        int ty = startY + j;
                        String key = tx + "," + ty;

                        BiomeType type = ClientChunkGrid.get().getBiome(tx, ty, key);

                        if(!biomeMinimap.containsKey(type)) biomeMinimap.put(type, new LinkedList<>());
                        biomeMinimap.get(type).add(new int[] {i, j});
                    }
                }
            }

            pixmap.setColor(0f, 0f, 0f, 0f);
            pixmap.fill();

            for(BiomeType b : biomeMinimap.keySet()) {
                float[] color = b.BIOME_COLOR;
                pixmap.setColor(color[0], color[1], color[2], color[3]);
                List<int[]> coords = biomeMinimap.get(b);

                for(int[] coord : coords) {
                    pixmap.drawPixel(coord[0], coord[1]);
                }
            }

            pixmapTexture.draw(pixmap, 0, 0);
        }
    }

    public void draw(RenderContext r) {
        float startX = Gdx.graphics.getWidth() - 2 - minimapW;
        float startY = 2;

        // Background
        r.hudBatch.draw(minimap, startX, startY, minimapW, minimapH);

        // Timer
        float worldTime = ExpoClientContainer.get().getClientWorld().worldTime;

        String worldTimeAsString = ExpoTime.worldTimeString(worldTime);
        ui.glyphLayout.setText(ui.m5x7_shadow_use, worldTimeAsString);
        ui.m5x7_shadow_use.draw(r.hudBatch, worldTimeAsString, startX + 7 * ui.uiScale, startY + ui.glyphLayout.height + 109 * ui.uiScale);

        // Arrow
        float arrowX;

        if(worldTime < 120) {
            arrowX = (ExpoTime.worldDurationHours(22) + worldTime) / ExpoTime.WORLD_CYCLE_DURATION * 69;
        } else {
            arrowX = (worldTime - ExpoTime.worldDurationHours(2)) / ExpoTime.WORLD_CYCLE_DURATION * 69;
        }

        r.hudBatch.draw(minimapArrow, startX + 35 * ui.uiScale + arrowX * ui.uiScale, startY + 105 * ui.uiScale, minimapArrowW, minimapArrowH);

        // Actual map
        r.hudBatch.draw(pixmapTexture, startX + 5 * ui.uiScale, startY + 5 * ui.uiScale + pixmapTexture.getHeight() * ui.uiScale, pixmapTexture.getWidth() * ui.uiScale, pixmapTexture.getHeight() * -ui.uiScale);

        // Player heads + names on minimap
        List<ClientEntity> players = ClientEntityManager.get().getEntitiesByType(ClientEntityType.PLAYER);
        drawUsers.clear();

        for(ClientEntity player : players) {
            ClientPlayer p = (ClientPlayer) player;

            int tileX = ExpoShared.posToTile(player.clientPosX);
            int tileY = ExpoShared.posToTile(player.clientPosY);

            int ctx = centerTileX - 50;
            int cty = centerTileY - 50;

            if(tileX >= ctx && tileY >= cty && tileX < (ctx + 100) && tileY < (cty + 100)) {
                int dx = tileX - ctx;
                int dy = tileY - cty;

                float phx = startX + 5 * ui.uiScale + dx * ui.uiScale;
                float phy = startY + 5 * ui.uiScale + dy * ui.uiScale;

                r.hudBatch.draw(minimapPlayer, phx - ((int) (minimapPlayerW * 0.5f)), phy - ((int) (minimapPlayerH * 0.5f)), minimapPlayerW, minimapPlayerH);

                ui.glyphLayout.setText(ui.m5x7_border_use, p.username);
                float drawAtX = phx - ui.glyphLayout.width * 0.5f;
                float drawAtY = phy + ui.glyphLayout.height + minimapPlayerH;

                if(ServerWorld.get() == null) {
                    drawUsers.put(p, new Pair<>(drawAtX, drawAtY));
                }
            }
        }

        for(ClientPlayer player : drawUsers.keySet()) {
            Pair<Float, Float> pos = drawUsers.get(player);
            ui.m5x7_border_use.draw(r.hudBatch, player.username, pos.key, pos.value);
        }
    }

    public void updateWH(float uiScale) {
        minimapW = minimap.getRegionWidth() * uiScale;
        minimapH = minimap.getRegionHeight() * uiScale;
        minimapArrowW = minimapArrow.getRegionWidth() * uiScale;
        minimapArrowH = minimapArrow.getRegionHeight() * uiScale;
        minimapPlayerW = minimapPlayer.getRegionWidth() * uiScale;
        minimapPlayerH = minimapPlayer.getRegionHeight() * uiScale;
    }

}
