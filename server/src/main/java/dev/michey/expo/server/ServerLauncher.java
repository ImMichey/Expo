package dev.michey.expo.server;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.server.config.ExpoServerConfiguration;
import dev.michey.expo.server.main.arch.ExpoServerDedicated;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.world.gen.WorldGen;

import static dev.michey.expo.log.ExpoLogger.log;

/** Launches the server application. */
public class ServerLauncher {

	public static void main(String[] args) {
		// Enable logging to file + console for debugging
		ExpoLogger.enableDualLogging("serverlogs");

		// Create a server configuration
		ExpoServerConfiguration fileConfig = new ExpoServerConfiguration();

		if(!fileConfig.loadContents()) {
			log("Failed to load ExpoServerConfiguration, aborting application.");
			System.exit(0);
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
	}

}