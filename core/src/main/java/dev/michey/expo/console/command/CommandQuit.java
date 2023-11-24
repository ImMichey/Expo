package dev.michey.expo.console.command;

import dev.michey.expo.Expo;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.logic.inventory.PlayerInventory;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.util.ClientStatic;

public class CommandQuit extends AbstractConsoleCommand {

    @Override
    public String getCommandName() {
        return "/quit";
    }

    @Override
    public String getCommandDescription() {
        return "Disconnects you from your current game session";
    }

    @Override
    public String getCommandSyntax() {
        return "/quit";
    }

    @Override
    public void executeCommand(String[] args) {
        if(Expo.get().isPlaying()) {
            lock();
            success("Disconnecting from current game session...");
            ClientPlayer.setLocalPlayer(null);
            PlayerInventory.LOCAL_INVENTORY = null;
            RenderContext.get().expoCamera.resetLerp();
            Expo.get().switchToExistingScreen(ClientStatic.SCREEN_MENU);

            new Thread(() -> {
                Expo.get().disposeAndRemoveInactiveScreen(ClientStatic.SCREEN_GAME);
                ExpoServerBase.get().resetInstance();
                unlock();
            }).start();
        } else {
            error("You are not ingame.");
        }
    }

}
