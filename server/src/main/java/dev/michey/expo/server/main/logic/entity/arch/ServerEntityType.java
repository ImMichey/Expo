package dev.michey.expo.server.main.logic.entity.arch;

import dev.michey.expo.server.main.logic.entity.ServerDummy;
import dev.michey.expo.server.main.logic.entity.ServerGrass;
import dev.michey.expo.server.main.logic.entity.ServerPlayer;

public enum ServerEntityType {

    PLAYER(0, "Player"),
    DUMMY(1, "Dummy"),
    GRASS(2, "Grass")
    ;

    public final int ENTITY_ID;
    public final String ENTITY_NAME;

    ServerEntityType(int ENTITY_ID, String ENTITY_NAME) {
        this.ENTITY_ID = ENTITY_ID;
        this.ENTITY_NAME = ENTITY_NAME;
    }

    public static ServerEntity typeToEntity(int id) {
        return switch (id) {
            case 0 -> new ServerPlayer();
            case 1 -> new ServerDummy();
            case 2 -> new ServerGrass();
            default -> null;
        };
    }

}