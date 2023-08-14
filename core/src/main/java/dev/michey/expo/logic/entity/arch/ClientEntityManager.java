package dev.michey.expo.logic.entity.arch;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.logic.entity.misc.ClientDynamic3DTile;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.server.main.logic.world.dimension.EntityOperation;
import dev.michey.expo.server.packet.P29_EntityCreateAdvanced;
import dev.michey.expo.server.packet.P2_EntityCreate;
import dev.michey.expo.server.util.GenerationUtils;
import dev.michey.expo.util.ClientPackets;
import dev.michey.expo.util.EntityRemovalReason;
import dev.michey.expo.util.ExpoShared;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static dev.michey.expo.log.ExpoLogger.log;

public class ClientEntityManager {

    /** Singleton */
    private static ClientEntityManager INSTANCE;

    /** Storage maps */
    private final LinkedList<ClientEntity> depthEntityList;
    private final HashMap<Integer, ClientEntity> idEntityMap;
    private final HashMap<ClientEntityType, LinkedList<ClientEntity>> typeEntityListMap;
    private final ConcurrentLinkedQueue<Integer> removalQueue;
    private final ConcurrentLinkedQueue<ClientEntity> additionQueue;

    /** Client side only entities */
    private int clientEntityId = -1;

    /** Method helper */
    private final List<List<ClientEntity>> listOfEntities;

    /** Selectable entities */
    private final HashMap<ClientEntity, Object[]> selectableEntities;
    public ClientEntity selectedEntity;
    // Rendering helpers
    private int lastEntityId = -1;
    private float pDelta;
    private boolean plus = true;
    public float pulseProgress;

    public ClientEntityManager() {
        depthEntityList = new LinkedList<>();
        idEntityMap = new HashMap<>();
        typeEntityListMap = new HashMap<>();
        removalQueue = new ConcurrentLinkedQueue<>();
        additionQueue = new ConcurrentLinkedQueue<>();

        for(ClientEntityType type : ClientEntityType.values()) {
            typeEntityListMap.put(type, new LinkedList<>());
        }

        listOfEntities = new LinkedList<>();
        selectableEntities = new HashMap<>();

        INSTANCE = this;
    }

    public void tickEntities(float delta) {
        selectableEntities.clear();

        // poll addition
        while(!additionQueue.isEmpty()) {
            ClientEntity toAdd = additionQueue.poll();

            if(idEntityMap.containsKey(toAdd.entityId)) {
                ExpoLogger.log("ILLEGAL ENTITY ID ? " + toAdd.entityId + " creation/now: " + toAdd.entityTimestamp + "/" + System.currentTimeMillis());
            } else {
                depthEntityList.add(toAdd);
                idEntityMap.put(toAdd.entityId, toAdd);
                typeEntityListMap.get(toAdd.getEntityType()).add(toAdd);

                toAdd.onCreation();
            }
        }

        // poll removal
        Iterator<Integer> operationIterator = removalQueue.iterator();

        while(operationIterator.hasNext()) {
            int entityId = operationIterator.next();

            ClientEntity entity = idEntityMap.get(entityId);
            if(entity == null) continue;

            boolean poll = true;

            if(entity.removalFade > 0 && entity.removalReason != EntityRemovalReason.DEATH) {
                entity.removalFade -= delta;
                poll = entity.removalFade <= 0;
            }

            if(poll) {
                operationIterator.remove();

                depthEntityList.remove(entity);
                idEntityMap.remove(entityId);
                typeEntityListMap.get(entity.getEntityType()).remove(entity);

                entity.onDeletion();
            }
        }

        depthEntityList.sort(depthSorter);
        var player = ClientPlayer.getLocalPlayer();
        boolean doCheck = player != null;

        for(ClientEntity entity : depthEntityList) {
            entity.tick(delta);

            if(doCheck) {
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

            for(ClientEntity e : selectableEntities.keySet()) {
                Object[] data = selectableEntities.get(e);
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
    }

    public void updateSelectionShader(ClientEntity entity, RenderContext rc) {
        if(entity.entityId != lastEntityId) {
            lastEntityId = entity.entityId;
            plus = false;
            pDelta = 1.0f;
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

        pulseProgress = Interpolation.smooth2.apply(pDelta);

        if(rc.batch.isDrawing()) rc.batch.end();
        if(rc.arraySpriteBatch.isDrawing()) rc.arraySpriteBatch.end();
    }

    public void renderEntities(float delta) {
        RenderContext rc = RenderContext.get();

        rc.batch.setShader(rc.DEFAULT_GLES3_SHADER);
        rc.arraySpriteBatch.setShader(rc.DEFAULT_GLES3_ARRAY_SHADER);
        rc.batch.begin();

        rc.batch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
        rc.arraySpriteBatch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);

        for(ClientEntity entity : depthEntityList) {
            if(entity.selected) {
                updateSelectionShader(entity, rc);
                ((SelectableEntity) entity).renderSelected(rc, delta);

                rc.useArrayBatch();
                rc.arraySpriteBatch.setShader(rc.DEFAULT_GLES3_ARRAY_SHADER);
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

        rc.arraySpriteBatch.begin();
        rc.arraySpriteBatch.setShader(rc.DEFAULT_GLES3_ARRAY_SHADER);

        rc.arraySpriteBatch.setBlendFunction(GL30.GL_ONE, GL30.GL_ONE);
        Gdx.gl30.glBlendEquation(GL30.GL_MAX);

        for(ClientEntity entity : depthEntityList) {
            entity.renderShadow(rc, delta);
        }

        rc.arraySpriteBatch.setBlendFunction(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl30.glBlendEquation(GL30.GL_FUNC_ADD);

        rc.arraySpriteBatch.end();
        rc.arraySpriteBatch.setShader(null);
    }

    public ClientEntity getEntityById(int entityId) {
        return idEntityMap.get(entityId);
    }

    public List<ClientEntity> getEntitiesByType(ClientEntityType type) {
        return typeEntityListMap.get(type);
    }

    public Set<ClientEntityType> getExistingEntityTypes() {
        return typeEntityListMap.keySet();
    }

    public List<List<ClientEntity>> getEntitiesByType(ClientEntityType... types) {
        listOfEntities.clear();

        for(ClientEntityType type : types) {
            listOfEntities.add(getEntitiesByType(type));
        }

        return listOfEntities;
    }

    public List<List<ClientEntity>> getEntitiesByType(List<ClientEntityType> list) {
        listOfEntities.clear();

        for(ClientEntityType type : list) {
            listOfEntities.add(getEntitiesByType(type));
        }

        return listOfEntities;
    }

    public void addEntity(ClientEntity entity) {
        additionQueue.add(entity);
    }

    public void removeEntity(ClientEntity entity) {
        removeEntity(entity.entityId);
    }

    public void removeEntity(int entityId) {
        removalQueue.add(entityId);
    }

    public Collection<ClientEntity> allEntities() {
        return idEntityMap.values();
    }

    public ClientEntity createFromPacket(P2_EntityCreate p) {
        ClientEntity e = ClientEntityType.typeToClientEntity(p.entityType.ENTITY_ID);
        e.entityId = p.entityId;
        e.serverPosX = p.serverPosX;
        e.serverPosY = p.serverPosY;
        e.clientPosX = p.serverPosX;
        e.clientPosY = p.serverPosY;
        e.entityTimestamp = p.timestamp;
        return e;
    }

    public ClientEntity createFromPacketAdvanced(P29_EntityCreateAdvanced p) {
        ClientEntity e = ClientEntityType.typeToClientEntity(p.entityType.ENTITY_ID);
        e.entityId = p.entityId;
        e.serverPosX = p.serverPosX;
        e.serverPosY = p.serverPosY;
        e.clientPosX = p.serverPosX;
        e.clientPosY = p.serverPosY;
        e.entityTimestamp = p.timestamp;
        e.applyPacketPayload(p.payload);
        return e;
    }

    private int generateClientSideEntityId() {
        int current = clientEntityId;
        clientEntityId--;
        return current;
    }

    public void addClientSideEntity(ClientEntity entity) {
        entity.entityId = generateClientSideEntityId();
        addEntity(entity);
    }

    // A simple depth comparator
    private final Comparator<ClientEntity> depthSorter = ((o1, o2) -> Float.compare(o2.depth, o1.depth));

    public int entityCount() {
        return idEntityMap.size();
    }

    public Object[] isNowSelected(ClientEntity entity) {
        // only check if in view
        if(!entity.visibleToRenderEngine) return null;

        if(entity.getEntityType() == ClientEntityType.DYNAMIC_3D_TILE) {
            ClientDynamic3DTile dyn = (ClientDynamic3DTile) entity;
            int minus = dyn.emulatingType.TILE_ID_DATA[0];
            if(dyn.layerIds.length == 1 && (dyn.layerIds[0] - minus) == 0) return null;

            if(dyn.layerIds.length == 4) {
                for(int id : dyn.layerIds) {
                    if(id >= (minus + 18) && id <= (minus + 21)) {
                        return null;
                    }
                }
            }
        }

        ClientPlayer player = ClientPlayer.getLocalPlayer();
        RenderContext r = RenderContext.get();

        // range
        float px = player.playerReachCenterX;
        float py = player.playerReachCenterY;

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

        if(shortestDistance > player.getPlayerRange()) return null;

        // view angle
        double entityPlayerAngle = GenerationUtils.angleBetween360(player.playerReachCenterX, player.playerReachCenterY, shortestDistanceX, shortestDistanceY);
        if(!ExpoShared.inAngleProximity(r.mousePlayerAngle, entityPlayerAngle, 225)) return null;

        float sx = entity.finalTextureStartX;
        float sy = entity.finalTextureStartY;

        float distanceMouseEntity = Vector2.dst(r.mouseWorldX, r.mouseWorldY, shortestDistanceX, shortestDistanceY);
        boolean directMouseContact = r.mouseWorldX >= sx && r.mouseWorldX <= (sx + entity.textureWidth) && r.mouseWorldY >= sy && r.mouseWorldY < (sy + entity.textureHeight);

        return new Object[] {directMouseContact, distanceMouseEntity};
    }

    public static ClientEntityManager get() {
        return INSTANCE;
    }

}
