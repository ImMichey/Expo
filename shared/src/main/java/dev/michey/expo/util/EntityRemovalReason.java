package dev.michey.expo.util;

public enum EntityRemovalReason {

    UNLOAD,         // When the chunk gets unloaded and the entity gets stored as a SavableEntity
    DEATH,          // When the entity has <= 0 health and is dead
    VISIBILITY,     // When the entity is out of range of a player and thus has to be forcefully unloaded by the server
    MERGE,          // When the entity is merged with another entity (only applies to ServerItem at the moment)
    DESPAWN,        // When the entity despawns due to a systematic design choice
    UNSPECIFIED,    // Unspecified
    CAUGHT,
    COMMAND,
    EXPLOSION,

}
