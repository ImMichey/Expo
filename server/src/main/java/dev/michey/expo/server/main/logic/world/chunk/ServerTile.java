package dev.michey.expo.server.main.logic.world.chunk;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.noise.TileLayerType;
import dev.michey.expo.server.main.logic.entity.animal.ServerWorm;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.entity.misc.ServerDynamic3DTile;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;
import dev.michey.expo.server.main.logic.inventory.item.ServerInventoryItem;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.dimension.ServerDimension;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.util.Pair;

import java.util.Arrays;
import java.util.HashSet;

import static dev.michey.expo.util.ExpoShared.ROW_TILES;

public class ServerTile {

    public ServerChunk chunk;
    public int tileX; // Absolute tile X
    public int tileY; // Absolute tile Y
    public int tileArray;

    public BiomeType biome;
    public DynamicTilePart[] dynamicTileParts;

    public ServerTile(ServerChunk chunk, int tileX, int tileY, int tileArray) {
        this.chunk = chunk;
        this.tileX = tileX;
        this.tileY = tileY;
        this.tileArray = tileArray;
        biome = BiomeType.VOID;
    }

    public static final int NORTH = 1;
    public static final int EAST = 2;
    public static final int SOUTH = 4;
    public static final int WEST = 8;

    public static final int NORTH_EAST = 1;
    public static final int SOUTH_EAST = 2;
    public static final int SOUTH_WEST = 4;
    public static final int NORTH_WEST = 8;

    public void updateLayer(int layer, TileLayerType type) {
        if(layer == 0) {
            updateLayer0(type);
        } else if(layer == 1) {
            updateLayer1(type);
        } else {
            updateLayer2(type);
        }
    }

    private void setTileWall(TileLayerType use, int layer) {
        int x = tileArray % ROW_TILES;
        int y = tileArray / ROW_TILES;

        ServerDynamic3DTile entity = new ServerDynamic3DTile();
        entity.posX = ExpoShared.tileToPos(tileX);
        entity.posY = ExpoShared.tileToPos(tileY);
        entity.setStaticEntity();
        entity.layerIds = dynamicTileParts[layer].layerIds;
        entity.emulatingType = use;

        ServerWorld.get().registerServerEntity(chunk.getDimension().getDimensionName(), entity);
        entity.attachToTile(chunk, x, y);

        dynamicTileParts[layer].setTileIds(new int[] {-1});
    }

    public void updateLayer0(TileLayerType type) {
        TileLayerType use = type == null ? TileLayerType.biomeToLayer0(biome) : type;
        dynamicTileParts[0].update(use);
        int[] td = dynamicTileParts[0].emulatingType.TILE_ID_DATA;

        if(td.length == 1) {
            dynamicTileParts[0].setTileIds(new int[] {td[0]});
        } else {
            dynamicTileParts[0].setTileIds(runTextureGrab(td[0], 0));
        }

        if(use.TILE_IS_WALL) {
            setTileWall(use, 0);
        }
    }

    public void updateLayer1(TileLayerType type) {
        TileLayerType use = type == null ? TileLayerType.biomeToLayer1(biome) : type;
        dynamicTileParts[1].update(use);
        int[] td = dynamicTileParts[1].emulatingType.TILE_ID_DATA;

        if(td.length == 1) {
            dynamicTileParts[1].setTileIds(new int[] {td[0]});
        } else {
            dynamicTileParts[1].setTileIds(runTextureGrab(td[0], 1));
        }

        if(use.TILE_IS_WALL) {
            setTileWall(use, 1);
        }
    }

    public void updateLayer2(TileLayerType type) {
        TileLayerType use = type == null ? TileLayerType.biomeToLayer2(biome) : type;
        dynamicTileParts[2].update(use);
        int[] td = dynamicTileParts[2].emulatingType.TILE_ID_DATA;

        if(td.length == 1) {
            dynamicTileParts[2].setTileIds(new int[] {td[0]});
        } else {
            dynamicTileParts[2].setTileIds(runTextureGrab(td[0], 2));
        }

        if(use.TILE_IS_WALL) {
            setTileWall(use, 2);
        }
    }

    public boolean updateLayerAdjacent(int layer, boolean updateDynamicTilesFlag) {
        if(layer == 0) {
            return updateLayer0Adjacent(updateDynamicTilesFlag);
        } else if(layer == 1) {
            return updateLayer1Adjacent(updateDynamicTilesFlag);
        }

        return updateLayer2Adjacent(updateDynamicTilesFlag);
    }

    /** This method is called when an adjacent layer 0 tile has been updated and this tile potentially needs to adjust its texture. */
    public boolean updateLayer0Adjacent(boolean updateDynamicTilesFlag) {
        if(dynamicTileParts == null) return false;
        if(dynamicTileParts[0].emulatingType == null) return false;
        int[] td = dynamicTileParts[0].emulatingType.TILE_ID_DATA;
        int[] old = dynamicTileParts[0].layerIds;

        if(!dynamicTileParts[0].emulatingType.TILE_IS_WALL) {
            if(td.length == 1) {
                dynamicTileParts[0].setTileIds(new int[] {td[0]});
            } else {
                dynamicTileParts[0].setTileIds(runTextureGrab(td[0], 0));
            }
        }

        if(updateDynamicTilesFlag) {
            updateDynamic3DTile(0);
        }

        return !Arrays.equals(old, dynamicTileParts[0].layerIds);
    }

    /** This method is called when an adjacent layer 1 tile has been updated and this tile potentially needs to adjust its texture.
     * Returns whether an update packet is needed or not. */
    public boolean updateLayer1Adjacent(boolean updateDynamicTilesFlag) {
        if(dynamicTileParts == null) return false;
        if(dynamicTileParts[1].emulatingType == null) return false;
        int[] td = dynamicTileParts[1].emulatingType.TILE_ID_DATA;
        int[] old = dynamicTileParts[1].layerIds;

        if(!dynamicTileParts[1].emulatingType.TILE_IS_WALL) {
            if(td.length == 1) {
                dynamicTileParts[1].setTileIds(new int[] {td[0]});
            } else {
                dynamicTileParts[1].setTileIds(runTextureGrab(td[0], 1));
            }
        }

        if(updateDynamicTilesFlag) {
            updateDynamic3DTile(1);
        }

        return !Arrays.equals(old, dynamicTileParts[1].layerIds);
    }

    /** This method is called when an adjacent layer 2 tile has been updated and this tile potentially needs to adjust its texture. */
    public boolean updateLayer2Adjacent(boolean updateDynamicTilesFlag) {
        if(dynamicTileParts == null) return false;
        if(dynamicTileParts[2].emulatingType == null) return false;
        int[] td = dynamicTileParts[2].emulatingType.TILE_ID_DATA;
        int[] old = dynamicTileParts[2].layerIds;

        if(!dynamicTileParts[2].emulatingType.TILE_IS_WALL) {
            if(td.length == 1) {
                dynamicTileParts[2].setTileIds(new int[] {td[0]});
            } else {
                dynamicTileParts[2].setTileIds(runTextureGrab(td[0], 2));
            }
        }

        if(updateDynamicTilesFlag) {
            updateDynamic3DTile(2);
        }

        return !Arrays.equals(old, dynamicTileParts[2].layerIds);
    }

    private void updateDynamic3DTile(int layer) {
        ServerDynamic3DTile entity = getDynamic3DTile();

        if(entity != null) {
            entity.layerIds = runTextureGrab(entity.emulatingType.TILE_ID_DATA[0], layer);
            entity.checkForBoundingBox();
            ServerPackets.p30EntityDataUpdate(entity.entityId, new Object[] {entity.layerIds, entity.emulatingType.SERIALIZATION_ID}, PacketReceiver.whoCanSee(entity));
        }
    }

    public static int[] runTextureGrab(int minTile, int[] indices) {
        int tis = indices[0];
        int tid = indices[1];

        if(tis == 0 && tid == 0) {
            // Special case, no neighbour
            return new int[] {minTile + 1};
        } else if(tis == 15 && tid == 15) {
            // Special case, every 8 neighbours are same tile
            return new int[] {minTile};
        } else if(tis == 15) {
            // N E S W all valid neighbours (straight)
            if(tid == 0) {
                // No diagonal neighbours
                return new int[] {minTile + 20, minTile + 21, minTile + 18, minTile + 19};
            } else {
                // tig between [1-14]
                boolean northWest = tid / NORTH_WEST == 1;
                boolean southWest = (tid % NORTH_WEST) / SOUTH_WEST == 1;
                boolean southEast = (tid % NORTH_WEST % SOUTH_WEST) / SOUTH_EAST == 1;
                boolean northEast = (tid % NORTH_WEST % SOUTH_WEST % SOUTH_EAST) / NORTH_EAST == 1;

                return new int[] {
                        southWest ? minTile + 8 : minTile + 20,
                        southEast ? minTile + 5 : minTile + 21,
                        northWest ? minTile + 14 : minTile + 18,
                        northEast ? minTile + 11 : minTile + 19
                };
            }
        } else {
            // N E S W not all valid neighbours
            boolean west = tis / WEST == 1;
            boolean south = (tis % WEST) / SOUTH == 1;
            boolean east = (tis % WEST % SOUTH) / EAST == 1;
            boolean north = (tis % WEST % SOUTH % EAST) / NORTH == 1;

            boolean northWest = tid / NORTH_WEST == 1;
            boolean southWest = (tid % NORTH_WEST) / SOUTH_WEST == 1;
            boolean southEast = (tid % NORTH_WEST % SOUTH_WEST) / SOUTH_EAST == 1;
            boolean northEast = (tid % NORTH_WEST % SOUTH_WEST % SOUTH_EAST) / NORTH_EAST == 1;

            int c1, c2, c3, c4;

            { // Corner Bottom Left
                if(west && south && southWest) {
                    c1 = minTile + 8;
                } else if(west && south) {
                    c1 = minTile + 20;
                } else if(west && southWest) {
                    c1 = minTile + 16;
                } else if(south && southWest) {
                    c1 = minTile + 4;
                } else if(west) {
                    c1 = minTile + 16;
                } else if(south) {
                    c1 = minTile + 4;
                } else {
                    c1 = minTile + 12;
                }
            }

            { // Corner Bottom Right
                if(east && south && southEast) {
                    c2 = minTile + 5;
                } else if(east && south) {
                    c2 = minTile + 21;
                } else if(east && southEast) {
                    c2 = minTile + 13;
                } else if(south && southEast) {
                    c2 = minTile + 9;
                } else if(east) {
                    c2 = minTile + 13;
                } else if(south) {
                    c2 = minTile + 9;
                } else {
                    c2 = minTile + 17;
                }
            }

            { // Corner Top Left
                if(west && north && northWest) {
                    c3 = minTile + 14;
                } else if(west && north) {
                    c3 = minTile + 18;
                } else if(west && northWest) {
                    c3 = minTile + 6;
                } else if(north && northWest) {
                    c3 = minTile + 10;
                } else if(west) {
                    c3 = minTile + 6;
                } else if(north) {
                    c3 = minTile + 10;
                } else {
                    c3 = minTile + 2;
                }
            }

            { // Corner Top Right
                if(east && north && northEast) {
                    c4 = minTile + 11;
                } else if(east && north) {
                    c4 = minTile + 19;
                } else if(east && northEast) {
                    c4 = minTile + 3;
                } else if(north && northEast) {
                    c4 = minTile + 15;
                } else if(east) {
                    c4 = minTile + 3;
                } else if(north) {
                    c4 = minTile + 15;
                } else {
                    c4 = minTile + 7;
                }
            }

            return new int[] {c1, c2, c3, c4};
        }
    }

    private int[] runTextureGrab(int minTile, int layer, TileLayerType type) {
        return runTextureGrab(minTile, indexStraightDiagonal(layer, tileX, tileY, type));
    }

    public int[] runTextureGrab(int minTile, int layer) {
        return runTextureGrab(minTile, layer, dynamicTileParts[layer].emulatingType);
    }

    private int[] indexStraightDiagonal(int checkLayer, int x, int y, TileLayerType b) {
        ServerChunkGrid g = chunk.getDimension().getChunkHandler();

        TileLayerType n = g.getTileLayerType(x, y + 1, checkLayer);
        TileLayerType e = g.getTileLayerType(x + 1, y, checkLayer);
        TileLayerType s = g.getTileLayerType(x, y - 1, checkLayer);
        TileLayerType w = g.getTileLayerType(x - 1, y, checkLayer);
        TileLayerType ne = g.getTileLayerType(x + 1, y + 1, checkLayer);
        TileLayerType se = g.getTileLayerType(x + 1, y - 1, checkLayer);
        TileLayerType sw = g.getTileLayerType(x - 1, y - 1, checkLayer);
        TileLayerType nw = g.getTileLayerType(x - 1, y + 1, checkLayer);

        int tis = 0, tid = 0;

        if(TileLayerType.isConnected(n, b)) tis += NORTH;
        if(TileLayerType.isConnected(e, b)) tis += EAST;
        if(TileLayerType.isConnected(s, b)) tis += SOUTH;
        if(TileLayerType.isConnected(w, b)) tis += WEST;
        if(TileLayerType.isConnected(ne, b)) tid += NORTH_EAST;
        if(TileLayerType.isConnected(se, b)) tid += SOUTH_EAST;
        if(TileLayerType.isConnected(sw, b)) tid += SOUTH_WEST;
        if(TileLayerType.isConnected(nw, b)) tid += NORTH_WEST;

        return new int[] {tis, tid};
    }

    private Pair<int[], TileLayerType[]> indexStraightDiagonalPrecise(int checkLayer, int x, int y, TileLayerType b) {
        ServerChunkGrid g = chunk.getDimension().getChunkHandler();

        TileLayerType n = g.getTileLayerType(x, y + 1, checkLayer);
        TileLayerType e = g.getTileLayerType(x + 1, y, checkLayer);
        TileLayerType s = g.getTileLayerType(x, y - 1, checkLayer);
        TileLayerType w = g.getTileLayerType(x - 1, y, checkLayer);
        TileLayerType ne = g.getTileLayerType(x + 1, y + 1, checkLayer);
        TileLayerType se = g.getTileLayerType(x + 1, y - 1, checkLayer);
        TileLayerType sw = g.getTileLayerType(x - 1, y - 1, checkLayer);
        TileLayerType nw = g.getTileLayerType(x - 1, y + 1, checkLayer);

        int tis = 0, tid = 0;

        if(n == b) tis += NORTH;
        if(e == b) tis += EAST;
        if(s == b) tis += SOUTH;
        if(w == b) tis += WEST;
        if(ne == b) tid += NORTH_EAST;
        if(se == b) tid += SOUTH_EAST;
        if(sw == b) tid += SOUTH_WEST;
        if(nw == b) tid += NORTH_WEST;

        return new Pair<>(new int[] {tis, tid}, new TileLayerType[] {n, e, s, w, ne, se, sw, nw});
    }

    private int[] indexStraightDiagonalX(int checkLayer, int x, int y, TileLayerType b) {
        ServerChunkGrid g = chunk.getDimension().getChunkHandler();

        TileLayerType n = g.getTileLayerType(x, y + 1, checkLayer);
        TileLayerType e = g.getTileLayerType(x + 1, y, checkLayer);
        TileLayerType s = g.getTileLayerType(x, y - 1, checkLayer);
        TileLayerType w = g.getTileLayerType(x - 1, y, checkLayer);
        TileLayerType ne = g.getTileLayerType(x + 1, y + 1, checkLayer);
        TileLayerType se = g.getTileLayerType(x + 1, y - 1, checkLayer);
        TileLayerType sw = g.getTileLayerType(x - 1, y - 1, checkLayer);
        TileLayerType nw = g.getTileLayerType(x - 1, y + 1, checkLayer);

        int tis = 0, tid = 0;

        if(n == b) tis += NORTH;
        if(e == b) tis += EAST;
        if(s == b) tis += SOUTH;
        if(w == b) tis += WEST;
        if(ne == b) tid += NORTH_EAST;
        if(se == b) tid += SOUTH_EAST;
        if(sw == b) tid += SOUTH_WEST;
        if(nw == b) tid += NORTH_WEST;

        return new int[] {tis, tid};
    }

    private boolean dig(int layer, float damage) {
        return dynamicTileParts[layer].hit(damage);
    }

    public boolean performDigOperation(float damage, ServerInventoryItem item, boolean sendUpdatePacket, boolean recursiveLayerDamage, float damageSourceX, float damageSourceY) {
        int digLayer = selectDigLayer();
        if(digLayer == -1) return false;

        int pColor = toParticleColorId();
        boolean dugUp = dig(digLayer, damage);
        float afterDamageHealth = Math.abs(dynamicTileParts[digLayer].tileHealth);
        boolean applyRecursive = false;

        { // Play dig up sound.
            String sound = TileLayerType.typeToHitSound(dynamicTileParts[digLayer].emulatingType);
            playTileSound(sound);
        }

        ServerDimension dim = chunk.getDimension();

        if(dugUp) {
            {
                if(digLayer == 0 && (biome == BiomeType.PLAINS || biome == BiomeType.FOREST || biome == BiomeType.DENSE_FOREST)) {
                    if(MathUtils.random() <= 0.1f) {
                        ServerWorm worm = new ServerWorm();
                        worm.posX = ExpoShared.tileToPos(tileX) + 8f;
                        worm.posY = ExpoShared.tileToPos(tileY) + 4f;
                        ServerWorld.get().registerServerEntity(dim.getDimensionName(), worm);
                        ServerPackets.p24PositionalSound("pop", worm.posX, worm.posY, ExpoShared.PLAYER_AUDIO_RANGE, PacketReceiver.whoCanSee(dim, chunk.chunkX, chunk.chunkY));
                    }
                } /*else if(digLayer == 1 && dynamicTileParts[digLayer].emulatingType == TileLayerType.SAND && biome == BiomeType.BEACH) {
                    if(MathUtils.random() <= 0.01f) {
                        ServerChest chest = new ServerChest();
                        chest.chestInventory.fillRandom();
                        chest.posX = ExpoShared.tileToPos(tileX);
                        chest.posY = ExpoShared.tileToPos(tileY);
                        chest.setStaticEntity();
                        ServerWorld.get().registerServerEntity(dim.getDimensionName(), chest);
                        ServerPackets.p24PositionalSound("pop", chest.posX, chest.posY, ExpoShared.PLAYER_AUDIO_RANGE, PacketReceiver.whoCanSee(dim, chunk.chunkX, chunk.chunkY));
                    }
                }
                */

                // Drop layer as item.
                String identifier = TileLayerType.typeToItemDrop(dynamicTileParts[digLayer].emulatingType);

                if(identifier != null) {
                    float bx = ExpoShared.tileToPos(tileX) + 8.0f + MathUtils.random(3.0f);
                    float by = ExpoShared.tileToPos(tileY) + 8.0f + MathUtils.random(3.0f);

                    float dstX;
                    float dstY;

                    if(damageSourceX != 0 || damageSourceY != 0) {
                        Vector2 temp = new Vector2(bx, by).sub(damageSourceX, damageSourceY).nor().scl(64);
                        dstX = temp.x;
                        dstY = temp.y;
                    } else {
                        dstX = 0;
                        dstY = 0;
                    }

                    chunk.getDimension().getEntityManager().spawnItemSingleDst(bx, by, identifier, dstX, dstY);
                }
            }

            HashSet<String> affectedChunks = new HashSet<>();
            affectedChunks.add(chunk.getChunkKey());

            { // Update tile data
                if(digLayer == 0 && dynamicTileParts[digLayer].emulatingType == TileLayerType.SOIL) {
                    if(item != null && item.itemMetadata.toolType == ToolType.SCYTHE) {
                        updateLayer0(TileLayerType.SOIL_FARMLAND);
                    } else {
                        updateLayer0(TileLayerType.SOIL_HOLE);
                        chunk.checkWaterSpread = true;
                    }

                    if(sendUpdatePacket) {
                        ServerPackets.p32ChunkDataSingle(this, 0);
                    }

                    for(ServerTile neighbour : getNeighbouringTiles()) {
                        if(neighbour == null) continue;
                        if(neighbour.updateLayer0Adjacent(false)) {
                            if(sendUpdatePacket) {
                                ServerPackets.p32ChunkDataSingle(neighbour, 0);
                            }
                            affectedChunks.add(neighbour.chunk.getChunkKey());
                        }
                    }
                } else if(digLayer == 1) {
                    updateLayer1(TileLayerType.EMPTY);

                    if(sendUpdatePacket) {
                        ServerPackets.p32ChunkDataSingle(this, 1);
                    }

                    for(ServerTile neighbour : getNeighbouringTiles()) {
                        if(neighbour == null) continue;
                        if(neighbour.updateLayer1Adjacent(false)) {
                            if(sendUpdatePacket) {
                                ServerPackets.p32ChunkDataSingle(neighbour, 1);
                            }
                            affectedChunks.add(neighbour.chunk.getChunkKey());
                        }
                    }

                    if(recursiveLayerDamage) {
                        applyRecursive = true;
                    }
                }

            }

            long now = System.currentTimeMillis();

            for(String affectedChunkKey : affectedChunks) {
                // Update tile timestamp
                ServerChunk sv = dim.getChunkHandler().getActiveChunk(affectedChunkKey);
                if(sv == null) continue;
                sv.lastTileUpdate = now;

                if(sendUpdatePacket) {
                    for(ServerPlayer player : dim.getEntityManager().getAllPlayers()) {
                        if(player.canSeeChunk(affectedChunkKey)) {
                            player.hasSeenChunks.put(sv, now);
                        }
                    }
                }
            }
        }

        if(applyRecursive) {
            performDigOperation(afterDamageHealth, item, sendUpdatePacket, false, damageSourceX, damageSourceY);
        }

        if(item != null) {
            ServerPackets.p33TileDig(tileX, tileY, pColor, PacketReceiver.whoCanSee(dim, chunk.chunkX, chunk.chunkY));
        }

        return true;
    }

    public int selectDigLayer() {
        TileLayerType t2 = dynamicTileParts[2].emulatingType;
        if(TileLayerType.isWater(t2)) return -1;

        TileLayerType t1 = dynamicTileParts[1].emulatingType;
        if(t1 == TileLayerType.GRASS || t1 == TileLayerType.FOREST || t1 == TileLayerType.OAK_PLANK) return 1;
        if(t1 == TileLayerType.SAND) return 1;

        TileLayerType t0 = dynamicTileParts[0].emulatingType;
        if(t0 == TileLayerType.SOIL) return 0;

        return -1;
    }

    public void playTileSound(String sound) {
        if(sound != null) {
            ServerPackets.p24PositionalSound(sound,
                    ExpoShared.tileToPos(tileX) + 8f, ExpoShared.tileToPos(tileY) + 8f,
                    ExpoShared.PLAYER_AUDIO_RANGE, PacketReceiver.whoCanSee(chunk.getDimension(), chunk.chunkX, chunk.chunkY));
        }
    }

    // Bottom Left -> Bottom Right -> Top Right -> Top Left -> Bottom Left
    public ServerTile[] getNeighbouringTiles() {
        ServerTile[] list = new ServerTile[8];
        ServerChunkGrid grid = chunk.getDimension().getChunkHandler();

        list[0] = grid.getTile(tileX - 1, tileY - 1);
        list[1] = grid.getTile(tileX, tileY - 1);
        list[2] = grid.getTile(tileX + 1, tileY - 1);
        list[3] = grid.getTile(tileX + 1, tileY);
        list[4] = grid.getTile(tileX + 1, tileY + 1);
        list[5] = grid.getTile(tileX, tileY + 1);
        list[6] = grid.getTile(tileX - 1, tileY + 1);
        list[7] = grid.getTile(tileX - 1, tileY);

        return list;
    }

    // North -> East -> South -> West (4 tiles)
    public ServerTile[] getNeighbouringTilesNESW() {
        ServerTile[] list = new ServerTile[4];
        ServerChunkGrid grid = chunk.getDimension().getChunkHandler();

        list[0] = grid.getTile(tileX, tileY + 1);
        list[1] = grid.getTile(tileX + 1, tileY);
        list[2] = grid.getTile(tileX, tileY - 1);
        list[3] = grid.getTile(tileX - 1, tileY);

        return list;
    }

    public BiomeType[] getNeighbouringBiomes() {
        BiomeType[] list = new BiomeType[8];
        ServerChunkGrid grid = chunk.getDimension().getChunkHandler();

        list[0] = grid.getBiome(tileX - 1, tileY - 1);
        list[1] = grid.getBiome(tileX, tileY - 1);
        list[2] = grid.getBiome(tileX + 1, tileY - 1);
        list[3] = grid.getBiome(tileX + 1, tileY);
        list[4] = grid.getBiome(tileX + 1, tileY + 1);
        list[5] = grid.getBiome(tileX, tileY + 1);
        list[6] = grid.getBiome(tileX - 1, tileY + 1);
        list[7] = grid.getBiome(tileX - 1, tileY);

        return list;
    }

    @Override
    public String toString() {
        return "ServerTile{" +
                "chunkX=" + chunk.chunkX +
                ", chunkY=" + chunk.chunkY +
                ", tileX=" + tileX +
                ", tileY=" + tileY +
                ", tileArray=" + tileArray +
                ", biome=" + biome +
                '}';
    }

    public int toParticleColorId() {
        if(isType(TileLayerType.GRASS, 1) || isType(TileLayerType.FOREST, 1)) return 1;
        if(isType(TileLayerType.SAND, 1) || isType(TileLayerType.DESERT, 1)) return 2;
        if(isType(TileLayerType.SOIL, 0)) return 0;
        return -1;
    }

    public ServerDynamic3DTile getDynamic3DTile() {
        if(!chunk.hasTileBasedEntities()) return null;
        int entityId = chunk.getTileBasedEntityIdGrid()[tileArray];
        if(entityId == -1) return null;
        ServerEntity found = ServerWorld.get().getDimension(chunk.getDimension().getDimensionName()).getEntityManager().getEntityById(entityId);
        if(found == null) return null;
        if(found.getEntityType() == ServerEntityType.DYNAMIC_3D_TILE) {
            return (ServerDynamic3DTile) found;
        }
        return null;
    }

    public ServerEntity hasTileBasedEntity(ServerEntityType type) {
        if(!chunk.hasTileBasedEntities()) return null;
        if(chunk.getTileBasedEntityIdGrid() == null) return null;
        int entityId = chunk.getTileBasedEntityIdGrid()[tileArray];
        if(entityId == -1) return null;
        ServerEntity found = ServerWorld.get().getDimension(chunk.getDimension().getDimensionName()).getEntityManager().getEntityById(entityId);
        if(found == null) return null;
        if(found.getEntityType() == type) return found;
        return null;
    }

    public boolean hasTileBasedEntityB(ServerEntityType... type) {
        if(!chunk.hasTileBasedEntities()) return false;
        int entityId = chunk.getTileBasedEntityIdGrid()[tileArray];
        if(entityId == -1) return false;
        ServerEntity found = ServerWorld.get().getDimension(chunk.getDimension().getDimensionName()).getEntityManager().getEntityById(entityId);
        if(found == null) return false;
        for(ServerEntityType t : type) {
            if(found.getEntityType() == t) return true;
        }
        return false;
    }

    public boolean isType(TileLayerType type, int layer) {
        return dynamicTileParts[layer].emulatingType == type;
    }

}
