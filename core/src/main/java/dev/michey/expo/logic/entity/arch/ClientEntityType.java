package dev.michey.expo.logic.entity.arch;

import dev.michey.expo.logic.entity.animal.ClientCrab;
import dev.michey.expo.logic.entity.animal.ClientWorm;
import dev.michey.expo.logic.entity.flora.*;
import dev.michey.expo.logic.entity.misc.*;
import dev.michey.expo.logic.entity.particle.ClientParticleFood;
import dev.michey.expo.logic.entity.particle.ClientParticleHit;
import dev.michey.expo.logic.entity.particle.ClientParticleOakLeaf;
import dev.michey.expo.logic.entity.player.ClientPlayer;
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
    ANCIENT_TREE(8, "AncientTree", ServerEntityType.ANCIENT_TREE),
    GRAVESTONE(9, "Gravestone", ServerEntityType.GRAVESTONE),
    DANDELION(10, "Dandelion", ServerEntityType.DANDELION),
    POPPY(11, "Poppy", ServerEntityType.POPPY),
    BLUEBERRY_BUSH(12, "BlueberryBush", ServerEntityType.BLUEBERRY_BUSH),
    WORM(13, "Worm", ServerEntityType.WORM),
    ROCK(14, "Rock", ServerEntityType.ROCK),
    CRAB(15, "Crab", ServerEntityType.CRAB),

    /** Client only */
    SELECTOR(-1, "Selector", null),
    RAINDROP(-2, "Raindrop", null),
    PARTICLE_HIT(-3, "ParticleHit", null),
    PARTICLE_FOOD(-4, "ParticleFood", null),
    GHOST_ITEM(-5, "GhostItem", null),
    PARTICLE_OAK_LEAF(-6, "ParticleOakLeaf", null),
    PUDDLE(-7, "Puddle", null),

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
            case -1 -> new ClientSelector();
            case -2 -> new ClientRaindrop();
            case -3 -> new ClientParticleHit();
            case -4 -> new ClientParticleFood();
            case -5 -> new ClientGhostItem();
            case -6 -> new ClientParticleOakLeaf();
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
            default -> null;
        };
    }

}
