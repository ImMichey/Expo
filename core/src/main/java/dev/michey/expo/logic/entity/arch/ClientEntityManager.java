package dev.michey.expo.logic.entity.arch;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.logic.entity.ClientPlayer;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.packet.P2_EntityCreate;
import dev.michey.expo.server.util.GenerationUtils;
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

            depthEntityList.add(toAdd);
            idEntityMap.put(toAdd.entityId, toAdd);
            typeEntityListMap.get(toAdd.getEntityType()).add(toAdd);

            toAdd.onCreation();
        }

        // poll removal
        while(!removalQueue.isEmpty()) {
            int entityId = removalQueue.poll();

            ClientEntity entity = idEntityMap.get(entityId);
            if(entity == null) continue;

            depthEntityList.remove(entity);
            idEntityMap.remove(entityId);
            typeEntityListMap.get(entity.getEntityType()).remove(entity);

            entity.onDeletion();
        }

        depthEntityList.sort(depthSorter);

        for(ClientEntity entity : depthEntityList) {
            entity.tick(delta);

            if(entity instanceof SelectableEntity) {
                Object[] data = isNowSelected(entity);

                if(data != null) {
                    selectableEntities.put(entity, data);
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

            for(ClientEntity e : selectableEntities.keySet()) {
                Object[] data = selectableEntities.get(e);
                boolean direct = (boolean) data[0];

                if(direct) {
                    if(directContactEntity == null) {
                        directContactEntity = e;
                    } else {
                        if(e.depth < directContactEntity.depth) {
                            directContactEntity = e;
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
    }

    private void updateSelectionShader(ClientEntity entity, RenderContext rc) {
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

        rc.currentBatch.end();

        rc.selectionShader.bind();
        rc.selectionShader.setUniformf("u_progress", pulseProgress);
        rc.selectionShader.setUniformf("u_pulseStrength", 1.25f);
        rc.selectionShader.setUniformf("u_pulseMin", 1.05f);

        rc.currentBatch = rc.batch;
        rc.currentBatch.begin();
        rc.currentBatch.setShader(rc.selectionShader);
    }

    public void renderEntities(float delta) {
        RenderContext rc = RenderContext.get();

        rc.forceBatchAndShader(rc.arraySpriteBatch, rc.DEFAULT_GLES3_ARRAY_SHADER);

        for(ClientEntity entity : depthEntityList) {
            if(entity.selected) {
                updateSelectionShader(entity, rc);
                ((SelectableEntity) entity).renderSelected(rc, delta);
                rc.useBatchAndShader(rc.arraySpriteBatch, rc.DEFAULT_GLES3_ARRAY_SHADER);
            } else {
                entity.render(rc, delta);
            }
        }

        rc.cleanUp();
    }

    public void renderEntityShadows(float delta) {
        RenderContext rc = RenderContext.get();

        rc.forceBatchAndShader(rc.arraySpriteBatch, rc.DEFAULT_GLES3_SHADER);

        rc.arraySpriteBatch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE);
        Gdx.gl30.glBlendEquation(GL30.GL_MAX);

        for(ClientEntity entity : depthEntityList) {
            entity.renderShadow(rc, delta);
        }

        rc.cleanUp();

        Gdx.gl30.glBlendEquation(GL20.GL_FUNC_ADD);
        rc.arraySpriteBatch.setBlendFunction(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);
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
    private final Comparator<ClientEntity> depthSorter = ((o1, o2) -> {
        if(o1.depth > o2.depth) {
            return -1;
        } else if(o1.depth == o2.depth) {
            return 0;
        }

        return 1;
    });

    public int entityCount() {
        return idEntityMap.size();
    }

    public Object[] isNowSelected(ClientEntity entity) {
        // only check if in view
        if(!entity.drawnLastFrame) return null;

        ClientPlayer player = ClientPlayer.getLocalPlayer();
        RenderContext r = RenderContext.get();

        // range
        float px = player.playerReachCenterX;
        float py = player.playerReachCenterY;
        float distancePlayerEntity = Vector2.dst(px, py, entity.drawCenterX, entity.drawCenterY);

        if(distancePlayerEntity > player.getPlayerRange()) return null;

        // view angle
        double entityPlayerAngle = GenerationUtils.angleBetween360(player.playerReachCenterX, player.playerReachCenterY, entity.drawCenterX, entity.drawCenterY);
        if(!ExpoShared.inAngleProximity(r.mousePlayerAngle, entityPlayerAngle, 225)) return null;

        float sx = entity.clientPosX + entity.drawOffsetX;
        float sy = entity.clientPosY + entity.drawOffsetY;

        float distanceMouseEntity = Vector2.dst(r.mouseWorldX, r.mouseWorldY, entity.drawCenterX, entity.drawCenterY);
        boolean directMouseContact = r.mouseWorldX >= sx && r.mouseWorldX <= (sx + entity.drawWidth) && r.mouseWorldY >= sy && r.mouseWorldY < (sy + entity.drawHeight);

        return new Object[] {directMouseContact, distanceMouseEntity};
    }

    public static ClientEntityManager get() {
        return INSTANCE;
    }

}
