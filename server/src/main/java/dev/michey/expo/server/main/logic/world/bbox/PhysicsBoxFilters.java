package dev.michey.expo.server.main.logic.world.bbox;

import com.dongbat.jbump.CollisionFilter;
import com.dongbat.jbump.Response;
import dev.michey.expo.server.main.logic.entity.arch.PhysicsEntity;
import dev.michey.expo.server.main.logic.entity.arch.PhysicsMassClassification;

public class PhysicsBoxFilters {

    public static CollisionFilter noclipFilter = (item, other) -> Response.cross;

    public static CollisionFilter playerKnockbackFilter = (item, other) -> {
        if(other.userData instanceof PhysicsEntity oe) {
            if(oe.getPhysicsMassClassification() == PhysicsMassClassification.HEAVY) {
                return Response.slide;
            }

            if(oe.getPhysicsMassClassification() == PhysicsMassClassification.PLAYER) {
                return Response.slide;
            }

            return Response.cross;
        }

        return Response.slide;
    };

    public static CollisionFilter playerCollisionFilter = (item, other) -> {
        if(other.userData instanceof PhysicsEntity oe) {
            PhysicsMassClassification otherClassification = oe.getPhysicsMassClassification();

            if(otherClassification == PhysicsMassClassification.PLAYER || otherClassification == PhysicsMassClassification.ITEM || otherClassification == PhysicsMassClassification.MEDIUM_PLAYER_PASSABLE) {
                return null;
            }

            if(otherClassification == PhysicsMassClassification.LIGHT || otherClassification == PhysicsMassClassification.THROWN) {
                return null;
            }
        }

        return Response.slide;
    };

    public static CollisionFilter thrownFilter = (item, other) -> Response.cross;

    public static CollisionFilter generalFilter = (item, other) -> {
        if(item.userData instanceof PhysicsEntity pe) {
            PhysicsMassClassification classification = pe.getPhysicsMassClassification();

            if(other.userData instanceof PhysicsEntity oe) {
                PhysicsMassClassification otherClassification = oe.getPhysicsMassClassification();

                if(otherClassification == PhysicsMassClassification.PLAYER || otherClassification == PhysicsMassClassification.THROWN) {
                    return Response.cross;
                }

                if(otherClassification == PhysicsMassClassification.MEDIUM && classification == PhysicsMassClassification.MEDIUM) {
                    return Response.slide;
                }

                if(otherClassification == PhysicsMassClassification.WALL) {
                    return Response.slide;
                }

                if(otherClassification == PhysicsMassClassification.LIGHT || otherClassification == PhysicsMassClassification.ITEM) {
                    return null;
                }
            }
        }

        return Response.slide;
    };

}
