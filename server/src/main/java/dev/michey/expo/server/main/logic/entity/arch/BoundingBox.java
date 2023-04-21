package dev.michey.expo.server.main.logic.entity.arch;

import com.dongbat.jbump.CollisionFilter;
import com.dongbat.jbump.Item;
import com.dongbat.jbump.Response;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;

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

    public void teleport(float x, float y) {
        parent.getDimension().getPhysicsWorld().update(physicsBody, x + xOffset, y + yOffset);
    }

    public Response.Result move(float x, float y, CollisionFilter filter) {
        return parent.getDimension().getPhysicsWorld().move(physicsBody, parent.posX + xOffset + x, parent.posY + yOffset + y, filter);
    }

    public Response.Result move(float x, float y) {
        return parent.getDimension().getPhysicsWorld().move(physicsBody, parent.posX + xOffset + x, parent.posY + yOffset + y, CollisionFilter.defaultFilter);
    }

    public static CollisionFilter playerCollisionFilter = (item, other) -> {
        if(item.userData instanceof ServerPlayer && other.userData instanceof ServerPlayer) {
            return Response.cross;
        }

        return Response.slide;
    };

}
