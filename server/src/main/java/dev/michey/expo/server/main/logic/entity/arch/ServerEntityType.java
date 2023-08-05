package dev.michey.expo.server.main.logic.entity.arch;

import dev.michey.expo.server.main.logic.entity.animal.ServerCrab;
import dev.michey.expo.server.main.logic.entity.animal.ServerFirefly;
import dev.michey.expo.server.main.logic.entity.animal.ServerMaggot;
import dev.michey.expo.server.main.logic.entity.animal.ServerWorm;
import dev.michey.expo.server.main.logic.entity.flora.*;
import dev.michey.expo.server.main.logic.entity.hostile.ServerZombie;
import dev.michey.expo.server.main.logic.entity.misc.*;
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
    ROCK(14, "Rock", true),
    CRAB(15, "Crab", false),
    DYNAMIC_3D_TILE(16, "Dynamic3DTile", true),
    WHEAT_PLANT(17, "WheatPlant", false),
    SUNFLOWER(18, "Sunflower", false),
    FIREFLY(19, "Firefly", false),
    MUSHROOM_GLOWING(20, "MushroomGlowing", false),
    LILYPAD(21, "Lilypad", true),
    ZOMBIE(22, "Zombie", false),
    MAGGOT(23, "Maggot", false),
    FENCE_STICK(24, "FenceStick", true),
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
            case 14 -> new ServerRock();
            case 15 -> new ServerCrab();
            case 16 -> new ServerDynamic3DTile();
            case 17 -> new ServerWheat();
            case 18 -> new ServerSunflower();
            case 19 -> new ServerFirefly();
            case 20 -> new ServerMushroomGlowing();
            case 21 -> new ServerLilypad();
            case 22 -> new ServerZombie();
            case 23 -> new ServerMaggot();
            case 24 -> new ServerFenceStick();
            default -> null;
        };
    }

    public static ServerEntity nameToEntity(String type) {
        return switch (type) {
            case "PLAYER" -> new ServerPlayer();
            case "DUMMY" -> new ServerDummy();
            case "GRASS" -> new ServerGrass();
            case "OAKTREE", "OAK_TREE", "TREE" -> new ServerOakTree();
            case "MUSHROOMRED", "MUSHROOM_RED" -> new ServerMushroomRed();
            case "MUSHROOMBROWN", "MUSHROOM_BROWN" -> new ServerMushroomBrown();
            case "BUSH" -> new ServerBush();
            case "ITEM" -> new ServerItem();
            case "ANCIENTTREE", "ANCIENT_TREE" -> new ServerAncientTree();
            case "GRAVESTONE" -> new ServerGravestone();
            case "DANDELION" -> new ServerDandelion();
            case "POPPY" -> new ServerPoppy();
            case "BLUEBERRYBUSH", "BLUEBERRY_BUSH" -> new ServerBlueberryBush();
            case "WORM" -> new ServerWorm();
            case "ROCK" -> new ServerRock();
            case "CRAB" -> new ServerCrab();
            case "DYNAMIC3DTILE", "DYNAMIC_3D_TILE" -> new ServerDynamic3DTile();
            case "WHEAT" -> new ServerWheat();
            case "SUNFLOWER" -> new ServerSunflower();
            case "FIREFLY" -> new ServerFirefly();
            case "MUSHROOMGLOWING", "MUSHROOM_GLOWING" -> new ServerMushroomGlowing();
            case "LILYPAD" -> new ServerLilypad();
            case "ZOMBIE" -> new ServerZombie();
            case "MAGGOT" -> new ServerMaggot();
            case "FENCESTICK", "FENCE_STICK", "STICKFENCE", "STICK_FENCE" -> new ServerFenceStick();
            default -> null;
        };
    }

    public static ServerEntity typeToEntity(ServerEntityType type) {
        return typeToEntity(type.ENTITY_ID);
    }

}