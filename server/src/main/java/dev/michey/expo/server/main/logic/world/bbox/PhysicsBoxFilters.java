package dev.michey.expo.server.main.logic.world.bbox;

import com.dongbat.jbump.CollisionFilter;
import com.dongbat.jbump.Response;
import dev.michey.expo.server.main.logic.entity.misc.ServerItem;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;

public class PhysicsBoxFilters {

    public static CollisionFilter noclipFilter = (item, other) -> Response.cross;

    public static CollisionFilter playerCollisionFilter = (item, other) -> {
        if(other.userData instanceof ServerPlayer) {
            return Response.cross;
        }
        if(other.userData instanceof ServerItem) {
            return Response.cross;
        }

        return Response.slide;
    };

    public static CollisionFilter onlyCrossPlayerFilter = (item, other) -> {
        if(other.userData instanceof ServerPlayer) {
            return Response.cross;
        }

        return Response.slide;
    };

}
