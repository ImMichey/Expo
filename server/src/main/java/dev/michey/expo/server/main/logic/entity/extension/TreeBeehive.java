package dev.michey.expo.server.main.logic.entity.extension;

import dev.michey.expo.server.main.logic.entity.flora.ServerOakTree;

public class TreeBeehive {

    public float offsetX;
    public float offsetY;

    public ServerOakTree.BeehiveData toData() {
        return new ServerOakTree.BeehiveData(offsetX, offsetY);
    }

}
