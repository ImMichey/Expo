package dev.michey.expo.server.main.logic.entity.misc;

import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;

public class ServerBeehive extends ServerEntity {

    public float hiveOffsetX;
    public float hiveOffsetY;

    public ServerBeehive() {

    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.BEEHIVE;
    }

}