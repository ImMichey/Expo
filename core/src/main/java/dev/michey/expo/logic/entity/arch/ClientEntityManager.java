package dev.michey.expo.logic.entity.arch;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.packet.P2_EntityCreate;

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

        INSTANCE = this;
    }

    public void tickEntities(float delta) {
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
        }
    }

    public void renderEntities(float delta) {
        RenderContext rc = RenderContext.get();

        rc.forceBatchAndShader(rc.arraySpriteBatch, rc.DEFAULT_GLES3_SHADER);

        for(ClientEntity entity : depthEntityList) {
            entity.render(rc, delta);
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

    public static ClientEntityManager get() {
        return INSTANCE;
    }

}
