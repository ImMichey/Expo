package dev.michey.expo.server.main.logic.entity.misc;

import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntity;
import dev.michey.expo.server.main.logic.entity.arch.ServerEntityType;
import dev.michey.expo.server.util.PacketReceiver;
import dev.michey.expo.server.util.ServerPackets;

public class ServerDummy extends ServerEntity {

    public float dstX;
    public float dstY;
    public float speed = 1.0f;

    @Override
    public void onCreation() {
        generateDst();
    }

    @Override
    public void tick(float delta) {
        boolean travelX = Math.abs(dstX - posX) > 2.0f;
        boolean travelY = Math.abs(dstY - posY) > 2.0f;

        int xDir = dstX > posX ? 1 : -1;
        int yDir = dstY > posY ? 1 : -1;

        if(travelX) {
            posX += speed * xDir;
        }
        if(travelY) {
            posY += speed * yDir;
        }

        if(!travelX && !travelY) {
            generateDst();
        } else {
            ServerPackets.p13EntityMove(entityId, xDir, yDir, posX, posY, PacketReceiver.whoCanSee(this));
            //ServerPackets.p6EntityPosition(entityId, posX, posY, PacketReceiver.whoCanSee(this));
        }
    }

    public void generateDst() {
        dstX = getDimension().getDimensionSpawnX() + MathUtils.random(-96f, 96f);
        dstY = getDimension().getDimensionSpawnY() + MathUtils.random(-96f, 96f);
    }

    @Override
    public ServerEntityType getEntityType() {
        return ServerEntityType.DUMMY;
    }

}
