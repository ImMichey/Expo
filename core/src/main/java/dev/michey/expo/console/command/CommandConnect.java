package dev.michey.expo.console.command;

import com.badlogic.gdx.Gdx;
import dev.michey.expo.Expo;
import dev.michey.expo.client.ExpoClient;
import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.screen.GameScreen;
import dev.michey.expo.server.main.logic.crafting.CraftingRecipeMapping;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.util.ClientPackets;
import dev.michey.expo.util.ExpoShared;

public class CommandConnect extends AbstractConsoleCommand {

    @Override
    public String getCommandName() {
        return "/connect";
    }

    @Override
    public String getCommandDescription() {
        return "Connects to a dedicated game server";
    }

    @Override
    public String getCommandSyntax() {
        return "/connect <ip[:port]> [password]";
    }

    @Override
    public void executeCommand(String[] args) throws CommandSyntaxException {
        if(!Expo.get().isPlaying()) {
            String ip = parseString(args, 1);
            message("Parsing server address [CYAN]" + ip);
            int port = ExpoShared.DEFAULT_EXPO_SERVER_PORT;
            String password = null;

            if(ip.contains(":")) {
                String[] splitIp = ip.split(":");

                if(splitIp.length > 1) {
                    ip = splitIp[0];
                    var conv = ExpoShared.asInt(splitIp[1]);

                    if(conv.value) {
                        port = conv.key;
                        message("Using custom port [GREEN]" + port);
                    } else {
                        error("Invalid port '" + splitIp[1] + "', using default port " + port);
                    }
                } else {
                    error("Invalid custom port, using default port " + port);
                }
            }

            if(args.length > 2) {
                password = parseString(args, 2);
            }

            // Connect to server.
            message("Attempting to connect to remote server [CYAN]" + ip + ":" + port + "[WHITE]...");

            // TODO: Move this block to GDX runnable upon connecting?
            if(ItemMapper.get() == null) {
                new ItemMapper(true, false);
                Expo.get().loadItemMapperTextures();
                new CraftingRecipeMapping(true);
            }

            String finalIp = ip;
            int finalPort = port;
            String finalPassword = password;
            lock();

            new Thread(() -> {
                ExpoClient client = new ExpoClient();
                Exception exception = client.connect(finalIp, finalPort);

                if(exception == null) {
                    Gdx.app.postRunnable(() -> {
                        Expo.get().switchToNewScreen(new GameScreen(client));
                        success("Successfully connected to remote server [CYAN]" + finalIp + ":" + finalPort);

                        ClientPackets.p0ConnectReq(finalPassword);
                    });
                } else {
                    error("Failed to connect to remote server [CYAN]" + finalIp + ":" + finalPort);
                    error("Reason: " + exception.getMessage());
                }

                unlock();
            }).start();
        } else {
            error("You are already ingame.");
        }
    }

}