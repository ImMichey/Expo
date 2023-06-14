package dev.michey.expo.server.command;

import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.server.main.arch.AbstractServerCommand;
import dev.michey.expo.server.main.arch.ServerCommandResolver;
import dev.michey.expo.server.main.logic.entity.player.ServerPlayer;

public class ServerCommandRepeat extends AbstractServerCommand {

    @Override
    public String getCommandName() {
        return "/repeat";
    }

    @Override
    public String getCommandDescription() {
        return "Repeats the command x times";
    }

    @Override
    public String getCommandSyntax() {
        return "/repeat <amount> <command>";
    }

    @Override
    public void executeCommand(String[] args, ServerPlayer player) throws CommandSyntaxException {
        int amount = parseI(args, 1);

        StringBuilder cmdBuilder = new StringBuilder();
        for(int i = 2; i < args.length; i++) {
            cmdBuilder.append(args[i]);

            if(i < args.length - 1) {
                cmdBuilder.append(" ");
            }
        }
        String cmd = cmdBuilder.toString();

        for(int i = 0; i < amount; i++) {
            ((ServerCommandResolver) getResolver()).resolveCommand(cmd, player);
        }
    }

}