package dev.michey.expo.logic.entity.arch;

import dev.michey.expo.logic.entity.ClientDummy;
import dev.michey.expo.logic.entity.ClientGrass;
import dev.michey.expo.logic.entity.ClientPlayer;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;

public enum ClientEntityType {

    /** Synced across server + client */
    PLAYER(0, "Player", ServerEntityType.PLAYER),
    DUMMY(1, "Dummy", ServerEntityType.DUMMY),
    GRASS(2, "Grass", ServerEntityType.GRASS),

    /** Client only */
    SELECTOR(-1, "Selector", null),
    RAINDROP(-2, "Raindrop", null),

    ;

    public final int ENTITY_ID;
    public final String ENTITY_NAME;
    public final ServerEntityType ENTITY_SERVER_TYPE;

    ClientEntityType(int ENTITY_ID, String ENTITY_NAME, ServerEntityType ENTITY_SERVER_TYPE) {
        this.ENTITY_ID = ENTITY_ID;
        this.ENTITY_NAME = ENTITY_NAME;
        this.ENTITY_SERVER_TYPE = ENTITY_SERVER_TYPE;
    }

    public static ClientEntity typeToClientEntity(int id) {
        return switch (id) {
            case 0 -> new ClientPlayer();
            case 1 -> new ClientDummy();
            case 2 -> new ClientGrass();
            default -> null;
        };
    }

}
