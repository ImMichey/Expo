package dev.michey.expo.server.main.logic.entity.arch;

import dev.michey.expo.server.main.logic.entity.animal.*;
import dev.michey.expo.server.main.logic.entity.container.ServerCrate;
import dev.michey.expo.server.main.logic.entity.crop.ServerCropWheat;
import dev.michey.expo.server.main.logic.entity.flora.*;
import dev.michey.expo.server.main.logic.entity.hostile.ServerSlime;
import dev.michey.expo.server.main.logic.entity.hostile.ServerWoodfolk;
import dev.michey.expo.server.main.logic.entity.hostile.ServerZombie;
import dev.michey.expo.server.main.logic.entity.misc.*;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;

public enum ServerEntityType {

    PLAYER(0, false),
    DUMMY(1, false),
    GRASS(2, true),
    OAK_TREE(3, true),
    MUSHROOM_RED(4, false),
    MUSHROOM_BROWN(5, false),
    BUSH(6, false),
    ITEM(7, true),
    ANCIENT_TREE(8, false),
    GRAVESTONE(9, true),
    DANDELION(10, false),
    POPPY(11, false),
    BLUEBERRY_BUSH(12, true),
    WORM(13, false),
    ROCK(14, true),
    CRAB(15, true),
    DYNAMIC_3D_TILE(16, true),
    WHEAT_PLANT(17, false),
    SUNFLOWER(18, true),
    FIREFLY(19, false),
    MUSHROOM_GLOWING(20, false),
    LILYPAD(21, true),
    ZOMBIE(22, false),
    MAGGOT(23, false),
    FENCE_STICK(24, true),
    CROP_WHEAT(25, true),
    BOULDER(26, true),
    CRATE(27, true),
    OAK_TREE_SAPLING(28, false),
    CATTAIL(29, true),
    CHICKEN(30, true),
    SIGN(31, true),
    CAMPFIRE(32, true),
    WOODFOLK(33, false),
    ALOE_VERA(34, false),
    TORCH(35, false),
    SLIME(36, false),
    ;

    public final int ENTITY_ID;
    public final boolean ADVANCED_PAYLOAD;

    ServerEntityType(int ENTITY_ID, boolean ADVANCED_PAYLOAD) {
        this.ENTITY_ID = ENTITY_ID;
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
            case 25 -> new ServerCropWheat();
            case 26 -> new ServerBoulder();
            case 27 -> new ServerCrate();
            case 28 -> new ServerOakTreeSapling();
            case 29 -> new ServerCattail();
            case 30 -> new ServerChicken();
            case 31 -> new ServerSign();
            case 32 -> new ServerCampfire();
            case 33 -> new ServerWoodfolk();
            case 34 -> new ServerAloeVera();
            case 35 -> new ServerTorch();
            case 36 -> new ServerSlime();
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
            case "CROPWHEAT", "CROP_WHEAT", "WHEATCROP", "WHEAT_CROP" -> new ServerCropWheat();
            case "BOULDER" -> new ServerBoulder();
            case "CRATE" -> new ServerCrate();
            case "OAKTREESAPLING", "OAK_TREE_SAPLING", "SAPLING" -> new ServerOakTreeSapling();
            case "CATTAIL", "CAT_TAIL" -> new ServerCattail();
            case "CHICKEN" -> new ServerChicken();
            case "SIGN" -> new ServerSign();
            case "CAMPFIRE" -> new ServerCampfire();
            case "WOODFOLK", "WOOD_FOLK" -> new ServerWoodfolk();
            case "ALOE", "ALOE_VERA", "ALOEVERA", "VERA" -> new ServerAloeVera();
            case "TORCH" -> new ServerTorch();
            case "SLIME" -> new ServerSlime();
            default -> null;
        };
    }

    public static ServerEntity typeToEntity(ServerEntityType type) {
        return typeToEntity(type.ENTITY_ID);
    }

}