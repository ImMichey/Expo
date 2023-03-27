package dev.michey.expo;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.ScreenUtils;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.audio.AudioEngine;
import dev.michey.expo.console.ConsoleMessage;
import dev.michey.expo.console.GameConsole;
import dev.michey.expo.devhud.DevHUD;
import dev.michey.expo.input.GameInput;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.imgui.ImGuiExpo;
import dev.michey.expo.screen.AbstractScreen;
import dev.michey.expo.screen.MenuScreen;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.util.ClientStatic;
import imgui.ImFontAtlas;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;

import static dev.michey.expo.log.ExpoLogger.log;
import static dev.michey.expo.util.ClientStatic.DEV_MODE;

public class Expo implements ApplicationListener {

	/** Singleton */
	private static Expo INSTANCE;

	/** Screen handling */
	private AbstractScreen activeScreen;
	private final HashMap<String, AbstractScreen> inactiveScreens;

	/** ImGui */
	private ImGuiImplGlfw imGuiGlfw;
	private ImGuiImplGl3 imGuiGl3;
	private ImGuiExpo imGuiExpo;

	public Expo() {
		// Enable logging to file + console for debugging
		//ExpoLogger.enableDualLogging("clientlogs");
		inactiveScreens = new HashMap<>();
	}

	@Override
	public void create() {
		if(DEV_MODE) {
			imGuiGlfw = new ImGuiImplGlfw();
			imGuiGl3 = new ImGuiImplGl3();
			imGuiExpo = new ImGuiExpo();

			// Initialize ImGui
			long pointer = ((Lwjgl3Graphics) Gdx.graphics).getWindow().getWindowHandle();
			GLFW.glfwMakeContextCurrent(pointer);
			ImGui.createContext();

			ImGuiIO io = ImGui.getIO();
			// io.setWantSaveIniSettings(false); 						For production build.

			String customFont = Gdx.files.local("Roboto-Regular.ttf").file().getAbsolutePath();
			// String customFont = Gdx.files.internal("assets/fonts/Roboto-Regular.ttf").file().getAbsolutePath();

			ImFontAtlas atlas = io.getFonts();
			atlas.addFontFromFileTTF(customFont, 16);
			atlas.build();

			imGuiGlfw.init(pointer, true);
			imGuiGl3.init("#version 330");
		}

		AudioEngine.get();
		ExpoAssets.get().loadAssets();
		setCursor();
		new RenderContext();
		Gdx.input.setInputProcessor(new GameInput());
		switchToNewScreen(new MenuScreen());
		GameConsole.get().addSystemMessage("In order to see an overview of existing commands, type '/help'.");
		INSTANCE = this;

		autoExec();

		/*
			ExpoAssets.get().slice("tile_soil", true, 0, 0);
			ExpoAssets.get().slice("tile_grass", false, 0, 16);
			ExpoAssets.get().slice("tile_sand", false, 0, 48);
			ExpoAssets.get().slice("tile_not_set", true, 0, 80);
			ExpoAssets.get().slice("tile_ocean", false, 0, 96);
			ExpoAssets.get().slice("tile_ocean_deep", false, 0, 128);
		*/
	}

	@Override
	public void resize(int width, int height) {
		if(activeScreen != null) {
			log("resize() invoked for '" + activeScreen.getScreenName() + "': " + width + "x" + height);
			activeScreen.resize(width, height);

			if(width > 0 && height > 0) {
				Matrix4 uiMatrix = new Matrix4().setToOrtho2D(0, 0, width, height);
				RenderContext.get().hudBatch.setProjectionMatrix(uiMatrix);
				RenderContext.get().onResize(width, height);
				GameConsole.get().updateBatches(uiMatrix);
			}
		} else {
			log("resize() invoked for null");
		}
	}

	@Override
	public void render() {
		RenderContext r = RenderContext.get();

		r.update();
		AudioEngine.get().tick();
		ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1.0f);

		if(r.drawImGui && DEV_MODE) {
			imGuiGlfw.newFrame();
			ImGui.newFrame();
		}

		if(activeScreen != null) {
			activeScreen.render();
		}

		GameConsole.get().draw();
		DevHUD.get().draw();

		if(r.drawImGui && DEV_MODE) {
			imGuiExpo.draw();
			ImGui.render();
			imGuiGl3.renderDrawData(ImGui.getDrawData());
		}

		r.reset();
	}

	@Override
	public void pause() {
		if(activeScreen != null) {
			log("pause() invoked for '" + activeScreen.getScreenName() + "'");
			activeScreen.pause();
		} else {
			log("pause() invoked for null");
		}
	}

	@Override
	public void resume() {
		if(activeScreen != null) {
			log("resume() invoked for '" + activeScreen.getScreenName() + "'");
			activeScreen.resume();
		} else {
			log("resume() invoked for null");
		}
	}

	@Override
	public void dispose() {
		log("dispose() invoked for game handle, disposing all screens...");
		if(activeScreen != null) {
			disposeScreen(activeScreen);
		}

		for(AbstractScreen inactiveScreen : inactiveScreens.values()) {
			disposeScreen(inactiveScreen);
		}

		GameConsole.get().dispose();
	}

	public void switchToExistingScreen(String screenName) {
		if(inactiveScreens.containsKey(screenName)) {
			log("Switching to existing screen '" + screenName + "'");
			if(activeScreen != null) {
				activeScreen.onInactive();
				inactiveScreens.put(activeScreen.getScreenName(), activeScreen);
			}
			activeScreen = inactiveScreens.get(screenName);
			activeScreen.onActive();
			inactiveScreens.remove(screenName);
		} else {
			log("Could not switch to existing screen '" + screenName + "'");
		}
	}

	public void switchToNewScreen(AbstractScreen screen) {
		log("Switching to new screen '" + screen.getScreenName() + "'");
		if(activeScreen != null) {
			activeScreen.onInactive();
			inactiveScreens.put(activeScreen.getScreenName(), activeScreen);
		}
		activeScreen = screen;
		activeScreen.onActive();
	}

	public void disposeAndRemoveInactiveScreen(String screenName) {
		if(inactiveScreens.containsKey(screenName)) {
			disposeScreen(inactiveScreens.get(screenName));
			inactiveScreens.remove(screenName);
		} else {
			log("dispose() invoked for null");
		}
	}

	private void setCursor() {
		Pixmap pixmap = new Pixmap(Gdx.files.internal("textures/system/cursor.png"));
		int xHotspot = pixmap.getWidth() / 2;
		int yHotspot = pixmap.getHeight() / 2;
		Cursor cursor = Gdx.graphics.newCursor(pixmap, xHotspot, yHotspot);
		Gdx.graphics.setCursor(cursor);
		pixmap.dispose();
	}

	private void autoExec() {
		File autoExecFile = new File(ExpoLogger.getLocalPath() + File.separator + "autoExec.txt");

		if(autoExecFile.exists()) {
			log("autoExec.txt found, attempting to execute...");

			try {
				for(String line : Files.readAllLines(autoExecFile.toPath())) {
					log("[autoExec] " + line);
					GameConsole.get().addConsoleMessage(new ConsoleMessage(line, true));
				}
			} catch (IOException e) {
				log("Failed to read autoExec.txt, ignoring...");
				e.printStackTrace();
			}
		} else {
			log("No autoExec.txt found, ignoring...");
		}
	}

	private void disposeScreen(AbstractScreen screen) {
		log("dispose() invoked for '" + screen.getScreenName() + "'");
		screen.dispose();
	}

	public void loadItemMapperTextures() {
		// load item render
		for(ItemMapping map : ItemMapper.get().getItemMappings()) {
			map.color = Color.valueOf(map.displayNameColor);
			map.uiRender.setTextureRegion(ExpoAssets.get().textureRegion(map.uiRender.texture));
			map.heldRender.setTextureRegion(ExpoAssets.get().textureRegion(map.heldRender.texture));
			if(map.armorRender != null) map.armorRender.setTextureRegion(ExpoAssets.get().textureRegion(map.armorRender.texture));
		}
	}

	public boolean isPlaying() {
		return activeScreen.getScreenName().equals(ClientStatic.SCREEN_GAME);
	}

	public ImGuiExpo getImGuiExpo() {
		return imGuiExpo;
	}

	public static Expo get() {
		return INSTANCE;
	}

}
