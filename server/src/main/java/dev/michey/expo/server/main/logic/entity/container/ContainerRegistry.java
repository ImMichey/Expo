package dev.michey.expo.server.main.logic.entity.container;

public class ContainerRegistry {

    private static ContainerRegistry INSTANCE;

    private int currentContainerId;

    public ContainerRegistry() {
        INSTANCE = this;
    }

    public int getNewUniqueContainerId() {
        int currentId = currentContainerId;
        currentContainerId++;
        return currentId;
    }

    public void setCurrentContainerId(int currentContainerId) {
        this.currentContainerId = currentContainerId;
    }

    public int getCurrentContainerId() {
        return currentContainerId;
    }

    public static ContainerRegistry get() {
        return INSTANCE;
    }

}
