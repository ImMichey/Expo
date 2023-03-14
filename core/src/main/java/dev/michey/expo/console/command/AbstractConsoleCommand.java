package dev.michey.expo.console.command;

import dev.michey.expo.command.abstraction.AbstractCommand;
import dev.michey.expo.command.abstraction.ExecutableSingle;
import dev.michey.expo.console.GameConsole;

public abstract class AbstractConsoleCommand extends AbstractCommand implements ExecutableSingle {

    /** Command lock */
    private boolean commandLock;

    public void empty() {
        GameConsole.get().addSystemMessage(" ");
    }

    public void success(String message) {
        GameConsole.get().addSystemSuccessMessage(message);
    }

    public void error(String message) {
        GameConsole.get().addSystemErrorMessage(message);
    }

    public void message(String message) {
        GameConsole.get().addSystemMessage(message);
    }

    public void lock() {
        commandLock = true;
    }

    public void unlock() {
        commandLock = false;
    }

    public boolean isLocked() {
        return commandLock;
    }

}
