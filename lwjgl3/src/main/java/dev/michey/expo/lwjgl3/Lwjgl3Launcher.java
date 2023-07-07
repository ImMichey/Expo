package dev.michey.expo.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import dev.michey.expo.Expo;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.util.GameSettings;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {

	public static void main(String[] args) {
		GameSettings settings = new GameSettings();
		
		try {
			new Lwjgl3Application(new Expo(settings), getDefaultConfiguration(settings));
		} catch (Exception e) {
			e.printStackTrace();

			CrashReportWindow window = new CrashReportWindow();
			window.show();
		}
	}

	private static Lwjgl3ApplicationConfiguration getDefaultConfiguration(GameSettings settings) {
		Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();

		configuration.setTitle("Expo");
		configuration.setWindowedMode(settings.preferredWidth, settings.preferredHeight);
		configuration.setWindowIcon("icon16.png");
		configuration.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30, 3, 3);
		configuration.useVsync(settings.vsync);
		configuration.setForegroundFPS(settings.fpsCap);

		return configuration;
	}

}