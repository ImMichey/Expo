package dev.michey.expo;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Graphics;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.ScreenUtils;
import com.codedisaster.steamworks.*;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.assets.TileMergerV2;
import dev.michey.expo.audio.AudioEngine;
import dev.michey.expo.console.ConsoleMessage;
import dev.michey.expo.console.GameConsole;
import dev.michey.expo.debug.DebugGL;
import dev.michey.expo.input.GameInput;
import dev.michey.expo.lang.Lang;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.noise.TileLayerType;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.imgui.ImGuiExpo;
import dev.michey.expo.render.ui.PlayerUI;
import dev.michey.expo.render.ui.notification.UINotificationPiece;
import dev.michey.expo.screen.AbstractScreen;
import dev.michey.expo.screen.MenuScreen;
import dev.michey.expo.server.ServerLauncher;
import dev.michey.expo.server.main.arch.ExpoServerBase;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ArmorRender;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapper;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemMapping;
import dev.michey.expo.server.main.logic.inventory.item.mapping.ItemRender;
import dev.michey.expo.server.util.ExpoHardware;
import dev.michey.expo.server.util.ServerUtils;
import dev.michey.expo.steam.ExpoSteam;
import dev.michey.expo.steam.ExpoSteamCallbackThread;
import dev.michey.expo.steam.SteamLibraryLoaderGdx;
import dev.michey.expo.util.ClientUtils;
import dev.michey.expo.util.ExpoShared;
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
import static dev.michey.expo.util.ClientStatic.*;
import static dev.michey.expo.util.ExpoShared.FRAMES_PLAYER_ANIMATION_IDLE;
import static dev.michey.expo.util.ExpoShared.FRAMES_PLAYER_ANIMATION_WALK;

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

	/** Translations */
	private final Lang lang;

	public Expo(GameSettings gameSettings) {
		if(gameSettings.enableDebugMode) DEV_MODE = true;
		if(gameSettings.enablePerformanceProfiler) ExpoShared.TRACK_PERFORMANCE = true;

		if(gameSettings.zoomLevel == 0) {
			DEFAULT_CAMERA_ZOOM = 0.5f;
			DEFAULT_CAMERA_ZOOM_INDEX = 4;
		} else if(gameSettings.zoomLevel == 1) {
			DEFAULT_CAMERA_ZOOM = 1f / 3f;
			DEFAULT_CAMERA_ZOOM_INDEX = 3;
		} else if(gameSettings.zoomLevel == 2) {
			DEFAULT_CAMERA_ZOOM = 0.25f;
			DEFAULT_CAMERA_ZOOM_INDEX = 2;
		}

		// Enable logging to file + console for debugging
		if(!DEV_MODE) {
			ExpoLogger.enableDualLogging("clientlogs");
		}

		ServerLauncher.overrideKryoLogger();

		inactiveScreens = new HashMap<>();
		this.gameSettings = gameSettings;

		this.lang = new Lang();

		{
			// Find the max refresh rate for local server tick rate
			int proposedRefreshRate = 60;

			for(Graphics.DisplayMode dm : Lwjgl3ApplicationConfiguration.getDisplayModes()) {
				if(dm.refreshRate > proposedRefreshRate) {
					proposedRefreshRate = dm.refreshRate;
				}
			}

			this.gameSettings.maxTickRate = proposedRefreshRate;
			ExpoShared.DEFAULT_LOCAL_TICK_RATE = proposedRefreshRate;
			ExpoLogger.log("Using maxTickRate: " + proposedRefreshRate);
		}
	}

	@Override
	public void create() {
		ExpoHardware.dump();

		lang.load(gameSettings.language);
		lang.setActiveLangCode(gameSettings.language);

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

			FileHandle localFont = Gdx.files.local("Roboto-Regular.ttf");
			String customFont = localFont.file().getAbsolutePath();
			File checkForFont = new File(customFont);

			if(!checkForFont.exists()) {
				// Try to copy to local path.
				ExpoLogger.log("ImGui font file doesn't exist locally, trying to copy to local file system.");
				FileHandle fh = Gdx.files.internal("fonts/Roboto-Regular.ttf");
				fh.copyTo(localFont);
			}

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
		GameConsole.get().addSystemMessage("[CYAN]F1 = Console [WHITE]:: [CYAN]F6 = Hide HUD [WHITE]:: [CYAN]F10 = Screenshot [WHITE]:: [CYAN]F11 = Toggle Fullscreen");
		INSTANCE = this;

		GameConsole.get().addSystemMessage("Initializing Steam...");

		try {
			SteamAPI.loadLibraries(new SteamLibraryLoaderGdx());

			if(!SteamAPI.init()) {
				// Steamworks initialization error, e.g. Steam client not running
				GameConsole.get().addSystemErrorMessage("Failed to initialize Steamworks.");
			} else {
				STEAM_INITIALIZED = true;
				GameConsole.get().addSystemSuccessMessage("Loaded Steam API successfully.");

				STEAM_CALLBACK_THREAD = new ExpoSteamCallbackThread();
				STEAM_CALLBACK_THREAD.start();

				STEAM_USER = new SteamUser(ExpoSteam.callback);
				STEAM_FRIENDS = new SteamFriends(ExpoSteam.friendsCallback);
				STEAM_ACCOUNT_ID = STEAM_USER.getSteamID().getAccountID();
				STEAM_STEAM_ID = SteamID.getNativeHandle(STEAM_USER.getSteamID());
				PLAYER_USERNAME = STEAM_FRIENDS.getFriendPersonaName(STEAM_USER.getSteamID());

				GameConsole.get().addSystemSuccessMessage("Steam IDs: " + STEAM_ACCOUNT_ID + "/" + STEAM_STEAM_ID);
				GameConsole.get().addSystemSuccessMessage("Using name: " + PLAYER_USERNAME);
			}
		} catch (SteamException e) {
			// Error extracting or loading native libraries
			GameConsole.get().addSystemErrorMessage("Failed to load Steam native libraries.");
			e.printStackTrace();
		}

		autoExec();
	}

	public void sliceAndPatch() {
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
		ExpoAssets.get().slice("tile_oak_plank", false, 0, 288);
		ExpoAssets.get().slice("tile_soil_farmland", false, 0, 320);
		ExpoAssets.get().slice("tile_dirt", false, 0, 352);
		ExpoAssets.get().slice("tile_water_sandy", false, 0, 384);
		ExpoAssets.get().slice("tile_oakplankwall", false, 0, 416);
		ExpoAssets.get().slice("tile_water_overlay", false, 0, 448);
		ExpoAssets.get().slice("tile_sand_waterlogged", false, 0, 480);
		ExpoAssets.get().slice("tile_soil_deep_waterlogged", false, 0, 512);
		ExpoAssets.get().slice("tile_soil_waterlogged", false, 0, 544);
		ExpoAssets.get().slice("tile_hedge", false, 0, 576);

		TileMergerV2 merger = new TileMergerV2();
		merger.prepare();
		var possibilities = merger.createAllPossibleVariations();

		for(TileLayerType tlt : TileLayerType.values()) {
			int minTile = tlt.TILE_ID_DATA[0];
			if(tlt.TILE_ID_DATA[0] == -1) continue;

			String elevationName = TileLayerType.ELEVATION_TEXTURE_MAP.get(tlt);

			if(tlt.TILE_ID_DATA.length == 1) {
				merger.createFreshTile(new int[] {tlt.TILE_ID_DATA[0]}, null, -1, tlt.TILE_ID_DATA);

				if(ExpoAssets.get().getTileSheet().hasVariation(tlt.TILE_ID_DATA[0])) {
					int variations = ExpoAssets.get().getTileSheet().getAmountOfVariations(tlt.TILE_ID_DATA[0]);
					ExpoLogger.log("Variations for " + tlt.TILE_ID_DATA[0] + ": " + variations);

					for(int var = 0; var < variations; var++) {
						merger.createFreshTile(new int[] {tlt.TILE_ID_DATA[0]}, null, var, tlt.TILE_ID_DATA);
					}
				}
			} else {
				for(int[] ids : possibilities.values()) {
					int[] newIds = new int[ids.length];

					for(int i = 0; i < newIds.length; i++) {
						newIds[i] = ids[i] + minTile;
					}

					if(elevationName == null) {
						merger.createFreshTile(newIds, null, -1, ids);

						if(newIds.length == 1 && ExpoAssets.get().getTileSheet().hasVariation(newIds[0])) {
							int variations = ExpoAssets.get().getTileSheet().getAmountOfVariations(newIds[0]);
							ExpoLogger.log("(NE) Variations for " + newIds[0] + ": " + variations);

							for(int var = 0; var < variations; var++) {
								merger.createFreshTile(newIds, null, var, ids);
							}
						}
					} else {
						if(newIds.length == 1 && ExpoAssets.get().getTileSheet().hasVariation(newIds[0])) {
							int variations = ExpoAssets.get().getTileSheet().getAmountOfVariations(newIds[0]);
							ExpoLogger.log("(E) Variations for " + newIds[0] + ": " + variations);

							for(int ev = 1; ev <= 4; ev++) {
								for(int var = 0; var < variations; var++) {
									merger.createFreshTile(newIds, elevationName + "_" + ev, var, ids);
								}
							}
						} else {
							for(int ev = 1; ev <= 4; ev++) {
								merger.createFreshTile(newIds, elevationName + "_" + ev, -1, ids);
							}
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
			boolean shouldCreateFBOs = width > 0 && height > 0;

			if(shouldCreateFBOs) {
				activeScreen.resize(width, height);

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
		AudioEngine.get().tick(r.delta);
		ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1.0f);

		if(r.drawImGui && DEV_MODE) {
			imGuiGlfw.newFrame();
			ImGui.newFrame();
		}

		if(activeScreen != null) {
			activeScreen.render();
			if(ExpoShared.TRACK_PERFORMANCE) ServerUtils.dumpPerformanceMetrics();
		}

		GameConsole.get().draw();
		// DevHUD.get().draw();

		if(r.drawImGui && DEV_MODE) {
			imGuiExpo.draw();
			ImGui.render();
			imGuiGl3.renderDrawData(ImGui.getDrawData());
		}

		if(debugGL != null) debugGL.postRender();
		r.reset();
		if(debugGL != null) debugGL.preRender();

		if(r.queueScreenshot) {
			r.queueScreenshot = false;
			String fn = "screenshot-" + System.currentTimeMillis();
			ClientUtils.takeScreenshot(fn);

			if(PlayerUI.get() != null) {
				UINotificationPiece[] pieces = Lang.ntp("ui.notification.screenshot", fn + ".png");
				PlayerUI.get().addNotification(PlayerUI.get().playerTabHead, 5.0f, "crab_snip", pieces);
			}
		}
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

		if(STEAM_INITIALIZED) {
			STEAM_CALLBACK_THREAD.stopTask();
		}
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
		Pixmap pixmap = new Pixmap(Gdx.files.internal("textures/system/cursor_test4.png"));
		int xHotspot = pixmap.getWidth() / 2;
		int yHotspot = pixmap.getHeight() / 2;
		Cursor cursor = Gdx.graphics.newCursor(pixmap, xHotspot, yHotspot);
		Gdx.graphics.setCursor(cursor);
		pixmap.dispose();
	}

	private void autoExec() {
		File autoExecFile = new File(ExpoLogger.getLocalPath() + File.separator + "autoExec.txt");

		if(autoExecFile.exists()) {
			log("autoExec.txt found, attempting to execute with 50ms delay...");

			Gdx.app.postRunnable(() -> {
                try {
                    Thread.sleep(50L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

				try {
					for(String line : Files.readAllLines(autoExecFile.toPath())) {
						if(line.isEmpty() || line.startsWith("#")) {
							log("[autoExec] " + line + " ...IGNORED");
							continue;
						}
						log("[autoExec] " + line);
						GameConsole.get().addConsoleMessage(new ConsoleMessage(line, true));
					}
				} catch (IOException e) {
					log("Failed to read autoExec.txt, ignoring...");
					e.printStackTrace();
				}
            });
		} else {
			log("No autoExec.txt found, ignoring...");
		}
	}

	private void disposeScreen(AbstractScreen screen) {
		log("dispose() invoked for '" + screen.getScreenName() + "'");
		screen.dispose();
	}

	public void loadItemMapperTextures() {
		for(ItemMapping map : ItemMapper.get().getItemMappings()) {
			map.color = Color.valueOf(map.displayNameColor);
			for(ItemRender ir : map.heldRender) update(ir);
			for(ItemRender ir : map.uiRender) update(ir);
			if(map.thrownRender != null) for(ItemRender ir : map.thrownRender) update(ir);
		}
	}

	public void loadItemDisplayNames() {
		for(ItemMapping map : ItemMapper.get().getItemMappings()) {
			String iden = map.identifier;
			if(iden.startsWith("item_")) iden = iden.substring(5);
			map.displayName = Lang.str("item." + iden);
		}
	}

	public void updateArmor(ArmorRender ar) {
		ar.idleFrames = new TextureRegion[FRAMES_PLAYER_ANIMATION_IDLE];
		ar.walkFrames = new TextureRegion[FRAMES_PLAYER_ANIMATION_WALK];

		for(int i = 0; i < ar.idleFrames.length; i++) {
			ar.idleFrames[i] = ExpoAssets.get().textureRegionFresh(ar.textureIdle + "_" + (i + 1));
		}

		for(int i = 0; i < ar.walkFrames.length; i++) {
			ar.walkFrames[i] = ExpoAssets.get().textureRegionFresh(ar.textureWalk + "_" + (i + 1));
		}
	}

	private void update(ItemRender ir) {
		if(ir.animationFrames == 0) {
			ir.textureRegions = new TextureRegion[] {ExpoAssets.get().getItemSheet().get(ir.texture)};
		} else {
			ir.textureRegions = new TextureRegion[ir.animationFrames];

			for(int i = 0; i < ir.animationFrames; i++) {
				ir.textureRegions[i] = ExpoAssets.get().textureRegion(ir.texture + "_" + (i + 1));
			}
		}

		ir.useTextureRegion = ir.textureRegions[0];

		if(ir.useWidth == 0) ir.useWidth = ir.useTextureRegion.getRegionWidth() * ir.scaleX;
		if(ir.useHeight == 0) ir.useHeight = ir.useTextureRegion.getRegionHeight() * ir.scaleY;
	}

	public boolean isPlaying() {
		return activeScreen.getScreenName().equals(SCREEN_GAME);
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

    public boolean isMultiplayer() {
		return ExpoServerBase.get() == null;
    }

}
