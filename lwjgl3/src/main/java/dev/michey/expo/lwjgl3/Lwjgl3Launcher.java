package dev.michey.expo.lwjgl3;

import com.badlogic.gdx.Graphics;
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
		configuration.setWindowIcon("icon16.png");
		configuration.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30, 3, 3);
		configuration.useVsync(settings.vsync);
		configuration.setForegroundFPS(settings.fpsCap);
		configuration.setAutoIconify(true);

		// int r = 8, g = 8, b = 8, a = 8;
		//	int depth = 16, stencil = 0;
		//	int samples = 0;

		int mode = settings.windowMode; // 0 = Windowed, 1 = Borderless, 2 = Fullscreen

		if(mode == 0) {
			// Windowed mode, use preferred width + height
			configuration.setWindowedMode(settings.preferredWidth, settings.preferredHeight);
		} else if(mode == 1) {
			// Borderless fullscreen, use screen dimensions
			Graphics.DisplayMode displayMode = Lwjgl3ApplicationConfiguration.getDisplayMode();
			configuration.setWindowedMode(displayMode.width, displayMode.height);
			configuration.setDecorated(false);
		} else if(mode == 2) {
			// Exclusive fullscreen, use display mode/monitor
			configuration.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
		}

		return configuration;
	}

}