package dev.michey.expo.screen;

import dev.michey.expo.client.ExpoClient;
import dev.michey.expo.localserver.ExpoServerLocal;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.util.ClientStatic;

public class GameScreen extends AbstractScreen {

    /** Single-player game container. */
    private final ExpoClientContainer clientContainer;

    /** Constructor called when player is playing a single-player instance. */
    public GameScreen(ExpoServerLocal localServer) {
        clientContainer = new ExpoClientContainer(localServer);
    }

    /** Constructor called when player is playing a multiplayer instance. */
    public GameScreen(ExpoClient client) {
        clientContainer = new ExpoClientContainer(client);
    }

    @Override
    public void render() {
        clientContainer.render();
    }

    @Override
    public void resize(int width, int height) {
        RenderContext.get().expoCamera.resize(width, height);
    }

    @Override
    public void dispose() {
        clientContainer.stopSession();
    }

    @Override
    public String getScreenName() {
        return ClientStatic.SCREEN_GAME;
    }

}
