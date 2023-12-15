package dev.michey.expo.server;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.esotericsoftware.minlog.Log;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.server.config.ExpoServerConfiguration;
import dev.michey.expo.server.main.arch.ExpoServerDedicated;
import dev.michey.expo.server.main.logic.crafting.CraftingRecipeMapping;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.world.gen.WorldGen;
import dev.michey.expo.server.util.ExpoHardware;

import static dev.michey.expo.log.ExpoLogger.log;

/** Launches the server application. */
public class ServerLauncher {

	public static void main(String[] args) {
		// Enable logging to file + console for debugging
		ExpoLogger.enableDualLogging("serverlogs");
		overrideKryoLogger();

		ExpoHardware.dump();

		// Create a server configuration
		ExpoServerConfiguration fileConfig = new ExpoServerConfiguration();

		if(!fileConfig.loadContents()) {
			log("Failed to load ExpoServerConfiguration, aborting application.");
			System.exit(0);
		}

		if(fileConfig.isAuthPlayersEnabled() && fileConfig.getSteamWebApiKey().isEmpty()) {
			log("Error: Your server has steam authentication enabled, however your Steam Web API key is empty/invalid.");
			System.exit(0);
		}

		if(!fileConfig.isAuthPlayersEnabled()) {
			log("");
			log("===================================== WARNING =====================================");
			log("");
			log(" Your server does not authenticate player connections, it is recommended to enable");
			log(" the whitelist or to set a password that is required to join the server.");
			log("");
			log("===================================== WARNING =====================================");
			log("");
		}

		new WorldGen();

		// Create the server instance
		ExpoServerDedicated server = new ExpoServerDedicated(fileConfig);

		// Create the Headless GDX instance
		HeadlessApplicationConfiguration gdxConfig = new HeadlessApplicationConfiguration();
		gdxConfig.updatesPerSecond = fileConfig.getServerTps();
		new HeadlessApplication(server, gdxConfig);

		// Item Mapper
		new ItemMapper(false, false);
		new CraftingRecipeMapping(false);
	}

	public static void overrideKryoLogger() {
		Log.setLogger(new Log.Logger() {
			@Override
			public void log(int level, String category, String message, Throwable ex) {
				String levelStr = switch (level) {
					case 1 -> "TRACE";
					case 2 -> "DEBUG";
					case 3 -> "INFO";
					case 4 -> "WARN";
					case 5 -> "ERROR";
					default -> "";
				};

				ExpoLogger.log("[KryoNet-" + levelStr + "] " + message);

				if(ex != null) {
					ExpoLogger.log("[KryoNet-" + levelStr + "-EX] " + ex.getMessage());
				}
			}
		});
	}

}