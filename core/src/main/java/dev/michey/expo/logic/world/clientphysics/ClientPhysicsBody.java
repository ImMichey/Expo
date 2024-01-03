package dev.michey.expo.logic.world.clientphysics;

import com.dongbat.jbump.CollisionFilter;
import com.dongbat.jbump.Item;
import com.dongbat.jbump.Response;
import com.dongbat.jbump.World;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.misc.ClientPickupLine;
import dev.michey.expo.server.main.logic.world.bbox.BBox;

public class ClientPhysicsBody extends BBox {

    private final ClientEntity parent;
    private final World<ClientEntity> world;
    private final Item<ClientEntity> body;

    public static final Object uglyClientSideLock = new Object();

    public ClientPhysicsBody(ClientEntity parent, float xOffset, float yOffset, float width, float height) {
        super(xOffset, yOffset, width, height);
        this.parent = parent;
        this.world = ExpoClientContainer.get().getClientWorld().getClientPhysicsWorld();

        synchronized (uglyClientSideLock) {
            body = world.add(new Item<>(parent), parent.clientPosX + xOffset, parent.clientPosY + yOffset, width, height);
        }
    }

    public void dispose() {
        synchronized (uglyClientSideLock) {
            world.remove(body);
        }
    }

    public void teleport(float x, float y) {
        synchronized (uglyClientSideLock) {
            world.update(body, x + xOffset, y + yOffset);
        }
    }

    public Response.Result move(float x, float y, CollisionFilter filter) {
        synchronized (uglyClientSideLock) {
            return world.move(body, parent.clientPosX + xOffset + x, parent.clientPosY + yOffset + y, filter);
        }
    }

    public Response.Result move(float x, float y) {
        synchronized (uglyClientSideLock) {
            return world.move(body, parent.clientPosX + xOffset + x, parent.clientPosY + yOffset + y, CollisionFilter.defaultFilter);
        }
    }

    public Response.Result moveAbsolute(float x, float y, CollisionFilter filter) {
        synchronized (uglyClientSideLock) {
            return world.move(body, xOffset + x, yOffset + y, filter);
        }
    }

    public static CollisionFilter pickupCollisionFilter = (item, other) -> {
        if(other.userData instanceof ClientPickupLine) {
            return Response.slide;
        }

        return Response.cross;
    };

}
