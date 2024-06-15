package dev.michey.expo.logic.entity.misc;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityType;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.noise.TileLayerType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.ui.SelectorType;
import dev.michey.expo.render.visbility.TopVisibilityEntity;
import dev.michey.expo.server.main.logic.inventory.item.FloorType;
import dev.michey.expo.server.main.logic.inventory.item.PlaceAlignment;
import dev.michey.expo.server.main.logic.inventory.item.PlaceData;
import dev.michey.expo.server.main.logic.inventory.item.PlaceType;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.server.util.ServerUtils;
import dev.michey.expo.util.ExpoShared;

import static dev.michey.expo.util.ExpoShared.ROW_TILES;
import static dev.michey.expo.util.ExpoShared.TILE_SIZE;

public class ClientSelector extends ClientEntity implements TopVisibilityEntity {

    private TextureRegion selector;
    private float lastTileWorldX, lastTileWorldY;
    private float fluidTransitionDelta;
    private float fluidTransitionOriginX, fluidTransitionOriginY;
    private boolean fluidTransition;
    private boolean wasHidden;

    /** New selector values */
    public SelectorType currentSelectorType = SelectorType.DIG_SHOVEL; // default
    public String currentEntityPlacementTexture;
    public float entityPlacementOffset;
    public boolean currentlyVisible = false;
    private boolean eligible;
    public boolean blockSelection = false;
    public boolean playPulseAnimation;
    public float pulseAnimationDelta;
    public float pulseValue;

    // Externally set by ClientPlayer
    public int tileGridX, tileGridY;    // Tile position [0, 1, 2, 3, etc.]
    public float worldX, worldY;          // Mouse world position
    public boolean useTileCheck;
    public boolean useFreeCheck;
    public float drawPosX, drawPosY;
    public boolean reset0;

    // Calculated in tick0()
    public int toChunkX, toChunkY;
    public int toTileArray;
    public String text;

    /** Thrown entity curve */
    public Vector2[] thrownEntityCurve;
    public boolean invalidCurve = true;

    @Override
    public void onCreation() {
        selector = tr("selector");
    }

    @Override
    public void onDeletion() {

    }

    public boolean canDoAction() {
        return currentlyVisible && eligible;
    }

    private int getTileArray() {
        int stx = ExpoShared.posToTile(ExpoShared.chunkToPos(toChunkX));
        int sty = ExpoShared.posToTile(ExpoShared.chunkToPos(toChunkY));
        int rtx = tileGridX - stx;
        int rty = tileGridY - sty;
        return rty * ROW_TILES + rtx;
    }

    public void tick0() {
        eligible = false;
        useTileCheck = false;
        useFreeCheck = false;

        if(!currentlyVisible) {
            wasHidden = true;
            return;
        }

        ClientPlayer p = ClientPlayer.getLocalPlayer();
        if(p.holdingItemId == -1) return;

        // Get current chunk
        toChunkX = ExpoShared.posToChunk(worldX);
        toChunkY = ExpoShared.posToChunk(worldY);

        // Check if chunk is out of bounds
        var chunk = chunkGrid().getChunk(toChunkX, toChunkY);
        if(chunk == null) return;

        ItemMapping mapping = ItemMapper.get().getMapping(p.holdingItemId);

        if(mapping.logic.placeData != null && mapping.logic.placeData.type == PlaceType.ENTITY && mapping.logic.placeData.alignment == PlaceAlignment.UNRESTRICTED) {
            toTileArray = 0;
        } else {
            toTileArray = getTileArray();
        }

        TileLayerType t0 = chunk.dynamicTiles[toTileArray][0].emulatingType;
        TileLayerType t1 = chunk.dynamicTiles[toTileArray][1].emulatingType;
        TileLayerType t2 = chunk.dynamicTiles[toTileArray][2].emulatingType;
        boolean layer0Soil = t0 == TileLayerType.SOIL;
        boolean layer0Farmland = t0 == TileLayerType.SOIL_FARMLAND;
        boolean layer0Hole = t0 == TileLayerType.SOIL_HOLE;
        boolean layer1Grass = t1 == TileLayerType.FOREST;
        boolean layer1Plains = t1 == TileLayerType.GRASS;
        boolean layer1Sand = t1 == TileLayerType.SAND;
        boolean layer1Empty = t1 == TileLayerType.EMPTY;
        boolean layer2Empty = t2 == TileLayerType.EMPTY;
        boolean layer1Wall = t1.TILE_IS_WALL;
        boolean layer2Wall = t2.TILE_IS_WALL;
        boolean tileEntity = chunk.tileEntityGrid != null && chunk.tileEntityGrid[toTileArray] != -1;
        boolean layer2Water = TileLayerType.isWater(t2);
        boolean layer1Floor = t1 == TileLayerType.OAK_PLANK;

        if(currentSelectorType == SelectorType.DIG_SHOVEL) {
            if(!tileEntity && !layer1Wall && !layer2Wall) {
                if(layer1Grass || layer1Sand || layer1Plains || layer1Floor) {
                    eligible = true;
                    text = "[LMB] Dig";
                } else if(layer1Empty && layer0Soil) {
                    eligible = true;
                    text = "[LMB] Dig";
                }
            }
        } else if(currentSelectorType == SelectorType.DIG_SCYTHE) {
            if(!tileEntity && !layer1Wall && !layer2Wall) {
                if(layer1Empty && layer0Soil) {
                    eligible = true;
                    text = "[LMB] Tile soil";
                }
            }
        } else if(currentSelectorType == SelectorType.PLACE_ENTITY) {
            if(mapping != null) {
                PlaceData placeData = mapping.logic.placeData;

                if(placeData != null) {
                    if(placeData.alignment == PlaceAlignment.TILE) {
                        if(!layer1Wall && !layer2Wall && !tileEntity) {
                            if(placeData.floorRequirement != null) {
                                if(t0 == placeData.floorRequirement || t1 == placeData.floorRequirement || t2 == placeData.floorRequirement) {
                                    eligible = true;
                                    text = "[RMB] Place object";
                                }
                            } else {
                                eligible = true;
                                text = "[RMB] Place object";
                            }
                        }
                    } else if(placeData.alignment == PlaceAlignment.UNRESTRICTED) {
                        eligible = true;
                        text = "[RMB] Place object";
                    }
                }
            }
        } else if(currentSelectorType == SelectorType.PLACE_TILE) {
            if(mapping != null) {
                PlaceData placeData = mapping.logic.placeData;

                if(placeData != null && !tileEntity) {
                    if(placeData.type == PlaceType.FLOOR_0) {
                        if(placeData.floorType == FloorType.DIRT && layer2Water) {
                            eligible = true;
                            text = "[RMB] Fill up with soil";
                        } else if(layer0Farmland || layer0Hole) {
                            eligible = true;
                            text = "[RMB] Fill up";
                        }
                    } else if(placeData.type == PlaceType.FLOOR_1) {
                        if(layer0Soil && layer1Empty && layer2Empty) {
                            eligible = true;
                            text = "[RMB] Place floor";
                        }
                    } else if(placeData.type == PlaceType.FLOOR_2) {
                        if(layer2Empty && !layer1Wall) {
                            eligible = true;
                            text = "[RMB] Place floor";
                        }
                    }
                }
            }
        }
    }

    @Override
    public void tick(float delta) {

    }

    @Override
    public void render(RenderContext rc, float delta) {

    }

    @Override
    public void renderShadow(RenderContext rc, float delta) {

    }

    @Override
    public ClientEntityType getEntityType() {
        return ClientEntityType.SELECTOR;
    }

    @Override
    public void renderTop(RenderContext rc, float delta) {
        ClientPlayer cp = ClientPlayer.getLocalPlayer();

        if(cp.holdingItemId != -1) {
            ItemMapping mapping = ItemMapper.get().getMapping(cp.holdingItemId);

            if(mapping.thrownRender != null) {
                if(invalidCurve) {
                    invalidCurve = false;

                    Vector2 start = new Vector2(cp.playerReachCenterX, cp.playerReachCenterY);
                    Vector2 dst = new Vector2(rc.mouseWorldX, rc.mouseWorldY);
                    float clDst = Vector2.dst(start.x, start.y, dst.x, dst.y);
                    float mtd = mapping.logic.throwData.maxThrowDistance;

                    if(clDst > mtd) {
                        dst = new Vector2(dst).sub(start).nor().scl(mtd).add(start);
                    }

                    thrownEntityCurve = ServerUtils.toSmoothCurve(start, dst, 128, 64);
                }

                rc.arraySpriteBatch.end();
                rc.chunkRenderer.begin(ShapeRenderer.ShapeType.Filled);
                Gdx.gl.glEnable(GL20.GL_BLEND);
                Gdx.gl.glBlendFuncSeparate(
                        GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA,
                        GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA
                );

                for(int i = 0; i < thrownEntityCurve.length - 1; i++) {
                    Vector2 a = thrownEntityCurve[i];
                    Vector2 b = thrownEntityCurve[i + 1];

                    if(i % 10 == 0 || i % 10 == 1) {
                        continue;
                    }

                    float alpha = ((float) i) / thrownEntityCurve.length * 1.25f;

                    rc.chunkRenderer.setColor(1.0f, 1.0f, 1.0f, alpha);
                    rc.chunkRenderer.rectLine(a, b, 1.5f);
                }

                rc.chunkRenderer.setColor(Color.WHITE);
                rc.chunkRenderer.end();
                rc.arraySpriteBatch.begin();
            }
        }

        if(currentlyVisible) {
            depth = worldY;

            boolean changedTile = (lastTileWorldX != drawPosX) || (lastTileWorldY != drawPosY);

            if(reset0) {
                reset0 = false;
                changedTile = true;
                wasHidden = true;
            }

            if(changedTile) {
                fluidTransitionDelta = 0.0f;
                fluidTransition = true;
                if(wasHidden) {
                    fluidTransitionOriginX = drawPosX;
                    fluidTransitionOriginY = drawPosY;
                    wasHidden = false;
                } else {
                    fluidTransitionOriginX = lastTileWorldX;
                    fluidTransitionOriginY = lastTileWorldY;
                }
            }

            float useDrawX = drawPosX;
            float useDrawY = drawPosY;

            if(fluidTransition) {
                float TRANSITION_SPEED = 12.0f;
                fluidTransitionDelta += delta * TRANSITION_SPEED;

                if(fluidTransitionDelta >= 1.0f) {
                    fluidTransitionDelta = 1.0f;
                    fluidTransition = false;
                }

                float interpolated = Interpolation.smooth2.apply(fluidTransitionDelta);

                useDrawX = fluidTransitionOriginX + (drawPosX - fluidTransitionOriginX) * interpolated;
                useDrawY = fluidTransitionOriginY + (drawPosY - fluidTransitionOriginY) * interpolated;
            }

            if(playPulseAnimation) {
                float PULSE_SPEED = 6.0f;
                pulseAnimationDelta += delta * PULSE_SPEED;

                if(pulseAnimationDelta >= 1.0f) {
                    pulseAnimationDelta = 1.0f;
                    playPulseAnimation = false;
                }

                float normalized;

                if(pulseAnimationDelta < 0.5f) {
                    normalized = pulseAnimationDelta * 2;
                } else {
                    normalized = 1f - (pulseAnimationDelta - 0.5f) * 2;
                }

                float PULSE_SCALE = 0.25f;
                pulseValue = Interpolation.pow3.apply(normalized) * PULSE_SCALE;
            } else {
                pulseValue = 0.0f;
                pulseAnimationDelta = 0.0f;
            }

            float seladj = 0;

            if(currentEntityPlacementTexture != null) {
                seladj = tr(currentEntityPlacementTexture).getRegionWidth() * 0.5f;
            }

            float sz = TILE_SIZE + 4 - ((TILE_SIZE + 4) * pulseValue);
            float px = useDrawX - 8 + seladj + (TILE_SIZE - sz) * 0.5f;
            float py = useDrawY + (TILE_SIZE - sz) * 0.5f;

            if(currentEntityPlacementTexture != null) {
                if(eligible) {
                    rc.arraySpriteBatch.setColor(0.1f, 1.0f, 0.1f, 0.75f);
                } else {
                    rc.arraySpriteBatch.setColor(1.0f, 0.2f, 0.2f, 0.75f);
                }

                rc.arraySpriteBatch.setShader(rc.buildPreviewShader);

                TextureRegion tr = tr(currentEntityPlacementTexture);
                rc.arraySpriteBatch.draw(tr, useDrawX, useDrawY);

                rc.arraySpriteBatch.setShader(rc.DEFAULT_GLES3_ARRAY_SHADER);
                rc.arraySpriteBatch.setColor(Color.WHITE);
            }

            rc.arraySpriteBatch.draw(selector, px, py - entityPlacementOffset, sz, sz);

            lastTileWorldX = drawPosX;
            lastTileWorldY = drawPosY;
        }
    }

    public void playPulseAnimation() {
        playPulseAnimation = true;
        pulseAnimationDelta = 0f;
    }

}
