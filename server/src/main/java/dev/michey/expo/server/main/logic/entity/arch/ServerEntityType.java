package dev.michey.expo.server.main.logic.entity.arch;

import dev.michey.expo.server.main.logic.entity.animal.ServerWorm;
import dev.michey.expo.server.main.logic.entity.flora.*;
import dev.michey.expo.server.main.logic.entity.misc.ServerDummy;
import dev.michey.expo.server.main.logic.entity.misc.ServerGravestone;
import dev.michey.expo.server.main.logic.entity.misc.ServerItem;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;

public enum ServerEntityType {

    PLAYER(0, "Player", false),
    DUMMY(1, "Dummy", false),
    GRASS(2, "Grass", true),
    OAK_TREE(3, "OakTree", true),
    MUSHROOM_RED(4, "MushroomRed", false),
    MUSHROOM_BROWN(5, "MushroomBrown", false),
    BUSH(6, "Bush", false),
    ITEM(7, "Item", true),
    ANCIENT_TREE(8, "AncientTree", false),
    GRAVESTONE(9, "Gravestone", false),
    DANDELION(10, "Dandelion", false),
    POPPY(11, "Poppy", false),
    BLUEBERRY_BUSH(12, "BlueberryBush", true),
    WORM(13, "Worm", false),
    ;

    public final int ENTITY_ID;
    public final String ENTITY_NAME;
    public final boolean ADVANCED_PAYLOAD;

    ServerEntityType(int ENTITY_ID, String ENTITY_NAME, boolean ADVANCED_PAYLOAD) {
        this.ENTITY_ID = ENTITY_ID;
        this.ENTITY_NAME = ENTITY_NAME;
        this.ADVANCED_PAYLOAD = ADVANCED_PAYLOAD;
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
            case 12 -> new ServerBlueberryBush();
            case 13 -> new ServerWorm();
            default -> null;
        };
    }

    public static ServerEntity typeToEntity(ServerEntityType type) {
        return typeToEntity(type.ENTITY_ID);
    }

}