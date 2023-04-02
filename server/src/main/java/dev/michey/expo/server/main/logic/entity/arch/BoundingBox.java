package dev.michey.expo.server.main.logic.entity.arch;

import com.dongbat.jbump.CollisionFilter;
import com.dongbat.jbump.Item;
import com.dongbat.jbump.Response;

public class BoundingBox {

    private final ServerEntity parent;
    public float xOffset;
    public float yOffset;
    private float width;
    private float height;

    // generated
    private final Item<ServerEntity> physicsBody;

    public BoundingBox(ServerEntity parent, float xOffset, float yOffset, float width, float height) {
        this.parent = parent;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.width = width;
        this.height = height;
        physicsBody = parent.getDimension().getPhysicsWorld().add(new Item<>(parent), parent.posX + xOffset, parent.posY + yOffset, width, height);
    }

    public void dispose() {
        parent.getDimension().getPhysicsWorld().remove(physicsBody);
    }

    public Response.Result move(float x, float y) {
        return parent.getDimension().getPhysicsWorld().move(physicsBody, parent.posX + xOffset + x, parent.posY + yOffset + y, CollisionFilter.defaultFilter);
    }

}