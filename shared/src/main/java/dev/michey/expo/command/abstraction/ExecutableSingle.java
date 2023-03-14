package dev.michey.expo.command.abstraction;

import dev.michey.expo.command.util.CommandSyntaxException;

public interface ExecutableSingle {

    void executeCommand(String[] args) throws CommandSyntaxException;

}