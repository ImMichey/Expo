package dev.michey.expo.logic.entity.arch;

import dev.michey.expo.logic.entity.animal.*;
import dev.michey.expo.logic.entity.container.ClientCrate;
import dev.michey.expo.logic.entity.crop.ClientCropWheat;
import dev.michey.expo.logic.entity.flora.*;
import dev.michey.expo.logic.entity.hostile.ClientSlime;
import dev.michey.expo.logic.entity.hostile.ClientWoodfolk;
import dev.michey.expo.logic.entity.hostile.ClientZombie;
import dev.michey.expo.logic.entity.misc.*;
import dev.michey.expo.logic.entity.particle.ClientParticleFood;
import dev.michey.expo.logic.entity.particle.ClientParticleGore;
import dev.michey.expo.logic.entity.particle.ClientParticleHit;
import dev.michey.expo.logic.entity.particle.ClientParticleOakLeaf;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;

public enum ClientEntityType {

    /** Synced across server + client */
    PLAYER(0, "Player", ServerEntityType.PLAYER, true),
    DUMMY(1, "Dummy", ServerEntityType.DUMMY, true),
    GRASS(2, "Grass", ServerEntityType.GRASS, true),
    OAK_TREE(3, "OakTree", ServerEntityType.OAK_TREE, true),
    MUSHROOM_RED(4, "MushroomRed", ServerEntityType.MUSHROOM_RED, true),
    MUSHROOM_BROWN(5, "MushroomBrown", ServerEntityType.MUSHROOM_BROWN, true),
    BUSH(6, "Bush", ServerEntityType.BUSH, true),
    ITEM(7, "Item", ServerEntityType.ITEM, false),
    ANCIENT_TREE(8, "AncientTree", ServerEntityType.ANCIENT_TREE, true),
    GRAVESTONE(9, "Gravestone", ServerEntityType.GRAVESTONE, true),
    DANDELION(10, "Dandelion", ServerEntityType.DANDELION, true),
    POPPY(11, "Poppy", ServerEntityType.POPPY, true),
    BLUEBERRY_BUSH(12, "BlueberryBush", ServerEntityType.BLUEBERRY_BUSH, true),
    WORM(13, "Worm", ServerEntityType.WORM, true),
    ROCK(14, "Rock", ServerEntityType.ROCK, true),
    CRAB(15, "Crab", ServerEntityType.CRAB, true),
    DYNAMIC_3D_TILE(16, "Dynamic3DTile", ServerEntityType.DYNAMIC_3D_TILE, true),
    WHEAT_PLANT(17, "WheatPlant", ServerEntityType.WHEAT_PLANT, true),
    SUNFLOWER(18, "Sunflower", ServerEntityType.SUNFLOWER, true),
    FIREFLY(19, "Firefly", ServerEntityType.FIREFLY, true),
    MUSHROOM_GLOWING(20, "MushroomGlowing", ServerEntityType.MUSHROOM_GLOWING, true),
    LILYPAD(21, "Lilypad", ServerEntityType.LILYPAD, false),
    ZOMBIE(22, "Zombie", ServerEntityType.ZOMBIE, true),
    MAGGOT(23, "Maggot", ServerEntityType.MAGGOT, true),
    FENCE_STICK(24, "FenceStick", ServerEntityType.FENCE_STICK, true),
    CROP_WHEAT(25, "CropWheat", ServerEntityType.CROP_WHEAT, true),
    BOULDER(26, "Boulder", ServerEntityType.BOULDER, true),
    CRATE(27, "Crate", ServerEntityType.CRATE, true),
    OAK_TREE_SAPLING(28, "OakTreeSapling", ServerEntityType.OAK_TREE_SAPLING, true),
    CATTAIL(29, "Cattail", ServerEntityType.CATTAIL, true),
    CHICKEN(30, "Chicken", ServerEntityType.CHICKEN, true),
    SIGN(31, "Sign", ServerEntityType.SIGN, true),
    CAMPFIRE(32, "Campfire", ServerEntityType.CAMPFIRE, true),
    WOODFOLK(33, "Woodfolk", ServerEntityType.WOODFOLK, true),
    ALOE_VERA(34, "AloeVera", ServerEntityType.ALOE_VERA, true),
    TORCH(35, "Torch", ServerEntityType.TORCH, true),
    SLIME(36, "Slime", ServerEntityType.SLIME, true),
    THROWN_ENTITY(37, "ThrownEntity", ServerEntityType.THROWN_ENTITY, true),

    /** Client only */
    SELECTOR(-1, "Selector", null, false),
    RAINDROP(-2, "Raindrop", null, false),
    PARTICLE_HIT(-3, "ParticleHit", null, false),
    PARTICLE_FOOD(-4, "ParticleFood", null, false),
    GHOST_ITEM(-5, "GhostItem", null, false),
    PARTICLE_OAK_LEAF(-6, "ParticleOakLeaf", null, false),
    PUDDLE(-7, "Puddle", null, false),
    FALLING_TREE(-8, "FallingTree", null, true),
    DAMAGE_INDICATOR(-9, "DamageIndicator", null, false),
    CLOUD(-10, "Cloud", null, true),
    HEALTH_BAR(-11, "HealthBar", null, false),
    PICKUP_LINE(-12, "PickupLine", null, false),
    PARTICLE_GORE(-13, "ParticleGore", null, false),

    ;

    public final int ENTITY_ID;
    public final String ENTITY_NAME;
    public final ServerEntityType ENTITY_SERVER_TYPE;
    public final boolean ENTITY_SHADOWS;

    ClientEntityType(int ENTITY_ID, String ENTITY_NAME, ServerEntityType ENTITY_SERVER_TYPE, boolean ENTITY_SHADOWS) {
        this.ENTITY_ID = ENTITY_ID;
        this.ENTITY_NAME = ENTITY_NAME;
        this.ENTITY_SERVER_TYPE = ENTITY_SERVER_TYPE;
        this.ENTITY_SHADOWS = ENTITY_SHADOWS;
    }

    public static ClientEntity typeToClientEntity(int id) {
        return switch (id) {
            case -1 -> new ClientSelector();
            case -2 -> new ClientRaindrop();
            case -3 -> new ClientParticleHit();
            case -4 -> new ClientParticleFood();
            case -5 -> new ClientGhostItem();
            case -6 -> new ClientParticleOakLeaf();
            case -7 -> new ClientPuddle();
            case -8 -> new ClientFallingTree();
            case -9 -> new ClientDamageIndicator();
            case -10 -> new ClientCloud();
            case -11 -> new ClientHealthBar();
            case -12 -> new ClientPickupLine();
            case -13 -> new ClientParticleGore();
            case 0 -> new ClientPlayer();
            case 1 -> new ClientDummy();
            case 2 -> new ClientGrass();
            case 3 -> new ClientOakTree();
            case 4 -> new ClientMushroomRed();
            case 5 -> new ClientMushroomBrown();
            case 6 -> new ClientBush();
            case 7 -> new ClientItem();
            case 8 -> new ClientAncientTree();
            case 9 -> new ClientGravestone();
            case 10 -> new ClientDandelion();
            case 11 -> new ClientPoppy();
            case 12 -> new ClientBlueberryBush();
            case 13 -> new ClientWorm();
            case 14 -> new ClientRock();
            case 15 -> new ClientCrab();
            case 16 -> new ClientDynamic3DTile();
            case 17 -> new ClientWheat();
            case 18 -> new ClientSunflower();
            case 19 -> new ClientFirefly();
            case 20 -> new ClientMushroomGlowing();
            case 21 -> new ClientLilypad();
            case 22 -> new ClientZombie();
            case 23 -> new ClientMaggot();
            case 24 -> new ClientFenceStick();
            case 25 -> new ClientCropWheat();
            case 26 -> new ClientBoulder();
            case 27 -> new ClientCrate();
            case 28 -> new ClientOakTreeSapling();
            case 29 -> new ClientCattail();
            case 30 -> new ClientChicken();
            case 31 -> new ClientSign();
            case 32 -> new ClientCampfire();
            case 33 -> new ClientWoodfolk();
            case 34 -> new ClientAloeVera();
            case 35 -> new ClientTorch();
            case 36 -> new ClientSlime();
            case 37 -> new ClientThrownEntity();
            default -> null;
        };
    }

}
