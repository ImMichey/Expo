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
import dev.michey.expo.assets.TileMergerX;
import dev.michey.expo.debug.DebugGL;
import dev.michey.expo.noise.TileLayerType;
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
import dev.michey.expo.util.GameSettings;
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

	/** Profiler */
	private DebugGL debugGL;

	/** GameSettings */
	private final GameSettings gameSettings;

	public Expo(GameSettings gameSettings) {
		// Enable logging to file + console for debugging
		if(!DEV_MODE) ExpoLogger.enableDualLogging("clientlogs");
		inactiveScreens = new HashMap<>();
		this.gameSettings = gameSettings;
	}

	@Override
	public void create() {
		if(DEV_MODE || gameSettings.enableDebugImGui) {
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

		if(gameSettings.enableDebugGL) {
			debugGL = new DebugGL();
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
		sliceAndPatch();
	}

	private void sliceAndPatch() {
		boolean slice = false;

		if(slice && DEV_MODE) {
			ExpoAssets.get().slice("tile_soil", true, 0, 0);
			ExpoAssets.get().slice("tile_grass", false, 0, 16);
			ExpoAssets.get().slice("tile_sand", false, 0, 48);
			ExpoAssets.get().slice("tile_not_set", true, 0, 80);
			ExpoAssets.get().slice("tile_ocean", false, 0, 96);
			ExpoAssets.get().slice("tile_ocean_deep", false, 0, 128);
			ExpoAssets.get().slice("tile_soil_hole", false, 0, 160);
			ExpoAssets.get().slice("tile_forest", false, 0, 192);
			ExpoAssets.get().slice("tile_desert", false, 0, 224);
			ExpoAssets.get().slice("tile_rock", false, 0, 256);
		}

		boolean patch = false;

		if(patch && DEV_MODE) {
			TileMergerX merger = new TileMergerX();
			merger.prepare();
			var possibilities = merger.createAllPossibleVariations();

			for(TileLayerType tlt : TileLayerType.values()) {
				int minTile = tlt.TILE_ID_DATA[0];
				if(tlt.TILE_ID_DATA[0] == -1) continue;
				if(tlt.TILE_ID_DATA.length == 1 && tlt.TILE_ID_DATA[0] != 0) continue;

				String elevationName = TileLayerType.ELEVATION_TEXTURE_MAP.get(tlt);

				for(int[] ids : possibilities.values()) {
					int[] newIds = new int[ids.length];

					for(int i = 0; i < newIds.length; i++) {
						newIds[i] = ids[i] + minTile;
					}

					if(elevationName == null) {
						merger.createFreshTile(newIds, null, -1);

						if(newIds.length == 1 && ExpoAssets.get().getTileSheet().hasVariation(newIds[0])) {
							int variations = ExpoAssets.get().getTileSheet().getAmountOfVariations(newIds[0]);

							for(int var = 0; var < variations; var++) {
								merger.createFreshTile(newIds, null, var);
							}
						}
					} else {
						for(int ev = 1; ev <= 4; ev++) {
							merger.createFreshTile(newIds, elevationName + "_" + ev, -1);
						}
					}
				}
			}
		}
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

		if(debugGL != null) debugGL.postRender();
		r.reset();
		if(debugGL != null) debugGL.preRender();
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
					if(line.length() == 0) continue;
					if(line.startsWith("#")) continue;
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
			map.heldRender.setTextureRegion(ExpoAssets.get().getItemSheet().get(map.heldRender.texture));
			map.uiRender.setTextureRegion(ExpoAssets.get().getItemSheet().get(map.uiRender.texture));
			if(map.armorRender != null) map.armorRender.setTextureRegion(ExpoAssets.get().getItemSheet().get(map.armorRender.texture));
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

	public AbstractScreen getActiveScreen() {
		return activeScreen;
	}

}
