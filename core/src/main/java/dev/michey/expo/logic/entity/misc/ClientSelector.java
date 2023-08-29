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
import dev.michey.expo.render.ui.SelectorType;
import dev.michey.expo.server.main.logic.inventory.item.*;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.server.main.logic.world.chunk.ServerTile;
import dev.michey.expo.util.ExpoShared;

import static dev.michey.expo.util.ExpoShared.ROW_TILES;

public class ClientSelector extends ClientEntity {

    private TextureRegion square;

    /*
    private float factor = -1.0f;
    private final float MAX_ALPHA = 0.25f;
    private final float ALPHA_SPEED = 2.0f;
    private float alphaDelta;
    private float alpha = MAX_ALPHA;

    private final Color COLOR_CAN_DIG = new Color(0.5f, 1.0f, 0.5f, 1.0f);
    private final Color COLOR_CANT_DIG = new Color(1.0f, 0.0f, 0.0f, 1.0f);
    */

    /** New selector values */
    public SelectorType currentSelectorType = SelectorType.DIG_SHOVEL; // default
    public String currentEntityPlacementTexture;
    public boolean currentlyVisible = false;
    private boolean eligible;

    public float externalPosX, externalPosY;
    public int selectionChunkX, selectionChunkY;
    public int selectionTileX, selectionTileY;
    public int selectionTileArray;

    @Override
    public void onCreation() {
        square = tr("square16x16");
    }

    @Override
    public void onDeletion() {

    }

    public boolean canDoAction() {
        return currentlyVisible && eligible;
    }

    @Override
    public void tick(float delta) {
        eligible = false;
        if(!currentlyVisible) return;

        selectionChunkX = ExpoShared.posToChunk(externalPosX);
        selectionChunkY = ExpoShared.posToChunk(externalPosY);
        var chunk = chunkGrid().getChunk(selectionChunkX, selectionChunkY);
        if(chunk == null) return;

        selectionTileX = ExpoShared.posToTile(externalPosX);
        selectionTileY = ExpoShared.posToTile(externalPosY);

        int stx = ExpoShared.posToTile(ExpoShared.chunkToPos(selectionChunkX));
        int sty = ExpoShared.posToTile(ExpoShared.chunkToPos(selectionChunkY));

        int rtx = selectionTileX - stx;
        int rty = selectionTileY - sty;
        selectionTileArray = rty * ROW_TILES + rtx;

        ClientPlayer p = ClientPlayer.getLocalPlayer();
        ItemMapping mapping = (p.holdingItemId == -1 ? null : ItemMapper.get().getMapping(p.holdingItemId));

        TileLayerType t0 = chunk.dynamicTiles[selectionTileArray][0].emulatingType;
        TileLayerType t1 = chunk.dynamicTiles[selectionTileArray][1].emulatingType;
        TileLayerType t2 = chunk.dynamicTiles[selectionTileArray][2].emulatingType;
        boolean layer0Soil = t0 == TileLayerType.SOIL;
        boolean layer0Farmland = t0 == TileLayerType.SOIL_FARMLAND;
        boolean layer0Hole = t0 == TileLayerType.SOIL_HOLE;
        boolean layer1Grass = t1 == TileLayerType.FOREST;
        boolean layer1Sand = t1 == TileLayerType.SAND;
        boolean layer1Empty = t1 == TileLayerType.EMPTY;
        boolean layer2Empty = t2 == TileLayerType.EMPTY;
        boolean layer1Wall = t1.TILE_IS_WALL;

        if(currentSelectorType == SelectorType.DIG_SHOVEL) {
            if(layer1Grass || layer1Sand) {
                eligible = true;
            } else if(layer1Empty && layer0Soil) {
                eligible = true;
            }
        } else if(currentSelectorType == SelectorType.DIG_SCYTHE) {
            if(layer1Empty && layer0Soil) {
                eligible = true;
            }
        } else if(currentSelectorType == SelectorType.PLACE_ENTITY) {
            if(mapping != null) {
                PlaceData placeData = mapping.logic.placeData;

                if(placeData != null) {
                    if(placeData.alignment == PlaceAlignment.TILE) {
                        if(placeData.floorRequirement != null) {
                            if(t0 == placeData.floorRequirement || t1 == placeData.floorRequirement) {
                                eligible = true;
                            }
                        } else {
                            eligible = true;
                        }
                    } else if(placeData.alignment == PlaceAlignment.UNRESTRICTED) {
                        eligible = true;
                    }
                }
            }
        } else if(currentSelectorType == SelectorType.PLACE_TILE) {
            if(mapping != null) {
                PlaceData placeData = mapping.logic.placeData;

                if(placeData != null) {
                    if(placeData.type == PlaceType.FLOOR_0) {
                        if(layer0Farmland || layer0Hole) {
                            eligible = true;
                        }
                    } else if(placeData.type == PlaceType.FLOOR_1) {
                        if(layer0Soil && layer1Empty && layer2Empty) {
                            eligible = true;
                        }
                    } else if(placeData.type == PlaceType.FLOOR_2) {
                        if(layer2Empty && !layer1Wall) {
                            eligible = true;
                        }
                    }
                }
            }
        }
            /*
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
            */
    }

    @Override
    public void render(RenderContext rc, float delta) {
        if(currentlyVisible) {
            depth = externalPosY;
            rc.useArrayBatch();
            rc.useRegularArrayShader();

            if(eligible) {
                rc.arraySpriteBatch.setColor(0.25f, 1.0f, 0.25f, 0.5f);
            } else {
                rc.arraySpriteBatch.setColor(1.0f, 0.25f, 0.25f, 0.5f);
            }
            if(currentEntityPlacementTexture == null) {
                rc.arraySpriteBatch.draw(square, externalPosX, externalPosY);
            } else {
                rc.arraySpriteBatch.draw(tr(currentEntityPlacementTexture), externalPosX, externalPosY);
            }

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
