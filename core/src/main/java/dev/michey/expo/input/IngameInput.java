package dev.michey.expo.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import dev.michey.expo.client.chat.ExpoClientChat;
import dev.michey.expo.console.GameConsole;

public class IngameInput {

    private static IngameInput INSTANCE;

    public IngameInput() {
        INSTANCE = this;
    }

    public boolean keyPressed(int key) {
        if(GameConsole.get().isVisible()) return false;
        if(ExpoClientChat.get().isFocused()) return false;
        return Gdx.input.isKeyPressed(key);
    }

    public boolean leftPressed() {
        if(GameConsole.get().isVisible()) return false;
        if(ExpoClientChat.get().isFocused()) return false;
        return Gdx.input.isButtonPressed(Input.Buttons.LEFT);
    }

    public boolean leftJustPressed() {
        if(GameConsole.get().isVisible()) return false;
        if(ExpoClientChat.get().isFocused()) return false;
        return Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);
    }

    public boolean rightPressed() {
        if(GameConsole.get().isVisible()) return false;
        if(ExpoClientChat.get().isFocused()) return false;
        return Gdx.input.isButtonPressed(Input.Buttons.RIGHT);
    }

    public boolean rightJustPressed() {
        if(GameConsole.get().isVisible()) return false;
        if(ExpoClientChat.get().isFocused()) return false;
        return Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT);
    }

    public int pressedNumber() {
        if(GameConsole.get().isVisible()) return -1;
        if(ExpoClientChat.get().isFocused()) return -1;

        for(int i = 0; i < 9; i++) {
            if(Gdx.input.isKeyJustPressed(8 + i)) return i;
        }

        return -1;
    }

    public boolean keyJustPressed(int key) {
        if(GameConsole.get().isVisible()) return false;
        if(ExpoClientChat.get().isFocused()) return false;
        return Gdx.input.isKeyJustPressed(key);
    }

    public static IngameInput get() {
        return INSTANCE;
    }

}
