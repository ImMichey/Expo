package dev.michey.expo.console.command;

import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.console.GameConsole;
import dev.michey.expo.lang.Lang;

public class CommandLang extends AbstractConsoleCommand {

    @Override
    public String getCommandName() {
        return "/lang";
    }

    @Override
    public String getCommandDescription() {
        return "Switch the game language";
    }

    @Override
    public String getCommandSyntax() {
        return "/lang <langCode>";
    }

    @Override
    public void executeCommand(String[] args) throws CommandSyntaxException {
        String langCode = parseString(args, 1);

        Lang.get().load(langCode);
        Lang.get().setActiveLangCode(langCode);

        GameConsole.get().addSystemSuccessMessage("Switched game language to '" + langCode + "'.");
    }

}