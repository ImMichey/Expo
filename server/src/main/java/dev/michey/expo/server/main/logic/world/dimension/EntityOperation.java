package dev.michey.expo.server.main.logic.world.dimension;

import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;

public class EntityOperation {

    public ServerEntity payload;
    public boolean add;
    public int optionalId;

    public EntityOperation(ServerEntity payload, boolean add) {
        this.payload = payload;
        this.add = add;
    }

    public EntityOperation(int optionalId, boolean add) {
        this.optionalId = optionalId;
        this.add = add;
    }

}
