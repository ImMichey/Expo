package dev.michey.expo.logic.world.chunk;

import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityManager;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.logic.world.ClientWorld;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.noise.TileLayerType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.server.main.logic.world.chunk.DynamicTilePart;
import dev.michey.expo.server.packet.P32_ChunkDataSingle;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.util.Pair;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static dev.michey.expo.util.ExpoShared.*;

public class ClientChunk {

    // passed by server
    public int chunkX;
    public int chunkY;
    public BiomeType[] biomes;
    public ClientDynamicTilePart[][] dynamicTiles;
    public float[] grassColor;

    public int[] tileEntityGrid; // is null by default
    private int tileEntityCount;
    public boolean ranAmbientOcclusion;
    private int initializationTileCount;

    // SET BY CLIENT
    public Pair[][] waterDisplacement;
    public float[][] ambientOcclusion;

    // generated by client
    public boolean chunkContainsWater;
    public int chunkDrawBeginX;
    public int chunkDrawBeginY;
    public int chunkDrawEndX;
    public int chunkDrawEndY;
    public boolean visibleRender;
    public boolean visibleLogic;

    public void updateVisibility(boolean render, boolean logic) {
        if(render) {
            visibleRender = RenderContext.get().inDrawBounds(this);
        }
        if(logic) {
            ClientPlayer player = ClientPlayer.getLocalPlayer();
            if(player == null) return;
            int[] viewport = player.clientViewport;

            boolean before = visibleLogic;
            visibleLogic = chunkX >= viewport[0] && chunkX <= viewport[1] && chunkY >= viewport[2] && chunkY <= viewport[3];

            if(before && !visibleLogic) {
                ranAmbientOcclusion = false;
            }
        }
    }

    /** Attaches a tile entity to the current chunk and returns the new amount of tile entities within this chunk. */
    public int attachTileEntity(int entityId, int tileArray) {
        tileEntityCount++;

        if(tileEntityGrid == null) {
            tileEntityGrid = new int[ROW_TILES * ROW_TILES];
            Arrays.fill(tileEntityGrid, -1);
        }

        tileEntityGrid[tileArray] = entityId;
        return tileEntityCount;
    }

    /** Detaches a tile entity from the current chunk and returns the new amount of tile entities within this chunk. */
    public int detachTileEntity(int tileArray) {
        tileEntityCount--;
        if(tileEntityGrid == null) {
            ExpoLogger.log("DETACH " + tileArray + " new: " + tileEntityCount);
        }
        tileEntityGrid[tileArray] = -1;
        return tileEntityCount;
    }

    public ClientChunk(int chunkX, int chunkY, BiomeType[] biomes, DynamicTilePart[][] dynamicTiles, float[] grassColor, int initializationTileCount) {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.biomes = biomes;
        this.dynamicTiles = convertToClient(dynamicTiles);
        this.grassColor = grassColor;
        this.initializationTileCount = initializationTileCount;

        chunkDrawBeginX = ExpoShared.chunkToPos(chunkX);
        chunkDrawBeginY = ExpoShared.chunkToPos(chunkY);
        chunkDrawEndX = chunkDrawBeginX + CHUNK_SIZE;
        chunkDrawEndY = chunkDrawBeginY + CHUNK_SIZE;

        for(BiomeType t : biomes) {
            if(BiomeType.isWater(t)) {
                chunkContainsWater = true;
                break;
            }
        }

        if(chunkContainsWater) {
            waterDisplacement = new Pair[dynamicTiles.length][];

            for(int i = 0; i < waterDisplacement.length; i++) {
                var tiles = dynamicTiles[i];

                for(DynamicTilePart tile : tiles) {
                    int[] ids = tile.layerIds;

                    if(BiomeType.isWater(biomes[i]) && biomes[i] != BiomeType.OCEAN_DEEP && biomes[i] != BiomeType.PUDDLE && ids.length > 1) {
                        Pair[] array = new Pair[ids.length];

                        for(int a = 0; a < ids.length; a++) {
                            array[a] = idToDir(ids[a], tile.emulatingType);
                        }

                        waterDisplacement[i] = array;
                    }
                }
            }
        }

        ambientOcclusion = new float[ROW_TILES * ROW_TILES][];
        for(int i = 0; i < ambientOcclusion.length; i++) ambientOcclusion[i] = new float[4];
    }

    public Pair<ClientChunk, Integer> getTileAt(int tileArray, int xIncrement, int yIncrement) {
        int _chunkX = chunkX;
        int _chunkY = chunkY;
        int x = tileArray % ROW_TILES;
        int y = tileArray / ROW_TILES;
        x += xIncrement;
        y += yIncrement;

        if(x < 0) {
            _chunkX -= 1;
            x += ROW_TILES;
        } else if(x >= ROW_TILES) {
            _chunkX += 1;
            x -= ROW_TILES;
        }

        if(y < 0) {
            _chunkY -= 1;
            y += ROW_TILES;
        } else if(y >= ROW_TILES) {
            _chunkY += 1;
            y -= ROW_TILES;
        }

        ClientChunk existingChunk = ClientChunkGrid.get().getChunk(_chunkX, _chunkY);
        if(existingChunk == null) return null;

        // Might break something in the future, not sure. Commented out to prevent a bug when updating broken 3D tiles.
        // if(existingChunk.tileEntityGrid == null) return null;

        return new Pair<>(existingChunk, y * ROW_TILES + x);
    }

    public ClientEntity getTileEntityAt(int tileArray, int xIncrement, int yIncrement) {
        if(xIncrement == 0 && yIncrement == 0) {
            int entityId = tileEntityGrid[tileArray];
            if(entityId == -1) return null;
            return ClientEntityManager.get().getEntityById(entityId);
        } else {
            int _chunkX = chunkX;
            int _chunkY = chunkY;
            int x = tileArray % ROW_TILES;
            int y = tileArray / ROW_TILES;
            x += xIncrement;
            y += yIncrement;

            if(x < 0) {
                _chunkX -= 1;
                x += ROW_TILES;
            } else if(x == ROW_TILES) {
                _chunkX += 1;
                x = 0;
            }

            if(y < 0) {
                _chunkY -= 1;
                y += ROW_TILES;
            } else if(y == ROW_TILES) {
                _chunkY += 1;
                y = 0;
            }

            ClientChunk existingChunk = ClientChunkGrid.get().getChunk(_chunkX, _chunkY);
            if(existingChunk == null) return null;
            if(existingChunk.tileEntityGrid == null) return null;

            int newTileArray = y * ROW_TILES + x;
            int newId = existingChunk.tileEntityGrid[newTileArray];
            if(newId == -1) return null;
            return ClientEntityManager.get().getEntityById(newId);
        }
    }

    private void addIfNotNull(Pair<ClientChunk, Integer> pair) {
        if(pair != null) neighbourList.add(pair);
    }

    private List<Pair<ClientChunk, Integer>> getNeighbouringTiles(int tileArray) {
        neighbourList.clear();

        addIfNotNull(getTileAt(tileArray, 0, 1));  // N
        addIfNotNull(getTileAt(tileArray, 0, -1)); // S
        addIfNotNull(getTileAt(tileArray, 1, 0));  // E
        addIfNotNull(getTileAt(tileArray, -1, 0)); // W

        addIfNotNull(getTileAt(tileArray, -1, 1)); // NW
        addIfNotNull(getTileAt(tileArray, 1, 1));  // NE
        addIfNotNull(getTileAt(tileArray, 1, -1)); // SE
        addIfNotNull(getTileAt(tileArray, -1, -1)); // SW

        return neighbourList;
    }

    public void updateAmbientOcclusion(int tileArray, boolean neighbours, boolean ignoreNullGrid) {
        if(!ignoreNullGrid) {
            if(tileEntityGrid == null) return;
        }

        if(tileEntityGrid != null && tileEntityGrid[tileArray] != -1) {
            ClientEntity entity = getTileEntityAt(tileArray, 0, 0);

            if(entity != null) {
                if(entity.getEntityType() == ClientEntityType.DYNAMIC_3D_TILE) {
                    Arrays.fill(ambientOcclusion[tileArray], 1.0f);
                } else {
                    Arrays.fill(ambientOcclusion[tileArray], 0.0f);
                }
            } else {
                Arrays.fill(ambientOcclusion[tileArray], 0.0f);
            }
        } else {
            // neighbours
            ClientEntity[] x = getNeighbouringEntities(tileArray);

            boolean n = x[0] != null && x[0].getEntityType() == ClientEntityType.DYNAMIC_3D_TILE;
            boolean e = x[1] != null && x[1].getEntityType() == ClientEntityType.DYNAMIC_3D_TILE;
            boolean s = x[2] != null && x[2].getEntityType() == ClientEntityType.DYNAMIC_3D_TILE;
            boolean w = x[3] != null && x[3].getEntityType() == ClientEntityType.DYNAMIC_3D_TILE;
            boolean ne = x[4] != null && x[4].getEntityType() == ClientEntityType.DYNAMIC_3D_TILE;
            boolean se = x[5] != null && x[5].getEntityType() == ClientEntityType.DYNAMIC_3D_TILE;
            boolean sw = x[6] != null && x[6].getEntityType() == ClientEntityType.DYNAMIC_3D_TILE;
            boolean nw = x[7] != null && x[7].getEntityType() == ClientEntityType.DYNAMIC_3D_TILE;

            if(s || w || sw) {
                ambientOcclusion[tileArray][0] = 1.0f;
            } else {
                ambientOcclusion[tileArray][0] = 0.0f;
            }

            // [1] = Top Left
            if(n || w || nw) {
                ambientOcclusion[tileArray][1] = 1.0f;
            } else {
                ambientOcclusion[tileArray][1] = 0.0f;
            }

            // [2] = Top Right
            if(n || e || ne) {
                ambientOcclusion[tileArray][2] = 1.0f;
            } else {
                ambientOcclusion[tileArray][2] = 0.0f;
            }

            // [3] = Bottom Right
            if(s || e || se) {
                ambientOcclusion[tileArray][3] = 1.0f;
            } else {
                ambientOcclusion[tileArray][3] = 0.0f;
            }
        }

        // Do neighbours
        if(neighbours) {
            for(var pair : getNeighbouringTiles(tileArray)) {
                pair.key.updateAmbientOcclusion(pair.value, false, true);
            }
        }
    }

    private static final ClientEntity[] iterationArray = new ClientEntity[8];
    private static final LinkedList<Pair<ClientChunk, Integer>> neighbourList = new LinkedList<>();

    public ClientEntity[] getNeighbouringEntitiesNESW(int tileArray) {
        iterationArray[0] = getTileEntityAt(tileArray, 0, 1);
        iterationArray[1] = getTileEntityAt(tileArray, 1, 0);
        iterationArray[2] = getTileEntityAt(tileArray, 0, -1);
        iterationArray[3] = getTileEntityAt(tileArray, -1, 0);
        iterationArray[4] = null;
        iterationArray[5] = null;
        iterationArray[6] = null;
        iterationArray[7] = null;
        return iterationArray;
    }

    public ClientEntity[] getNeighbouringEntities(int tileArray) {
        iterationArray[0] = getTileEntityAt(tileArray, 0, 1);       // N
        iterationArray[1] = getTileEntityAt(tileArray, 1, 0);       // E
        iterationArray[2] = getTileEntityAt(tileArray, 0, -1);      // S
        iterationArray[3] = getTileEntityAt(tileArray, -1, 0);      // W
        iterationArray[4] = getTileEntityAt(tileArray, 1, 1);       // NE
        iterationArray[5] = getTileEntityAt(tileArray, 1, -1);      // SE
        iterationArray[6] = getTileEntityAt(tileArray, -1, -1);     // SW
        iterationArray[7] = getTileEntityAt(tileArray, -1, 1);      // NW

        return iterationArray;
    }

    public void generateAmbientOcclusion(boolean skipGrid) {
        if(skipGrid) {
            for(int i = 0; i < ambientOcclusion.length; i++) getAmbientOcclusionAt(i);
        } else {
            if(tileEntityGrid == null) return; // No tile entities, skip

            for(int i = 0; i < ambientOcclusion.length; i++) {
                if(tileEntityGrid[i] != -1) {
                    ClientEntity entity = getTileEntityAt(i, 0, 0);

                    if(entity != null) {
                        if(entity.getEntityType() == ClientEntityType.DYNAMIC_3D_TILE) {
                            Arrays.fill(ambientOcclusion[i], 1.0f);
                        }
                    }
                } else {
                    getAmbientOcclusionAt(i);
                }
            }
        }
    }

    private void getAmbientOcclusionAt(int index) {
        // neighbours
        ClientEntity[] x = getNeighbouringEntities(index);

        boolean n = x[0] != null && x[0].getEntityType() == ClientEntityType.DYNAMIC_3D_TILE;
        boolean e = x[1] != null && x[1].getEntityType() == ClientEntityType.DYNAMIC_3D_TILE;
        boolean s = x[2] != null && x[2].getEntityType() == ClientEntityType.DYNAMIC_3D_TILE;
        boolean w = x[3] != null && x[3].getEntityType() == ClientEntityType.DYNAMIC_3D_TILE;
        boolean ne = x[4] != null && x[4].getEntityType() == ClientEntityType.DYNAMIC_3D_TILE;
        boolean se = x[5] != null && x[5].getEntityType() == ClientEntityType.DYNAMIC_3D_TILE;
        boolean sw = x[6] != null && x[6].getEntityType() == ClientEntityType.DYNAMIC_3D_TILE;
        boolean nw = x[7] != null && x[7].getEntityType() == ClientEntityType.DYNAMIC_3D_TILE;

        if(s || w || sw) {
            ambientOcclusion[index][0] = 1.0f;
        } else {
            ambientOcclusion[index][0] = 0.0f;
        }

        // [1] = Top Left
        if(n || w || nw) {
            ambientOcclusion[index][1] = 1.0f;
        } else {
            ambientOcclusion[index][1] = 0.0f;
        }

        // [2] = Top Right
        if(n || e || ne) {
            ambientOcclusion[index][2] = 1.0f;
        } else {
            ambientOcclusion[index][2] = 0.0f;
        }

        // [3] = Bottom Right
        if(s || e || se) {
            ambientOcclusion[index][3] = 1.0f;
        } else {
            ambientOcclusion[index][3] = 0.0f;
        }
    }

    public boolean hasGridNeighbours() {
        ClientChunk n = ClientChunkGrid.get().getChunk(chunkX, chunkY + 1);
        ClientChunk e = ClientChunkGrid.get().getChunk(chunkX + 1, chunkY);
        ClientChunk s = ClientChunkGrid.get().getChunk(chunkX, chunkY - 1);
        ClientChunk w = ClientChunkGrid.get().getChunk(chunkX - 1, chunkY);
        ClientChunk ne = ClientChunkGrid.get().getChunk(chunkX + 1, chunkY + 1);
        ClientChunk se = ClientChunkGrid.get().getChunk(chunkX + 1, chunkY - 1);
        ClientChunk sw = ClientChunkGrid.get().getChunk(chunkX - 1, chunkY - 1);
        ClientChunk nw = ClientChunkGrid.get().getChunk(chunkX - 1, chunkY + 1);

        boolean _n = n != null && (n.getInitializationTileCount() == 0 || (n.tileEntityCount == n.getInitializationTileCount()));
        boolean _e = e != null && (e.getInitializationTileCount() == 0 || (e.tileEntityCount == e.getInitializationTileCount()));
        boolean _s = s != null && (s.getInitializationTileCount() == 0 || (s.tileEntityCount == s.getInitializationTileCount()));
        boolean _w = w != null && (w.getInitializationTileCount() == 0 || (w.tileEntityCount == w.getInitializationTileCount()));

        boolean _ne = ne != null && (ne.getInitializationTileCount() == 0 || (ne.tileEntityCount == ne.getInitializationTileCount()));
        boolean _se = se != null && (se.getInitializationTileCount() == 0 || (se.tileEntityCount == se.getInitializationTileCount()));
        boolean _sw = sw != null && (sw.getInitializationTileCount() == 0 || (sw.tileEntityCount == sw.getInitializationTileCount()));
        boolean _nw = nw != null && (nw.getInitializationTileCount() == 0 || (nw.tileEntityCount == nw.getInitializationTileCount()));

        return _n && _e && _s && _w && _ne && _se && _sw && _nw;
    }

    public void updateSingle(P32_ChunkDataSingle p) {
        this.dynamicTiles[p.tileArray][p.layer].updateFrom(p.tile);
        this.grassColor[p.tileArray] = p.grassColor;
    }

    public void update(BiomeType[] biomes, DynamicTilePart[][] individualTileData, float[] grassColor, int initializationTileCount) {
        this.biomes = biomes;
        this.grassColor = grassColor;
        this.initializationTileCount = initializationTileCount;
        this.ranAmbientOcclusion = false;

        for(int i = 0; i < individualTileData.length; i++) {
            DynamicTilePart[] server = individualTileData[i];
            ClientDynamicTilePart[] client = this.dynamicTiles[i];

            for(int j = 0; j < server.length; j++) {
                client[j].updateFrom(server[j]);
            }
        }
    }

    private Pair<Integer, Integer> idToDir(int id, TileLayerType type) {
        int dirX = 0, dirY = 0;

        switch(id - type.TILE_ID_DATA[0]) {
            case 3, 6 -> {
                dirX = 0;
                dirY = -1;
            }
            case 2 -> {
                dirX = 1;
                dirY = -1;
            }
            case 9, 15 -> {
                dirX = -1;
                dirY = 0;
            }
            case 4, 10 -> {
                dirX = 1;
                dirY = 0;
            }
            case 7 -> {
                dirX = -1;
                dirY = -1;
            }
            case 12 -> {
                dirX = 1;
                dirY = 1;
            }
            case 13, 16 -> {
                dirX = 0;
                dirY = 1;
            }
            case 17 -> {
                dirX = -1;
                dirY = 1;
            }
            case 18, 19, 20, 21 -> {
                dirX = 0;
                dirY = 0;
            }
        }

        return new Pair<>(dirX, dirY);
    }

    private ClientDynamicTilePart[][] convertToClient(DynamicTilePart[][] tiles) {
        ClientDynamicTilePart[][] array = new ClientDynamicTilePart[tiles.length][];
        float startX = ExpoShared.chunkToPos(chunkX);
        float startY = ExpoShared.chunkToPos(chunkY);

        for(int i = 0; i < tiles.length; i++) {
            DynamicTilePart[] server = tiles[i];
            int x = i % ROW_TILES;
            int y = i / ROW_TILES;

            array[i] = new ClientDynamicTilePart[server.length];

            for(int j = 0; j < server.length; j++) {
                array[i][j] = new ClientDynamicTilePart(this, startX + TILE_SIZE * x, startY + TILE_SIZE * y, server[j]);
            }
        }

        return array;
    }

    public int getInitializationTileCount() {
        return initializationTileCount;
    }

    public void completeAO() {
        if(ranAmbientOcclusion) return;
        if(initializationTileCount > 0) return;
        ClientPlayer p = ClientPlayer.getLocalPlayer();
        if(p == null) return;
        int[] vp = p.clientViewport;

        int cy1 =       chunkY + 1;
        int cyN1 =      chunkY - 1;
        boolean y1 =    cy1  >= vp[2] && cy1 <= vp[3];      // one up in bounds?
        boolean yN1 =   cyN1 >= vp[2] && cyN1 <= vp[3];     // one down in bounds?

        int cx1 =       chunkX + 1;
        int cxN1 =      chunkX - 1;
        boolean x1 =    cx1  >= vp[0] && cx1 <= vp[1];      // one right in bounds?
        boolean xN1 =   cxN1 >= vp[0] && cxN1 <= vp[1];     // one left in bounds?

        boolean[] check = new boolean[8];

        if(x1 && y1) {
            // Top Right corner.
            check[0] = true;
            check[1] = true;
            check[2] = true;
        }
        if(xN1 && y1) {
            // Top Left corner.
            check[7] = true;
            check[0] = true;
            check[6] = true;
        }
        if(x1 && yN1) {
            // Bottom Right corner.
            check[2] = true;
            check[3] = true;
            check[4] = true;
        }
        if(xN1 && yN1) {
            // Bottom Left corner.
            check[4] = true;
            check[5] = true;
            check[6] = true;
        }

        if(!ch(0, check, chunkX, cy1)) return;  // N
        if(!ch(1, check, cx1, cy1)) return;     // NE
        if(!ch(2, check, cx1, chunkY)) return;  // E
        if(!ch(3, check, cx1, cyN1)) return;    // SE
        if(!ch(4, check, chunkX, cyN1)) return; // S
        if(!ch(5, check, cxN1, cyN1)) return;   // SW
        if(!ch(6, check, cxN1, chunkY)) return; // W
        if(!ch(7, check, cxN1, cy1)) return;    // NW

        ranAmbientOcclusion = true;
        generateAmbientOcclusion(true);
    }

    private boolean ch(int index, boolean[] check, int x, int y) {
        if(!check[index]) return true;
        ClientChunk n = ClientChunkGrid.get().getChunk(x, y);
        if(n == null) return false;
        return n.ranAmbientOcclusion || n.initializationTileCount == 0;
    }

}