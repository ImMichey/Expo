package dev.michey.expo.command.util;

public class CommandSyntaxException extends Exception {

    public CommandExceptionReason reason = CommandExceptionReason.UNKNOWN;

    public CommandSyntaxException(String message) {
        super(message);
    }

    public CommandSyntaxException(String message, CommandExceptionReason reason) {
        super(message);
        this.reason = reason;
    }

}