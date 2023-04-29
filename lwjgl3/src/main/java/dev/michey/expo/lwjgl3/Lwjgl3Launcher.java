package dev.michey.expo.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import dev.michey.expo.Expo;

/** Launches the desktop (LWJGL3) application. */
public class Lwjgl3Launcher {

	public static void main(String[] args) {
		try {
			createApplication();
		} catch (Exception e) {
			e.printStackTrace();

			CrashReportWindow window = new CrashReportWindow();
			window.show();
		}
	}

	private static Lwjgl3Application createApplication() {
		return new Lwjgl3Application(new Expo(), getDefaultConfiguration());
	}

	private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
		Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();

		configuration.setTitle("expo-multiplayer");
		configuration.setWindowedMode(1280, 720);
		configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");
		configuration.setOpenGLEmulation(Lwjgl3ApplicationConfiguration.GLEmulation.GL30, 3, 3);
		//configuration.setResizable(false);

		configuration.useVsync(false);
		//configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate);
		//configuration.setForegroundFPS(60);

		return configuration;
	}

}