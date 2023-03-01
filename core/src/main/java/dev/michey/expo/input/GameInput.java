package dev.michey.expo.input;

import com.badlogic.gdx.InputProcessor;
import dev.michey.expo.console.GameConsole;
import dev.michey.expo.logic.entity.ClientPlayer;

public class GameInput implements InputProcessor {

    private final InputController controller;

    public GameInput() {
        controller = new InputController();
    }

    @Override
    public boolean keyDown(int keycode) {
        controller.onKeyDown(keycode, consoleOpen(), inventoryOpen());
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        controller.onKeyUp(keycode, consoleOpen(), inventoryOpen());
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        controller.onKeyTyped(character, consoleOpen(), inventoryOpen());
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if(button == 0) {
            controller.onLeftClickBegin(screenX, screenY, consoleOpen(), inventoryOpen());
        } else if(button == 1) {
            controller.onRightClickBegin(screenX, screenY, consoleOpen(), inventoryOpen());
        } else if(button == 2) {
            controller.onMiddleClickBegin(screenX, screenY, consoleOpen(), inventoryOpen());
        }

        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if(button == 0) {
            controller.onLeftClickEnd(screenX, screenY, consoleOpen(), inventoryOpen());
        } else if(button == 1) {
            controller.onRightClickEnd(screenX, screenY, consoleOpen(), inventoryOpen());
        } else if(button == 2) {
            controller.onMiddleClickEnd(screenX, screenY, consoleOpen(), inventoryOpen());
        }

        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        controller.onDrag(screenX, screenY, consoleOpen(), inventoryOpen());
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        controller.onScroll(amountX, amountY, consoleOpen(), inventoryOpen());
        return false;
    }

    private boolean inventoryOpen() {
        return ClientPlayer.getLocalPlayer() != null && ClientPlayer.getLocalPlayer().inventoryOpen;
    }

    private boolean consoleOpen() {
        return GameConsole.get().isVisible();
    }

}
