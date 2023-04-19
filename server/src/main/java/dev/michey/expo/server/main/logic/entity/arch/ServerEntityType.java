package dev.michey.expo.server.main.logic.entity.arch;

import dev.michey.expo.server.main.logic.entity.*;

public enum ServerEntityType {

    PLAYER(0, "Player"),
    DUMMY(1, "Dummy"),
    GRASS(2, "Grass"),
    OAK_TREE(3, "OakTree"),
    MUSHROOM_RED(4, "MushroomRed"),
    MUSHROOM_BROWN(5, "MushroomBrown"),
    BUSH(6, "Bush"),
    ITEM(7, "Item"),
    ANCIENT_TREE(8, "AncientTree"),
    GRAVESTONE(9, "Gravestone"),
    DANDELION(10, "Dandelion"),
    POPPY(11, "Poppy"),
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
            case 3 -> new ServerOakTree();
            case 4 -> new ServerMushroomRed();
            case 5 -> new ServerMushroomBrown();
            case 6 -> new ServerBush();
            case 7 -> new ServerItem();
            case 8 -> new ServerAncientTree();
            case 9 -> new ServerGravestone();
            case 10 -> new ServerDandelion();
            case 11 -> new ServerPoppy();
            default -> null;
        };
    }

    public static ServerEntity typeToEntity(ServerEntityType type) {
        return typeToEntity(type.ENTITY_ID);
    }

}