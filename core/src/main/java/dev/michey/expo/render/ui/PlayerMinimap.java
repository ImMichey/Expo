package dev.michey.expo.render.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.Expo;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityManager;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.misc.ClientDynamic3DTile;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.logic.world.chunk.ClientChunkGrid;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.noise.TileLayerType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.entity.misc.ServerDynamic3DTile;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.main.logic.world.chunk.ServerTile;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.util.ExpoTime;
import dev.michey.expo.util.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static dev.michey.expo.util.ExpoShared.*;

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
    private final HashMap<BiomeType, HashSet<int[]>> minimapBiomeContainerMap;
    private final HashMap<ClientPlayer, Pair<Float, Float>> drawUsers;

    private static final int MAP_SIZE = 96;

    private int centerTileX, centerTileY;
    public boolean incomplete = true;

    public PlayerMinimap(PlayerUI ui, TextureRegion minimap, TextureRegion minimapArrow, TextureRegion minimapPlayer) {
        this.ui = ui;
        this.minimap = minimap;
        this.minimapArrow = minimapArrow;
        this.minimapPlayer = minimapPlayer;

        minimapContainerMap = new HashMap<>();
        minimapBiomeContainerMap = new HashMap<>();

        for(TileLayerType tlt : TileLayerType.values()) {
            minimapContainerMap.put(tlt, new HashSet<>());
        }
        minimapBiomeContainerMap.put(BiomeType.PLAINS, new HashSet<>());
        minimapBiomeContainerMap.put(BiomeType.DENSE_FOREST, new HashSet<>());
        minimapBiomeContainerMap.put(BiomeType.FOREST, new HashSet<>());

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

            for(Map.Entry<TileLayerType, HashSet<int[]>> entrySet : minimapContainerMap.entrySet()) entrySet.getValue().clear();
            for(Map.Entry<BiomeType, HashSet<int[]>> entrySet : minimapBiomeContainerMap.entrySet()) entrySet.getValue().clear();

            int mapBottomCornerX = centerTileX - MAP_SIZE / 2;
            int mapBottomCornerY = centerTileY - MAP_SIZE / 2;
            int existing = 0;
            int shouldExist = Math.min(MAP_SIZE * MAP_SIZE, ROW_TILES * ROW_TILES * PLAYER_CHUNK_VIEW_RANGE_X * PLAYER_CHUNK_VIEW_RANGE_Y);

            int viewportX0 = ExpoShared.posToChunk(player.clientPosX);
            int viewportY0 = ExpoShared.posToChunk(player.clientPosY);
            int startChunkTileX = ExpoShared.posToTile(ExpoShared.chunkToPos(viewportX0)) - ExpoShared.PLAYER_CHUNK_VIEW_RANGE_DIR_X * ROW_TILES;
            int startChunkTileY = ExpoShared.posToTile(ExpoShared.chunkToPos(viewportY0)) - ExpoShared.PLAYER_CHUNK_VIEW_RANGE_DIR_Y * ROW_TILES;
            int endChunkTileX = startChunkTileX + ExpoShared.PLAYER_CHUNK_VIEW_RANGE_X * ROW_TILES;
            int endChunkTileY = startChunkTileY + ExpoShared.PLAYER_CHUNK_VIEW_RANGE_Y * ROW_TILES;

            /*
            ClientUtils.log("[" + incomplete + "] tileLoopSize=" + POSSIBLE_MAP_SIZE_X + "," + POSSIBLE_MAP_SIZE_Y +
                    " :: diff=" + diffX + "," + diffY +
                    " :: player=" + player.clientPosX + "," + player.clientPosY +
                    " :: chunkToPos=" + ExpoShared.chunkToPos(viewportX0) + "," + ExpoShared.chunkToPos(viewportY0) +
                    " :: center=" + centerTileX + "," + centerTileY + " :: viewport=" + viewportX0 + "," + viewportY0 +
                    " :: sct=" + startChunkTileX + "," + startChunkTileY + " :: ect=" + endChunkTileX + "," + endChunkTileY, Input.Keys.X);
            */

            if(!Expo.get().isMultiplayer()) {
                ServerPlayer localPlayer = ServerPlayer.getLocalPlayer();

                for(int i = 0; i < MAP_SIZE; i++) {
                    for(int j = 0; j < MAP_SIZE; j++) {
                        int tx = mapBottomCornerX + i;
                        int ty = mapBottomCornerY + j;

                        ServerTile tile = localPlayer.getDimension().getChunkHandler().getTile(tx, ty);

                        if(tile == null || tile.dynamicTileParts == null || tile.dynamicTileParts[2].emulatingType == null) {
                            // Tile does not exist.
                            continue;
                        }

                        if(tx >= startChunkTileX && tx < endChunkTileX && ty >= startChunkTileY && ty < endChunkTileY) {
                            existing++;
                        }

                        TileLayerType use;
                        BiomeType useBiome = null;
                        TileLayerType t1 = tile.dynamicTileParts[1].emulatingType;

                        if(t1 == TileLayerType.WATER_DEEP || t1 == TileLayerType.SOIL_DEEP_WATERLOGGED) {
                            use = TileLayerType.WATER_DEEP;
                        } else {
                            ServerEntity sd3d = tile.hasTileBasedEntity(ServerEntityType.DYNAMIC_3D_TILE);

                            if(sd3d != null) {
                                use = ((ServerDynamic3DTile) sd3d).emulatingType;
                            } else {
                                use = tile.dynamicTileParts[2].emulatingType;

                                if(tile.biome.FOLIAGE_INDEX) {
                                    useBiome = tile.biome;
                                } else {
                                    if(use.TILE_COLOR == 255) {
                                        use = tile.dynamicTileParts[1].emulatingType;

                                        if(use.TILE_COLOR == 255) {
                                            use = tile.dynamicTileParts[0].emulatingType;
                                        }
                                    }
                                }
                            }
                        }

                        /*
                        if(use == TileLayerType.FOREST) {
                            if(tile.biome == BiomeType.FOREST || tile.biome == BiomeType.DENSE_FOREST || tile.biome == BiomeType.PLAINS) {
                                useBiome = tile.biome;
                            }
                        }
                        */

                        if(useBiome != null) {
                            minimapBiomeContainerMap.get(useBiome).add(new int[] {i, j});
                        } else {
                            minimapContainerMap.get(use).add(new int[] {i, j});
                        }
                    }
                }
            } else {
                for(int i = 0; i < MAP_SIZE; i++) {
                    for(int j = 0; j < MAP_SIZE; j++) {
                        int tx = mapBottomCornerX + i;
                        int ty = mapBottomCornerY + j;

                        int cx = ExpoShared.posToChunk(ExpoShared.tileToPos(tx));
                        int cy = ExpoShared.posToChunk(ExpoShared.tileToPos(ty));

                        var chunk = ClientChunkGrid.get().getChunk(cx, cy);

                        if(chunk == null) {
                            continue;
                        }

                        if(tx >= startChunkTileX && tx < endChunkTileX && ty >= startChunkTileY && ty < endChunkTileY) {
                            existing++;
                        }

                        int startTileX = ExpoShared.posToTile(ExpoShared.chunkToPos(cx));
                        int startTileY = ExpoShared.posToTile(ExpoShared.chunkToPos(cy));
                        int relativeTileX = tx - startTileX;
                        int relativeTileY = ty - startTileY;
                        int tileArray = relativeTileY * ROW_TILES + relativeTileX;

                        TileLayerType use;
                        BiomeType useBiome = null;
                        TileLayerType t1 = chunk.dynamicTiles[tileArray][1].emulatingType;

                        if(t1 == TileLayerType.WATER_DEEP || t1 == TileLayerType.SOIL_DEEP_WATERLOGGED) {
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

                                if(chunk.biomes[tileArray].FOLIAGE_INDEX) {
                                    useBiome = chunk.biomes[tileArray];
                                }

                                if(use.TILE_COLOR == 255) {
                                    use = chunk.dynamicTiles[tileArray][1].emulatingType;

                                    if(use.TILE_COLOR == 255) {
                                        use = chunk.dynamicTiles[tileArray][0].emulatingType;
                                    }
                                }
                            }
                        }

                        /*
                        if(use == TileLayerType.FOREST) {
                            if(chunk.biomes[tileArray] == BiomeType.FOREST || chunk.biomes[tileArray] == BiomeType.DENSE_FOREST || chunk.biomes[tileArray] == BiomeType.PLAINS) {
                                useBiome = chunk.biomes[tileArray];
                            }
                        }
                        */

                        if(useBiome != null) {
                            minimapBiomeContainerMap.get(useBiome).add(new int[] {i, j});
                        } else {
                            minimapContainerMap.get(use).add(new int[] {i, j});
                        }
                    }
                }

            }

            if(shouldExist != existing) {
                incomplete = true;
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

            for(BiomeType bt : minimapBiomeContainerMap.keySet()) {
                pixmap.setColor(bt.BIOME_COLOR[0], bt.BIOME_COLOR[1], bt.BIOME_COLOR[2], 1.0f);
                var coords = minimapBiomeContainerMap.get(bt);

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
        r.m5x7_shadow_use.draw(r.hudBatch, worldTimeAsString, (int) (startX + 5 * ui.uiScale + (28 * ui.uiScale - ui.glyphLayout.width) * 0.5f), (int) (startY + ui.glyphLayout.height + 105 * ui.uiScale));

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
        players.sort(ClientEntityManager.depthSorter); // We sort the player list by depth to make it more coherent with the ingame draw order
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

                if(p.player) {
                    float drawPlayerHeadX = phx - ((int) (minimapPlayerW * 0.5f));
                    float drawPlayerHeadY = phy - ((int) (minimapPlayerH * 0.5f));

                    r.hudBatch.setShader(r.whiteShaderDefault);
                    r.hudBatch.draw(minimapPlayer, drawPlayerHeadX - 1, drawPlayerHeadY, minimapPlayerW, minimapPlayerH);
                    r.hudBatch.draw(minimapPlayer, drawPlayerHeadX + 1, drawPlayerHeadY, minimapPlayerW, minimapPlayerH);
                    r.hudBatch.draw(minimapPlayer, drawPlayerHeadX, drawPlayerHeadY - 1, minimapPlayerW, minimapPlayerH);
                    r.hudBatch.draw(minimapPlayer, drawPlayerHeadX, drawPlayerHeadY + 1, minimapPlayerW, minimapPlayerH);
                    r.hudBatch.setShader(r.DEFAULT_GLES3_SHADER);

                    r.hudBatch.draw(minimapPlayer, drawPlayerHeadX, drawPlayerHeadY, minimapPlayerW, minimapPlayerH);
                } else {
                    r.hudBatch.draw(minimapPlayer, phx - ((int) (minimapPlayerW * 0.5f)), phy - ((int) (minimapPlayerH * 0.5f)), minimapPlayerW, minimapPlayerH);
                }

                ui.glyphLayout.setText(r.m5x7_border_use, p.username);
                float drawAtX = phx - ui.glyphLayout.width * 0.5f;
                float drawAtY = phy + ui.glyphLayout.height + minimapPlayerH;

                if(Expo.get().isMultiplayer()) {
                    drawUsers.put(p, new Pair<>(drawAtX, drawAtY));
                }
            }
        }

        if(Expo.get().isMultiplayer()) {
            for(ClientEntity player : players) {
                ClientPlayer p = (ClientPlayer) player;
                Pair<Float, Float> data = drawUsers.get(p);
                if(data == null) continue; // Requires a null check to prevent a crash when the player moves between visible chunks in the same frame
                r.m5x7_border_use.draw(r.hudBatch, p.username, data.key, data.value);
            }
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
