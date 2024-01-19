package dev.michey.expo.util;

public enum EntityRemovalReason {

    UNLOAD(false),         // When the chunk gets unloaded and the entity gets stored as a SavableEntity
    DEATH(true),          // When the entity has <= 0 health and is dead
    VISIBILITY(false),     // When the entity is out of range of a player and thus has to be forcefully unloaded by the server
    MERGE(false),          // When the entity is merged with another entity (only applies to ServerItem at the moment)
    DESPAWN(false),        // When the entity despawns due to a systematic design choice
    UNSPECIFIED(false),    // Unspecified
    CAUGHT(false),
    COMMAND(false),
    EXPLOSION(true),

    ;

    public final boolean REMOVAL_IS_KILL_REASON;

    EntityRemovalReason(boolean REMOVAL_IS_KILL_REASON) {
        this.REMOVAL_IS_KILL_REASON = REMOVAL_IS_KILL_REASON;
    }

    public boolean isKillReason() {
        return REMOVAL_IS_KILL_REASON;
    }

}
