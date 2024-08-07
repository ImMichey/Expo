package dev.michey.expo.logic.entity.arch;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.logic.entity.flora.ClientOakTree;
import dev.michey.expo.logic.entity.misc.ClientDynamic3DTile;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.logic.world.chunk.ClientChunk;
import dev.michey.expo.logic.world.chunk.ClientChunkGrid;
import dev.michey.expo.noise.TileLayerType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.reflections.ReflectableEntity;
import dev.michey.expo.render.shadow.AmbientOcclusionEntity;
import dev.michey.expo.render.visbility.TopVisibilityEntity;
import dev.michey.expo.server.main.logic.inventory.item.ToolType;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.server.packet.P29_EntityCreateAdvanced;
import dev.michey.expo.server.packet.P2_EntityCreate;
import dev.michey.expo.server.util.ServerUtils;
import dev.michey.expo.util.*;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientEntityManager {

    /** Singleton */
    private static ClientEntityManager INSTANCE;

    /** Storage maps */
    private final ArrayList<ClientEntity> depthEntityList;
    private final TreeMap<Integer, ClientEntity> idEntityMap;
    private final HashMap<ClientEntityType, ArrayList<ClientEntity>> typeEntityListMap;
    private final ConcurrentLinkedQueue<Pair<Integer, EntityRemovalReason>> removalQueue;
    private final ConcurrentLinkedQueue<ClientEntity> additionQueueSv;
    private final ConcurrentLinkedQueue<ClientEntity> additionQueueCl;
    private final HashSet<Pair<ClientChunk, Integer>> ambientOcclusionUpdateSet;
    private final LinkedList<ClientChunk> greenlitAmbientOcclusionList;

    /** Client side only entities */
    private int clientEntityId = -1;

    /** Method helper */
    public final List<List<ClientEntity>> listOfEntities;
    public final List<ClientEntity> listOfEntitiesSorted;

    /** Selectable entities */
    private final HashMap<ClientEntity, Object[]> selectableEntities;
    public ClientEntity selectedEntity;

    /** Rendering helpers */
    private int lastEntityId = -1;
    private float pDelta;
    private float tDelta;
    private boolean plus = true;
    private boolean plusT = true;
    public float pulseProgress;
    public float pulseThickness;

    /** Reflection */
    public boolean recalculateReflections;

    public ClientEntityManager() {
        depthEntityList = new ArrayList<>(4096);
        idEntityMap = new TreeMap<>();
        typeEntityListMap = new HashMap<>();
        removalQueue = new ConcurrentLinkedQueue<>();
        additionQueueSv = new ConcurrentLinkedQueue<>();
        additionQueueCl = new ConcurrentLinkedQueue<>();
        ambientOcclusionUpdateSet = new HashSet<>();
        greenlitAmbientOcclusionList = new LinkedList<>();

        for(ClientEntityType type : ClientEntityType.values()) {
            typeEntityListMap.put(type, new ArrayList<>(128));
        }

        listOfEntities = new LinkedList<>();
        listOfEntitiesSorted = new LinkedList<>();
        selectableEntities = new HashMap<>();

        INSTANCE = this;
    }

    public void tickEntities(float delta) {
        long a = System.nanoTime();
        selectableEntities.clear();
        ambientOcclusionUpdateSet.clear();
        greenlitAmbientOcclusionList.clear();

        long b = System.nanoTime();

        if(!additionQueueCl.isEmpty()) {
            Iterator<ClientEntity> additionIterator = additionQueueCl.iterator();

            while(additionIterator.hasNext()) {
                ClientEntity toAdd = additionIterator.next();

                int chunkX = ExpoShared.posToChunk(toAdd.clientPosX);
                int chunkY = ExpoShared.posToChunk(toAdd.clientPosY);
                ClientChunk chunk = ClientChunkGrid.get().getChunk(chunkX, chunkY);

                if(chunk != null) {
                    depthEntityList.add(toAdd);
                    idEntityMap.put(toAdd.entityId, toAdd);
                    typeEntityListMap.get(toAdd.getEntityType()).add(toAdd);
                    toAdd.onCreation();

                    if(toAdd instanceof ReflectableEntity) {
                        toAdd.calculateReflection();
                    }

                    additionIterator.remove();
                }
            }
        }

        long c = System.nanoTime();

        int CALC_CAP_ADD = (int) Math.clamp((additionQueueSv.size() * 0.25f), 25, 250);
        int CALC_CAP_REM = (int) Math.clamp((removalQueue.size() * 0.25f), 25, 400);        // Removal cap is higher because of client-side entities (such as particles).

        int serverEntityCapAddition = CALC_CAP_ADD, serverEntityCapRemoval = CALC_CAP_REM;

        if(!additionQueueSv.isEmpty()) {
            Iterator<ClientEntity> additionIterator = additionQueueSv.iterator();

            while(additionIterator.hasNext()) {
                ClientEntity toAdd = additionIterator.next();

                if(!idEntityMap.containsKey(toAdd.entityId)) { // Might cause some bugs in the future
                    serverEntityCapAddition--;

                    int chunkX = ExpoShared.posToChunk(toAdd.serverPosX);
                    int chunkY = ExpoShared.posToChunk(toAdd.serverPosY);
                    ClientChunk chunk = ClientChunkGrid.get().getChunk(chunkX, chunkY);

                    if(chunk != null) {
                        depthEntityList.add(toAdd);
                        idEntityMap.put(toAdd.entityId, toAdd);
                        typeEntityListMap.get(toAdd.getEntityType()).add(toAdd);

                        if(toAdd.tileEntityTileArray != -1) {
                            int newAmount = chunk.attachTileEntity(toAdd.entityId, toAdd.tileEntityTileArray);

                            if(!chunk.ranAmbientOcclusion) {
                                if(newAmount == chunk.getInitializationTileCount()) {
                                    greenlitAmbientOcclusionList.add(chunk);
                                    chunk.ranAmbientOcclusion = true;
                                }
                            } else {
                                ambientOcclusionUpdateSet.add(new Pair<>(chunk, toAdd.tileEntityTileArray));
                            }
                        }

                        toAdd.onCreation();

                        if(toAdd instanceof ReflectableEntity) {
                            toAdd.calculateReflection();
                        }

                        additionIterator.remove();
                    }
                } else {
                    // ExpoLogger.log("Entity addition clash: " + toAdd.getEntityType() + "/" + toAdd.entityId);
                }

                if(serverEntityCapAddition == 0) {
                    break;
                }
            }
        }

        long d = System.nanoTime();

        // poll removal
        if(!removalQueue.isEmpty()) {
            Iterator<Pair<Integer, EntityRemovalReason>> operationIterator = removalQueue.iterator();

            while(operationIterator.hasNext()) {
                Pair<Integer, EntityRemovalReason> pair = operationIterator.next();
                ClientEntity entity = idEntityMap.get(pair.key);

                if(entity == null) {
                    // ExpoLogger.log("Entity removal clash: " + pair.key + " (not existing)");
                    if(pair.key < 0) {
                        operationIterator.remove();
                    }
                    continue;
                } else {
                    entity.removalReason = pair.value;
                }

                if(entity.getEntityType().ENTITY_ID >= 0) {
                    serverEntityCapRemoval--;
                }

                boolean poll = true;

                if(entity.removalFade > 0 && !entity.removalReason.isKillReason() && entity.removalReason != EntityRemovalReason.CAUGHT) {
                    entity.removalFade -= delta;
                    poll = entity.removalFade <= 0;
                }

                if(poll) {
                    operationIterator.remove();

                    depthEntityList.remove(entity);
                    idEntityMap.remove(entity.entityId);
                    typeEntityListMap.get(entity.getEntityType()).remove(entity);

                    if(entity.entityId >= 0 && entity.tileEntityTileArray != -1) {
                        int chunkX = ExpoShared.posToChunk(entity.serverPosX);
                        int chunkY = ExpoShared.posToChunk(entity.serverPosY);
                        ClientChunk chunk = ClientChunkGrid.get().getChunk(chunkX, chunkY);
                        chunk.detachTileEntity(entity.tileEntityTileArray);

                        if(chunk.visibleLogic/* && (chunk.ranAmbientOcclusion || chunk.getInitializationTileCount() == 0)*/) { // This fixes the explosion AO
                            ambientOcclusionUpdateSet.add(new Pair<>(chunk, entity.tileEntityTileArray));
                        }
                    }

                    entity.onDeletion();
                }

                if(serverEntityCapRemoval == 0) {
                    break;
                }
            }
        }

        long ee = System.nanoTime();

        for(ClientChunk chunk : greenlitAmbientOcclusionList) {
            chunk.generateAmbientOcclusion(false);
        }

        if(!ambientOcclusionUpdateSet.isEmpty()) {
            for(var pair : ambientOcclusionUpdateSet) {
                pair.key.updateAmbientOcclusion(pair.value, true, false);
            }
        }

        for(ClientChunk chunk : ClientChunkGrid.get().getAllChunks()) {
            chunk.completeAO();
        }

        long f = System.nanoTime();

        depthEntityList.sort(depthSorter);
        long g = System.nanoTime();
        var player = ClientPlayer.getLocalPlayer();

        for(ClientEntity entity : depthEntityList) {
            entity.tick(delta);
        }

        if(player != null && player.selector != null) {
            player.selector.tick0();

            for(ClientEntity entity : depthEntityList) {
                if(entity instanceof SelectableEntity) {
                    Object[] data = isNowSelected(entity);

                    if(data != null) {
                        selectableEntities.put(entity, data);
                    }
                }
            }
        }

        if(selectedEntity != null) {
            selectedEntity.selected = false;
            selectedEntity = null;
        }

        if(!selectableEntities.isEmpty()) {
            ClientEntity directContactEntity = null;
            ClientEntity mouseProximityEntity = null;
            float lowestProximityDistance = Float.MAX_VALUE;
            float lowestDirectDistance = Float.MAX_VALUE;

            for(Map.Entry<ClientEntity, Object[]> entrySet : selectableEntities.entrySet()) {
                ClientEntity e = entrySet.getKey();
                Object[] data = entrySet.getValue();
                boolean direct = (boolean) data[0];

                if(direct) {
                    float dis = (float) data[1];

                    if(directContactEntity == null) {
                        directContactEntity = e;
                        lowestDirectDistance = dis;
                    } else {
                        if(dis < lowestDirectDistance) {
                            directContactEntity = e;
                            lowestDirectDistance = dis;
                        }
                    }
                } else {
                    if(directContactEntity == null) {
                        float dis = (float) data[1];

                        if(mouseProximityEntity == null) {
                            mouseProximityEntity = e;
                            lowestProximityDistance = dis;
                        } else {
                            if(dis < lowestProximityDistance) {
                                mouseProximityEntity = e;
                                lowestProximityDistance = dis;
                            }
                        }
                    }
                }
            }

            if(directContactEntity != null) {
                selectedEntity = directContactEntity;
            } else if(mouseProximityEntity != null) {
                selectedEntity = mouseProximityEntity;
            }

            if(selectedEntity != null) {
                selectedEntity.selected = true;
            }
        }

        if(selectedEntity != null) {
            if(selectedEntity.entityId != lastEntityId) ClientPackets.p27PlayerEntitySelection(selectedEntity.entityId);
        } else {
            if(lastEntityId != -1) {
                lastEntityId = -1;
                ClientPackets.p27PlayerEntitySelection(-1);
            }
        }

        if(recalculateReflections) {
            recalculateReflections = false;

            for(ClientEntity ent : depthEntityList) {
                if(ent.drawReflection) continue; // Little cheat optimization

                if(ent instanceof ReflectableEntity) {
                    ent.calculateReflection();
                }
            }
        }

        long h = System.nanoTime();
        if(ExpoShared.TRACK_PERFORMANCE) {
            ServerUtils.recordPerformanceMetric("entManager", new String[] {"cl", "addCl", "addSv", "rem", "ao", "sort", "tick"}, new long[] {a, b, c, d, ee, f, g, h},
                    Gdx.graphics.getFramesPerSecond());
        }
    }

    public void updateSelectionShader(ClientEntity entity, RenderContext rc) {
        if(entity.entityId != lastEntityId) {
            lastEntityId = entity.entityId;
            plus = false;
            plusT = false;
            pDelta = 1.0f;
            tDelta = 1.0f;
        }

        float speed = 3.0f;

        if(plus) {
            pDelta += rc.delta * speed;

            if(pDelta >= 1f) {
                pDelta = 1f;
                plus = false;
            }
        } else {
            pDelta -= rc.delta * speed;

            if(pDelta <= 0f) {
                pDelta = 0f;
                plus = true;
            }
        }

        if(plusT) {
            tDelta += rc.delta * 6.0f;

            if(tDelta >= 1f) {
                tDelta = 1f;
                plusT = false;
            }
        } else {
            tDelta -= rc.delta * 6.0f;

            if(tDelta <= 0f) {
                tDelta = 0f;
                plusT = true;
            }
        }

        pulseProgress = Interpolation.smooth2.apply(pDelta);
        pulseThickness = 0.5f;

        if(rc.batch.isDrawing()) rc.batch.end();
        if(rc.arraySpriteBatch.isDrawing()) rc.arraySpriteBatch.end();
    }

    public void renderTopEntities(List<ClientEntity> list) {
        RenderContext rc = RenderContext.get();

        rc.arraySpriteBatch.setShader(rc.DEFAULT_GLES3_ARRAY_SHADER);
        rc.arraySpriteBatch.begin();
        rc.arraySpriteBatch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);

        for(ClientEntity ce : list) {
            TopVisibilityEntity tve = (TopVisibilityEntity) ce;
            tve.renderTop(rc, rc.delta);
        }

        rc.arraySpriteBatch.end();
    }

    @SuppressWarnings("GDXJavaFlushInsideLoop")
    public void renderEntities(float delta) {
        RenderContext rc = RenderContext.get();

        rc.batch.setShader(rc.DEFAULT_GLES3_SHADER);
        rc.arraySpriteBatch.setShader(rc.DEFAULT_GLES3_ARRAY_SHADER);

        rc.arraySpriteBatch.begin();

        rc.batch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
        rc.arraySpriteBatch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);

        for(ClientEntity entity : depthEntityList) {
            if(entity.selected) {
                updateSelectionShader(entity, rc);
                ((SelectableEntity) entity).renderSelected(rc, delta);
                rc.defaultArrayBatch();
            } else {
                entity.render(rc, delta);
            }
        }

        if(rc.batch.isDrawing()) rc.batch.end();
        if(rc.arraySpriteBatch.isDrawing()) rc.arraySpriteBatch.end();
        rc.batch.setShader(null);
        rc.arraySpriteBatch.setShader(null);
    }

    public void renderEntityShadows(float delta) {
        RenderContext rc = RenderContext.get();

        rc.arraySpriteBatch.setShader(rc.shadowPass1Shader);
        rc.arraySpriteBatch.begin();

        rc.arraySpriteBatch.setBlendFunction(GL30.GL_ONE, GL30.GL_ONE);
        Gdx.gl30.glBlendEquation(GL30.GL_MAX);

        long a = System.nanoTime();

        for(Map.Entry<ClientEntityType, ArrayList<ClientEntity>> entrySet : typeEntityListMap.entrySet()) {
            if(!entrySet.getKey().ENTITY_SHADOWS) continue;
            if(entrySet.getValue().isEmpty()) continue;

            for(ClientEntity ent : entrySet.getValue()) {
                ent.renderShadow(rc, delta);
            }
        }

        long b = System.nanoTime();
        if(ExpoShared.TRACK_PERFORMANCE) {
            ServerUtils.recordPerformanceMetric("rsh", new String[] {"total"}, new long[] {a, b});
        }

        rc.arraySpriteBatch.setBlendFunction(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl30.glBlendEquation(GL30.GL_FUNC_ADD);

        rc.arraySpriteBatch.end();
        rc.arraySpriteBatch.setShader(null);

        // .........................

        if(GameSettings.get().ambientOcclusion) {
            rc.aoBatch.setShader(rc.aoShader);
            rc.aoBatch.begin();

            rc.aoBatch.setBlendFunction(GL30.GL_ONE, GL30.GL_ONE);
            Gdx.gl30.glBlendEquation(GL30.GL_MAX);

            for(ClientEntity entity : depthEntityList) {
                if(entity instanceof AmbientOcclusionEntity ao && entity.visibleToRenderEngine) {
                    ao.renderAO(rc);
                }
            }

            rc.aoBatch.setBlendFunction(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);
            Gdx.gl30.glBlendEquation(GL30.GL_FUNC_ADD);

            rc.aoBatch.end();
        }
    }

    public ClientEntity getEntityById(int entityId) {
        return idEntityMap.get(entityId);
    }

    public ConcurrentLinkedQueue<ClientEntity> getEntitiesInAdditionQueue() {
        return additionQueueSv;
    }

    public ConcurrentLinkedQueue<ClientEntity> getEntityClientAdditionQueue() {
        return additionQueueCl;
    }

    public ArrayList<ClientEntity> getEntitiesByType(ClientEntityType type) {
        return typeEntityListMap.get(type);
    }

    public Set<ClientEntityType> getExistingEntityTypes() {
        return typeEntityListMap.keySet();
    }

    public List<List<ClientEntity>> getEntitiesByType(ClientEntityType... types) {
        for(ClientEntityType type : types) {
            listOfEntities.add(getEntitiesByType(type));
        }

        return listOfEntities;
    }

    public List<ClientEntity> getEntitiesByTypeSorted(ClientEntityType... types) {
        for(ClientEntityType type : types) {
            listOfEntitiesSorted.addAll(getEntitiesByType(type));
        }

        listOfEntitiesSorted.sort(depthSorter);
        return listOfEntitiesSorted;
    }

    public List<List<ClientEntity>> getEntitiesByType(List<ClientEntityType> list) {
        for(ClientEntityType type : list) {
            listOfEntities.add(getEntitiesByType(type));
        }

        return listOfEntities;
    }

    public void addEntity(ClientEntity entity) {
        additionQueueSv.add(entity);
    }

    public void removeEntity(ClientEntity entity) {
        removeEntity(entity.entityId, EntityRemovalReason.UNSPECIFIED);
    }

    public void removeEntity(int entityId, EntityRemovalReason reason) {
        removalQueue.add(new Pair<>(entityId, reason));
    }

    public Collection<ClientEntity> allEntities() {
        return idEntityMap.values();
    }

    public ClientEntity createFromPacket(P2_EntityCreate p) {
        ClientEntity e = ClientEntityType.typeToClientEntity(p.entityType.ENTITY_ID);
        e.entityId = p.entityId;
        e.tileEntityTileArray = p.tileArray;
        e.serverPosX = p.serverPosX;
        e.serverPosY = p.serverPosY;
        e.clientPosX = p.serverPosX;
        e.clientPosY = p.serverPosY;
        e.serverHealth = p.entityHealth;
        return e;
    }

    public ClientEntity createFromPacketAdvanced(P29_EntityCreateAdvanced p) {
        ClientEntity e = ClientEntityType.typeToClientEntity(p.entityType.ENTITY_ID);
        e.entityId = p.entityId;
        e.tileEntityTileArray = p.tileArray;
        e.serverPosX = p.serverPosX;
        e.serverPosY = p.serverPosY;
        e.clientPosX = p.serverPosX;
        e.clientPosY = p.serverPosY;
        e.serverHealth = p.entityHealth;
        e.applyCreationPayload(p.payload);
        return e;
    }

    private int generateClientSideEntityId() {
        int current = clientEntityId;
        clientEntityId--;
        return current;
    }

    public void addClientSideEntity(ClientEntity entity) {
        entity.entityId = generateClientSideEntityId();
        additionQueueCl.add(entity);
    }

    // A simple depth comparator
    public static final Comparator<ClientEntity> depthSorter = ((o1, o2) -> Float.compare(o2.depth, o1.depth));

    public int entityCount() {
        return idEntityMap.size();
    }

    public Object[] isNowSelected(ClientEntity entity) {
        // only check if in view
        if(!entity.visibleToRenderEngine) return null;
        ClientPlayer p = ClientPlayer.getLocalPlayer();

        if(entity.getEntityType() == ClientEntityType.DYNAMIC_3D_TILE) {
            ClientDynamic3DTile dyn = (ClientDynamic3DTile) entity;
            int minus = dyn.emulatingType.TILE_ID_DATA[0];
            if(dyn.layerIds.length == 1 && (dyn.layerIds[0] - minus) == 0) return null;

            ItemMapping mapping = p.holdingItemId != -1 ? (ItemMapper.get().getMapping(p.holdingItemId)) : null;

            if(dyn.emulatingType == TileLayerType.DIRT) {
                // Check for shovel.
                if(mapping == null || !mapping.logic.isTool(ToolType.SHOVEL)) {
                    return null;
                }
            } else if(dyn.emulatingType == TileLayerType.ROCK) {
                // Check for pickaxe.
                if(mapping == null || !mapping.logic.isTool(ToolType.PICKAXE)) {
                    return null;
                }
            } else if(dyn.emulatingType == TileLayerType.OAKPLANKWALL) {
                // Check for axe.
                if(mapping == null || !mapping.logic.isTool(ToolType.AXE)) {
                    return null;
                }
            }

            ClientEntity[] entities = dyn.getNeighbouringTileEntitiesNESW();

            if(entities[0] != null && entities[1] != null && entities[2] != null && entities[3] != null) {
                return null;
            }
        }

        RenderContext r = RenderContext.get();

        // range
        float px = p.playerReachCenterX;
        float py = p.playerReachCenterY;

        float shortestDistance = Float.MAX_VALUE, shortestDistanceX = 0, shortestDistanceY = 0;
        float[] points = ((SelectableEntity) entity).interactionPoints();

        for(int i = 0; i < points.length; i += 2) {
            float distancePlayerEntity = Vector2.dst(px, py, points[i], points[i + 1]);

            if(distancePlayerEntity < shortestDistance) {
                shortestDistance = distancePlayerEntity;
                shortestDistanceX = points[i];
                shortestDistanceY = points[i + 1];
            }
        }

        if(shortestDistance > p.getPlayerRange()) return null;

        // view angle
        //double entityPlayerAngle = GenerationUtils.angleBetween360(p.playerReachCenterX, p.playerReachCenterY, shortestDistanceX, shortestDistanceY);
        //if(!ExpoShared.inAngleProximity(r.mousePlayerAngle, entityPlayerAngle, 225)) return null;

        float sx = entity.finalTextureStartX;
        float sy = entity.finalTextureStartY;

        float distanceMouseEntity = Vector2.dst(r.mouseWorldX, r.mouseWorldY, shortestDistanceX, shortestDistanceY);
        boolean directMouseContact;

        if(entity.getEntityType() == ClientEntityType.OAK_TREE) {
            ClientOakTree cot = (ClientOakTree) entity;
            sx = entity.clientPosX - cot.trunkWidth() * 0.5f;
            directMouseContact = r.mouseWorldX >= sx && r.mouseWorldX <= (sx + cot.trunkWidth()) && r.mouseWorldY >= sy && r.mouseWorldY < (sy + cot.trunkHeight());
        } else {
            directMouseContact = r.mouseWorldX >= sx && r.mouseWorldX <= (sx + entity.textureWidth) && r.mouseWorldY >= sy && r.mouseWorldY < (sy + entity.textureHeight);
        }

        return new Object[] {directMouseContact, distanceMouseEntity};
    }

    public ArrayList<ClientEntity> getDepthEntityList() {
        return depthEntityList;
    }

    public static ClientEntityManager get() {
        return INSTANCE;
    }

}
