package dev.michey.expo.render.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.logic.entity.misc.ClientDynamic3DTile;
import dev.michey.expo.logic.entity.misc.ClientFenceStick;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityManager;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.world.chunk.ClientChunkGrid;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.noise.TileLayerType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.entity.misc.ServerDynamic3DTile;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.chunk.ServerTile;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.util.ExpoTime;
import dev.michey.expo.util.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import static dev.michey.expo.util.ExpoShared.ROW_TILES;

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
    private final HashMap<TileLayerType, HashSet<int[]>> minimapContainerMap;
    private final HashMap<ClientPlayer, Pair<Float, Float>> drawUsers;

    private final int MAP_SIZE = 96;

    private int centerTileX, centerTileY;
    public boolean incomplete = true;

    public PlayerMinimap(PlayerUI ui, TextureRegion minimap, TextureRegion minimapArrow, TextureRegion minimapPlayer) {
        this.ui = ui;
        this.minimap = minimap;
        this.minimapArrow = minimapArrow;
        this.minimapPlayer = minimapPlayer;

        minimapContainerMap = new HashMap<>();
        pixmap = new Pixmap(MAP_SIZE, MAP_SIZE, Pixmap.Format.RGBA8888);
        pixmapTexture = new Texture(pixmap);
        pixmap.setBlending(Pixmap.Blending.None);
        drawUsers = new HashMap<>();
    }

    public void updateMinimap() {
        ClientPlayer player = ClientPlayer.getLocalPlayer();
        if(player == null) return;

        int newTileX = ExpoShared.posToTile(player.clientPosX);
        int newTileY = ExpoShared.posToTile(player.clientPosY);

        if(incomplete || (newTileX != centerTileX) || (newTileY != centerTileY)) {
            incomplete = false;
            centerTileX = newTileX;
            centerTileY = newTileY;
            minimapContainerMap.clear();

            for(TileLayerType tlt : TileLayerType.values()) {
                minimapContainerMap.put(tlt, new HashSet<>());
            }

            int startX = centerTileX - MAP_SIZE / 2;
            int startY = centerTileY - MAP_SIZE / 2;

            if(ExpoServerBase.get() != null) {
                ServerPlayer localPlayer = ServerPlayer.getLocalPlayer();

                for(int i = 0; i < MAP_SIZE; i++) {
                    for(int j = 0; j < MAP_SIZE; j++) {
                        int tx = startX + i;
                        int ty = startY + j;

                        ServerTile tile = localPlayer.getDimension().getChunkHandler().getTile(tx, ty);

                        if(tile == null || tile.dynamicTileParts == null || tile.dynamicTileParts[2].emulatingType == null) {
                            incomplete = true;
                            continue;
                        }

                        TileLayerType use;

                        if(tile.biome == BiomeType.OCEAN_DEEP) {
                            use = TileLayerType.WATER_DEEP;
                        } else {
                            ServerEntity sd3d = tile.hasTileBasedEntity(ServerEntityType.DYNAMIC_3D_TILE);

                            if(sd3d != null) {
                                use = ((ServerDynamic3DTile) sd3d).emulatingType;
                            } else {
                                use = tile.dynamicTileParts[2].emulatingType;

                                if(use.TILE_COLOR == 255) {
                                    use = tile.dynamicTileParts[1].emulatingType;

                                    if(use.TILE_COLOR == 255) {
                                        use = tile.dynamicTileParts[0].emulatingType;
                                    }
                                }
                            }
                        }

                        minimapContainerMap.get(use).add(new int[] {i, j});
                    }
                }
            } else {
                for(int i = 0; i < MAP_SIZE; i++) {
                    for(int j = 0; j < MAP_SIZE; j++) {
                        int tx = startX + i;
                        int ty = startY + j;

                        int cx = ExpoShared.posToChunk(ExpoShared.tileToPos(tx));
                        int cy = ExpoShared.posToChunk(ExpoShared.tileToPos(ty));

                        var chunk = ClientChunkGrid.get().getChunk(cx, cy);

                        if(chunk == null) {
                            incomplete = true;
                            continue;
                        }

                        int startTileX = ExpoShared.posToTile(ExpoShared.chunkToPos(cx));
                        int startTileY = ExpoShared.posToTile(ExpoShared.chunkToPos(cy));
                        int relativeTileX = tx - startTileX;
                        int relativeTileY = ty - startTileY;
                        int tileArray = relativeTileY * ROW_TILES + relativeTileX;

                        TileLayerType use;

                        if(chunk.biomes[tileArray] == BiomeType.OCEAN_DEEP) {
                            use = TileLayerType.WATER_DEEP;
                        } else {
                            ClientEntity te = null;

                            if(chunk.tileEntityGrid != null) {
                                te = chunk.getTileEntityAt(tileArray, 0, 0);
                            }

                            if(te instanceof ClientDynamic3DTile cd3d) {
                                use = cd3d.emulatingType;
                            } else {
                                use = chunk.dynamicTiles[tileArray][2].emulatingType;

                                if(use.TILE_COLOR == 255) {
                                    use = chunk.dynamicTiles[tileArray][1].emulatingType;

                                    if(use.TILE_COLOR == 255) {
                                        use = chunk.dynamicTiles[tileArray][0].emulatingType;
                                    }
                                }
                            }
                        }

                        minimapContainerMap.get(use).add(new int[] {i, j});
                    }
                }
            }

            pixmap.setColor(0f, 0f, 0f, 0f);
            pixmap.fill();

            for(TileLayerType tlt : minimapContainerMap.keySet()) {
                pixmap.setColor(tlt.TILE_COLOR);
                var coords = minimapContainerMap.get(tlt);

                for(int[] d : coords) {
                    pixmap.drawPixel(d[0], d[1]);
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
        ui.glyphLayout.setText(r.m5x7_shadow_use, worldTimeAsString);
        r.m5x7_shadow_use.draw(r.hudBatch, worldTimeAsString, startX + 5 * ui.uiScale + (28 * ui.uiScale - ui.glyphLayout.width) * 0.5f, startY + ui.glyphLayout.height + 105 * ui.uiScale);

        // Arrow
        float arrowX;

        if(worldTime < 120) {
            arrowX = (ExpoTime.worldDurationHours(22) + worldTime) / ExpoTime.WORLD_CYCLE_DURATION * 67;
        } else {
            arrowX = (worldTime - ExpoTime.worldDurationHours(2)) / ExpoTime.WORLD_CYCLE_DURATION * 67;
        }

        r.hudBatch.draw(minimapArrow, startX + 34 * ui.uiScale + arrowX * ui.uiScale, startY + 101 * ui.uiScale, minimapArrowW, minimapArrowH);

        // Actual map
        r.hudBatch.draw(pixmapTexture, startX + 5 * ui.uiScale, startY + 5 * ui.uiScale + pixmapTexture.getHeight() * ui.uiScale, pixmapTexture.getWidth() * ui.uiScale, pixmapTexture.getHeight() * -ui.uiScale);

        // Player heads + names on minimap
        List<ClientEntity> players = ClientEntityManager.get().getEntitiesByType(ClientEntityType.PLAYER);
        drawUsers.clear();

        for(ClientEntity player : players) {
            ClientPlayer p = (ClientPlayer) player;

            int tileX = ExpoShared.posToTile(player.clientPosX);
            int tileY = ExpoShared.posToTile(player.clientPosY);

            int ctx = centerTileX - (MAP_SIZE / 2);
            int cty = centerTileY - (MAP_SIZE / 2);

            if(tileX >= ctx && tileY >= cty && tileX < (ctx + MAP_SIZE) && tileY < (cty + MAP_SIZE)) {
                int dx = tileX - ctx;
                int dy = tileY - cty;

                float phx = startX + 5 * ui.uiScale + dx * ui.uiScale;
                float phy = startY + 5 * ui.uiScale + dy * ui.uiScale;

                r.hudBatch.draw(minimapPlayer, phx - ((int) (minimapPlayerW * 0.5f)), phy - ((int) (minimapPlayerH * 0.5f)), minimapPlayerW, minimapPlayerH);

                ui.glyphLayout.setText(r.m5x7_border_use, p.username);
                float drawAtX = phx - ui.glyphLayout.width * 0.5f;
                float drawAtY = phy + ui.glyphLayout.height + minimapPlayerH;

                if(ServerWorld.get() == null) {
                    drawUsers.put(p, new Pair<>(drawAtX, drawAtY));
                }
            }
        }

        for(ClientPlayer player : drawUsers.keySet()) {
            Pair<Float, Float> pos = drawUsers.get(player);
            r.m5x7_border_use.draw(r.hudBatch, player.username, pos.key, pos.value);
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
