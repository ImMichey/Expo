package dev.michey.expo.server.fs.world.entity;

import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.main.logic.world.chunk.ServerChunk;
import org.json.JSONObject;

public class SavableEntity {

    public int entityId;
    public int entityType;
    public float posX;
    public float posY;

    public boolean saveTile;
    public int tileX;
    public int tileY;

    public boolean staticEntity;

    public float health;

    /** Packaged entity */
    public JSONObject packaged;

    public SavableEntity(ServerEntity serverEntity) {
        this.entityId = serverEntity.entityId;
        this.entityType = serverEntity.getEntityType().ENTITY_ID;
        this.posX = serverEntity.posX;
        this.posY = serverEntity.posY;

        saveTile = serverEntity.tileEntity;

        if(saveTile) {
            this.tileX = serverEntity.tileX;
            this.tileY = serverEntity.tileY;
        }

        this.staticEntity = serverEntity.staticPosition;
        this.health = serverEntity.health;
    }

    public SavableEntity pack() {
        packaged = new JSONObject();

        packaged.put("id", entityId);
        packaged.put("type", entityType);
        packaged.put("x", posX);
        packaged.put("y", posY);

        if(saveTile) {
            packaged.put("tx", tileX);
            packaged.put("ty", tileY);
        }

        if(staticEntity) {
            packaged.put("static", true);
        }

        if(health != 0f) {
            packaged.put("hp", health);
        }

        return this;
    }

    public SavableEntity add(String key, Object value) {
        packaged.put(key, value);
        return this;
    }

    public static ServerEntity entityFromSavable(JSONObject object, ServerChunk chunk) {
        ServerEntity entity = ServerEntityType.typeToEntity(object.getInt("type"));
        entity.posX = object.getFloat("x");
        entity.posY = object.getFloat("y");
        entity.entityId = object.getInt("id");
        entity.entityDimension = chunk.getDimension().getDimensionName();

        if(object.has("static")) {
            entity.setStaticEntity();
        }

        if(object.has("tx") && object.has("ty")) {
            entity.attachToTile(chunk, object.getInt("tx"), object.getInt("ty"));
        }

        if(object.has("hp")) {
            entity.health = object.getFloat("hp");
        }

        return entity;
    }

}
