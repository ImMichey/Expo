package dev.michey.expo.server.main.logic.entity.arch;

import dev.michey.expo.server.main.logic.world.bbox.EntityPhysicsBox;

public interface PhysicsEntity {

    EntityPhysicsBox getPhysicsBox();

    PhysicsMassClassification getPhysicsMassClassification();

}
