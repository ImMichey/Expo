package dev.michey.expo.logic.entity.arch;

import dev.michey.expo.logic.entity.*;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;

public enum ClientEntityType {

    /** Synced across server + client */
    PLAYER(0, "Player", ServerEntityType.PLAYER),
    DUMMY(1, "Dummy", ServerEntityType.DUMMY),
    GRASS(2, "Grass", ServerEntityType.GRASS),
    OAK_TREE(3, "OakTree", ServerEntityType.OAK_TREE),
    MUSHROOM_RED(4, "MushroomRed", ServerEntityType.MUSHROOM_RED),
    MUSHROOM_BROWN(5, "MushroomBrown", ServerEntityType.MUSHROOM_BROWN),
    BUSH(6, "Bush", ServerEntityType.BUSH),
    ITEM(7, "Item", ServerEntityType.ITEM),

    /** Client only */
    SELECTOR(-1, "Selector", null),
    RAINDROP(-2, "Raindrop", null),
    PARTICLE_HIT(-3, "ParticleHit", null),
    PARTICLE_FOOD(-4, "ParticleFood", null),
    GHOST_ITEM(-5, "GhostItem", null),

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
            case 3 -> new ClientOakTree();
            case 4 -> new ClientMushroomRed();
            case 5 -> new ClientMushroomBrown();
            case 6 -> new ClientBush();
            case 7 -> new ClientItem();
            case 8 -> new ClientGhostItem();
            default -> null;
        };
    }

}
