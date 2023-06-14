package dev.michey.expo.server.main.logic.world.bbox;

import com.dongbat.jbump.CollisionFilter;
import com.dongbat.jbump.Item;
import com.dongbat.jbump.Response;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;

public class EntityPhysicsBox extends BBox {

    private final ServerEntity parent;
    private final Item<ServerEntity> physicsBody;

    public EntityPhysicsBox(ServerEntity parent, float xOffset, float yOffset, float width, float height) {
        super(xOffset, yOffset, width, height);
        this.parent = parent;
        physicsBody = parent.getDimension().getPhysicsWorld().add(new Item<>(parent), parent.posX + xOffset, parent.posY + yOffset, width, height);
    }

    public void dispose() {
        parent.getDimension().getPhysicsWorld().remove(physicsBody);
    }

    public void teleport(float x, float y) {
        parent.getDimension().getPhysicsWorld().update(physicsBody, x + xOffset, y + yOffset);
    }

    public Response.Result move(float x, float y, CollisionFilter filter) {
        return parent.getDimension().getPhysicsWorld().move(physicsBody, parent.posX + xOffset + x, parent.posY + yOffset + y, filter);
    }

    public Response.Result move(float x, float y) {
        return parent.getDimension().getPhysicsWorld().move(physicsBody, parent.posX + xOffset + x, parent.posY + yOffset + y, CollisionFilter.defaultFilter);
    }

    public Response.Result moveAbsolute(float x, float y, CollisionFilter filter) {
        return parent.getDimension().getPhysicsWorld().move(physicsBody, xOffset + x, yOffset + y, filter);
    }

}