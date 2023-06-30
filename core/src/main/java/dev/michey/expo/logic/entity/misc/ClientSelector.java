package dev.michey.expo.logic.entity.misc;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.noise.TileLayerType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.server.main.logic.inventory.item.ItemMetadata;
import dev.michey.expo.server.main.logic.inventory.item.PlaceData;
import dev.michey.expo.server.main.logic.inventory.item.PlaceType;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.server.main.logic.world.chunk.ServerTile;
import dev.michey.expo.util.ExpoShared;

import static dev.michey.expo.util.ExpoShared.ROW_TILES;

public class ClientSelector extends ClientEntity {

    private TextureRegion square;
    public boolean visible = false;
    public int selectionType; // Dig = 0, Place = 1
    public float tx, ty;
    public int tix, tiy;
    private int lastTix, lastTiy;
    private float factor = -1.0f;
    private final float MAX_ALPHA = 0.25f;
    private final float ALPHA_SPEED = 2.0f;
    private float alphaDelta;
    private float alpha = MAX_ALPHA;
    public boolean canDig = false;
    public boolean canPlace = false;

    public int svChunkX;
    public int svChunkY;
    public int svTileArray;
    public int svTileX;
    public int svTileY;

    private final Color COLOR_CAN_DIG = new Color(0.5f, 1.0f, 0.5f, 1.0f);
    private final Color COLOR_CANT_DIG = new Color(1.0f, 0.0f, 0.0f, 1.0f);

    @Override
    public void onCreation() {
        square = tr("square16x16");
    }

    @Override
    public void onDeletion() {

    }

    @Override
    public void tick(float delta) {
        if(visible) {
            if(lastTix != tix || lastTiy != tiy) {
                alphaDelta = 0;
                alpha = MAX_ALPHA;
            } else {
                alphaDelta += delta * factor * ALPHA_SPEED;
                alpha = Interpolation.fade.apply(1.0f - alphaDelta) * MAX_ALPHA;

                if(alphaDelta < 0) {
                    alphaDelta = 0;
                    factor *= -1;
                } else if(alphaDelta > 1.0f) {
                    alphaDelta = 1.0f;
                    factor *= -1;
                }
            }

            { // canDig check
                svChunkX = ExpoShared.posToChunk(tx + 1);
                svChunkY = ExpoShared.posToChunk(ty + 1);
                canDig = false;
                canPlace = false;

                var chunk = chunkGrid().getChunk(svChunkX, svChunkY);

                if(chunk != null) {
                    float mouseWorldX = tx + 1;
                    float mouseWorldY = ty + 1;
                    svTileX = ExpoShared.posToTile(mouseWorldX);
                    svTileY = ExpoShared.posToTile(mouseWorldY);
                    int mouseChunkX = ExpoShared.posToChunk(mouseWorldX);
                    int mouseChunkY = ExpoShared.posToChunk(mouseWorldY);
                    int startTileX = ExpoShared.posToTile(ExpoShared.chunkToPos(mouseChunkX));
                    int startTileY = ExpoShared.posToTile(ExpoShared.chunkToPos(mouseChunkY));
                    int mouseRelativeTileX = svTileX - startTileX;
                    int mouseRelativeTileY = svTileY - startTileY;
                    svTileArray = mouseRelativeTileY * ROW_TILES + mouseRelativeTileX;

                    TileLayerType t0 = chunk.dynamicTiles[svTileArray][0].emulatingType;
                    TileLayerType t1 = chunk.dynamicTiles[svTileArray][1].emulatingType;

                    ClientPlayer p = ClientPlayer.getLocalPlayer();
                    ItemMapping mapping = null;

                    if(p.holdingItemId != -1) {
                        mapping = ItemMapper.get().getMapping(p.holdingItemId);
                    }

                    if(selectionType == 0) {
                        // Check to dig.
                        boolean grass = t1 == TileLayerType.GRASS || t1 == TileLayerType.FOREST;
                        boolean sand = t1 == TileLayerType.SAND || t1 == TileLayerType.DESERT;
                        boolean soil = t0 == TileLayerType.SOIL;

                        if(mapping != null) {
                            ToolType tt = mapping.logic.toolType;

                            if(tt == ToolType.SCYTHE) {
                                canDig = soil && t1 == TileLayerType.EMPTY;
                            } else {
                                canDig = grass || sand || (soil && t1 == TileLayerType.EMPTY);
                            }
                        } else {
                            canDig = grass || sand || (soil && t1 == TileLayerType.EMPTY);
                        }
                    } else if(selectionType == 1) {
                        // Check to place.
                        if(mapping != null) {
                            PlaceData d = mapping.logic.placeData;

                            if(d != null) {
                                if(d.type == PlaceType.FLOOR_0) {
                                    canPlace = t1 == TileLayerType.EMPTY && (t0 == TileLayerType.SOIL_HOLE || t0 == TileLayerType.SOIL_FARMLAND);
                                } else if(d.type == PlaceType.FLOOR_1) {
                                    canPlace = t1 == TileLayerType.EMPTY && t0 == TileLayerType.SOIL;
                                }
                            }
                        }
                    }
                }
            }

            lastTix = tix;
            lastTiy = tiy;
        }
    }

    @Override
    public void render(RenderContext rc, float delta) {
        if(visible) {
            depth = ty;
            rc.useArrayBatch();
            rc.useRegularArrayShader();

            Color c = (canDig || canPlace) ? COLOR_CAN_DIG : COLOR_CANT_DIG;
            rc.arraySpriteBatch.setColor(c.r, c.g, c.b, alpha);
            rc.arraySpriteBatch.draw(square, tx, ty);
            rc.arraySpriteBatch.setColor(Color.WHITE);
        }
    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {

    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.SELECTOR;
    }

}
