package dev.michey.expo.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.arch.ClientEntityManager;
import dev.michey.expo.logic.world.chunk.ClientChunk;
import dev.michey.expo.render.arraybatch.ArrayTextureSpriteBatch;
import dev.michey.expo.render.camera.ExpoCamera;
import dev.michey.expo.render.light.ExpoLightEngine;
import dev.michey.expo.server.util.GenerationUtils;
import dev.michey.expo.util.ExpoShared;
import dev.michey.expo.util.GameSettings;
import dev.michey.expo.util.InputUtils;

import static dev.michey.expo.log.ExpoLogger.log;
import static dev.michey.expo.util.ExpoShared.ROW_TILES;

public class RenderContext {

    /** Singleton */
    private static RenderContext INSTANCE;

    /** Frame data */
    public long frameId;
    public float deltaTotal;
    public float delta;
    public float deltaMultiplier = 1.0f;
    public float deltaOneSecond;
    public float aspectRatio;
    public float aspectRatioInverse;
    public float drawStartX;
    public float drawEndX;
    public float drawStartY;
    public float drawEndY;

    /** User data */
    public float mouseRotation;         // Mouse rotation in angles. [0 - 360]
    public float mouseX;                // Mouse x position in game window. [0 - WindowWidth]
    public float mouseY;                // Mouse y position in game window. [0 - WindowHeight]
    public float mouseWorldX;           // Mouse x position in game world.
    public float mouseWorldY;           // Mouse y position in game world.
    public int mouseTileX;              // Mouse world tile x position in TILE UNITS.
    public int mouseTileY;              // Mouse world tile y position in TILE UNITS.
    public float mouseWorldGridX;       // Mouse world tile x position in game world.
    public float mouseWorldGridY;       // Mouse world tile y position in game world.
    public int mouseChunkX;             // Mouse world chunk x position in CHUNK UNITS.
    public int mouseChunkY;             // Mouse world chunk y position in CHUNK UNITS.
    public int mouseRelativeTileX;      // Mouse tile x position relative to the start of the chunk. [0 - 7]
    public int mouseRelativeTileY;      // Mouse tile y position relative to the start of the chunk. [0 - 7]
    public int mouseTileArray;          // Mouse tile array position. [0 - 63]
    public int mouseDirection;          // Mouse direction in game window. [0 = Left, 1 = Right]
    public boolean mouseMoved;          // If the mouse moved between frames.
    public int cameraX;
    public int cameraY;
    public double mousePlayerAngle;

    /** Water data */
    public Texture waterNoiseTexture;
    public Texture displacementTexture;
    public float waterSpeed = 0.4f;
    public float brightness = 0.5f;
    public float contrast = 0.5f;
    public float[] waterColor = new float[] {0.0f, 194f / 255f, 1.0f};
    public float waterDelta;
    public float waterAlpha = 0.17f;

    /** Shaders */
    public ShaderProgram DEFAULT_GLES3_SHADER;          // Should be used by all regular batches.
    public ShaderProgram DEFAULT_GLES3_ARRAY_SHADER;    // Should be used by all array batches.
    public ShaderProgram vignetteShader;
    public ShaderProgram itemShineShader;
    public ShaderProgram blurShader;
    public ShaderProgram waterShader;
    public ShaderProgram outlineShader;
    public ShaderProgram grassShader;

    /** Light engine */
    public ExpoLightEngine lightEngine;

    /** Render helpers */
    public SpriteBatch batch;                           // Game world batch
    public PolygonTileBatch polygonTileBatch;           // Game world batch
    public ShapeRenderer chunkRenderer;                 // Game world batch
    public ArrayTextureSpriteBatch arraySpriteBatch;    // Game world batch
    public SpriteBatch hudBatch;                        // HUD batch
    public TextureRegion square;

    /** Fonts */
    public BitmapFont[] m5x7_all;
    public BitmapFont m5x7_base;
    public BitmapFont[] m5x7_border_all;
    public BitmapFont m5x7_bordered;
    public BitmapFont[] m5x7_shadow_all;
    public BitmapFont m5x7_shadowed;

    public BitmapFont[] m6x11_all;
    public BitmapFont m6x11_base;
    public BitmapFont[] m6x11_border_all;
    public BitmapFont m6x11_bordered;

    public BitmapFont m5x7_use, m6x11_use, m5x7_border_use, m6x11_border_use, m5x7_shadow_use;

    /** Frame buffers */
    public int lastFBOWidth, lastFBOHeight;
    public FrameBuffer mainFbo;
    public FrameBuffer shadowFbo;
    public FrameBuffer waterReflectionFbo;
    public FrameBuffer waterTilesFbo;
    public FrameBuffer entityFbo;
    public FrameBuffer blurTargetAFbo;
    public FrameBuffer blurTargetBFbo;
    public FrameBuffer hudFbo;
    public FrameBuffer tilesFbo;

    /** Debug helpers */
    public boolean drawTileInfo = false;
    public boolean drawDebugHUD = true;
    public boolean drawImGui = true;
    public boolean drawShapes = false;
    public boolean drawHUD = true;

    /** ImGui Shader Helper */
    public float speed = 1.0f;
    public float minStrength = 0.05f;
    public float maxStrength = 0.01f;
    public float strengthScale = 100.0f;
    public float interval = 3.5f;
    public float detail = 1.0f;
    public float distortion = 0.0f;
    public float heightOffset = 0.0f;
    public float offset = 0.0f;
    public float skew = 0.0f;

    /** Numbers */
    private final TextureRegion[] numbers;
    private final TextureRegion[] numbersOutline;

    /** Game camera */
    public ExpoCamera expoCamera;

    /** Blur */
    public float blurStrength = 0.0f;
    public float blurDelta = 0.0f;
    public boolean blurActive = false;

    /** Menu textures */
    public TextureRegion[] buttonParts;
    public TextureRegion[] buttonPartsSelected;
    public GlyphLayout globalGlyph;

    /** Screenshots */
    public boolean queueScreenshot;

    public float gradientStartOffset;
    public float gradientMultiplier;

    public RenderContext() {
        batch = new SpriteBatch();
        polygonTileBatch = new PolygonTileBatch();
        hudBatch = new SpriteBatch();
        chunkRenderer = new ShapeRenderer();
        expoCamera = new ExpoCamera();
        arraySpriteBatch = new ArrayTextureSpriteBatch(8191, 2048, 2048, 32, GL30.GL_NEAREST, GL30.GL_NEAREST);
        globalGlyph = new GlyphLayout();

        {
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/Habbo.ttf"));
            FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            m5x7_all = new BitmapFont[5];
            m5x7_border_all = new BitmapFont[5];
            m5x7_shadow_all = new BitmapFont[5];

            for(int i = 0; i < 5; i++) {
                parameter.size = 16 + i * 16;
                m5x7_all[i] = generator.generateFont(parameter);
                m5x7_all[i].getData().markupEnabled = true;
                m5x7_all[i].getData().setLineHeight(11 * (i + 1));
                if(i == 0) m5x7_base = m5x7_all[i];
            }

            parameter.shadowOffsetX = 1;
            parameter.shadowOffsetY = 1;
            parameter.shadowColor = new Color(0f, 0f, 0f, 1.0f);

            for(int i = 0; i < 5; i++) {
                parameter.size = 16 + i * 16;
                m5x7_shadow_all[i] = generator.generateFont(parameter);
                m5x7_shadow_all[i].getData().markupEnabled = true;
                m5x7_shadow_all[i].getData().setLineHeight(11 * (i + 1));
                if(i == 0) m5x7_shadowed = m5x7_shadow_all[i];
            }

            parameter.shadowOffsetX = 0;
            parameter.shadowOffsetY = 0;
            parameter.borderWidth = 1;
            parameter.borderColor = new Color(48f / 255f, 48f / 255f, 48f / 255f, 0.6f);
            parameter.spaceX = -1;

            for(int i = 0; i < 5; i++) {
                parameter.size = 16 + i * 16;
                m5x7_border_all[i] = generator.generateFont(parameter);
                m5x7_border_all[i].getData().markupEnabled = true;
                m5x7_border_all[i].getData().setLineHeight(11 * (i + 1));
                if(i == 0) m5x7_bordered = m5x7_border_all[i];
            }

            generator.dispose();
        }
        {
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/m6x11.ttf"));
            FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            m6x11_all = new BitmapFont[5];
            m6x11_border_all = new BitmapFont[5];

            for(int i = 0; i < 5; i++) {
                parameter.size = 16 + i * 16;
                m6x11_all[i] = generator.generateFont(parameter);
                m6x11_all[i].getData().markupEnabled = true;
                if(i == 0) m6x11_base = m6x11_all[i];
            }

            parameter.borderWidth = 1;
            parameter.borderColor = new Color(48f / 255f, 48f / 255f, 48f / 255f, 0.6f);
            parameter.spaceX = -1;

            for(int i = 0; i < 5; i++) {
                parameter.size = 16 + i * 16;
                m6x11_border_all[i] = generator.generateFont(parameter);
                if(i == 0) m6x11_bordered = m6x11_border_all[i];
            }

            generator.dispose();
        }

        updatePreferredFonts(GameSettings.get().uiScale);

        waterNoiseTexture = ExpoAssets.get().texture("water/waternoise512.png");
        waterNoiseTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

        displacementTexture = ExpoAssets.get().texture("water/displacementmap.png");
        displacementTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

        numbers = new TextureRegion[10];
        TextureRegion baseTexture = ExpoAssets.get().textureRegion("numbers");

        square = ExpoAssets.get().findTile("square16x16");

        for(int i = 0; i < 10; i++) {
            numbers[i] = new TextureRegion(baseTexture, i * 8, 0, 6, 8);
        }

        numbersOutline = new TextureRegion[10];
        TextureRegion baseTexture2 = ExpoAssets.get().textureRegion("numbersOutline");

        for(int i = 0; i < 10; i++) {
            numbersOutline[i] = new TextureRegion(baseTexture2, i * 8, 0, 7, 9);
        }

        TextureRegion baseButton = ExpoAssets.get().textureRegion("ui_button_patches");
        buttonParts = new TextureRegion[7];
        buttonParts[0] = new TextureRegion(baseButton, 0, 0, 2, 2);
        buttonParts[1] = new TextureRegion(baseButton, 3, 0, 1, 2);
        buttonParts[2] = new TextureRegion(baseButton, 5, 0, 2, 2);
        buttonParts[3] = new TextureRegion(baseButton, 0, 3, 2, 4);
        buttonParts[4] = new TextureRegion(baseButton, 3, 3, 1, 4);
        buttonParts[5] = new TextureRegion(baseButton, 5, 3, 2, 4);
        buttonParts[6] = new TextureRegion(baseButton, 8, 0, 1, 1);

        TextureRegion baseButtonSel = ExpoAssets.get().textureRegion("ui_button_patches_sel");
        buttonPartsSelected = new TextureRegion[7];
        buttonPartsSelected[0] = new TextureRegion(baseButtonSel, 0, 0, 2, 2);
        buttonPartsSelected[1] = new TextureRegion(baseButtonSel, 3, 0, 1, 2);
        buttonPartsSelected[2] = new TextureRegion(baseButtonSel, 5, 0, 2, 2);
        buttonPartsSelected[3] = new TextureRegion(baseButtonSel, 0, 3, 2, 4);
        buttonPartsSelected[4] = new TextureRegion(baseButtonSel, 3, 3, 1, 4);
        buttonPartsSelected[5] = new TextureRegion(baseButtonSel, 5, 3, 2, 4);
        buttonPartsSelected[6] = new TextureRegion(baseButtonSel, 8, 0, 1, 1);

        DEFAULT_GLES3_SHADER = compileShader("gl3/base/default_gl3");
        DEFAULT_GLES3_ARRAY_SHADER = compileShader("gl3/base/default_array");
        vignetteShader = compileShader("gl3/vignette");
        itemShineShader = compileShader("gl3/itemshine");
        blurShader = compileShader("gl3/blur");
        waterShader = compileShader("gl3/water");
        outlineShader = compileShader("gl3/outline");
        grassShader = compileShader("gl3/grass");

        batch.setShader(DEFAULT_GLES3_SHADER);
        lightEngine = new ExpoLightEngine();

        INSTANCE = this;
    }

    /** Base update method to update all important timers. */
    public void update() {
        frameId++;
        delta = Gdx.graphics.getDeltaTime() * deltaMultiplier;
        deltaTotal += delta;
        deltaOneSecond += delta;
        aspectRatio = Gdx.graphics.getWidth() / (float) Gdx.graphics.getHeight();
        aspectRatioInverse = Gdx.graphics.getHeight() / (float) Gdx.graphics.getWidth();

        if(deltaOneSecond >= 1.0f) {
            deltaOneSecond -= 1.0f;
        }

        mouseRotation = (float) Math.toDegrees(Math.atan2(Gdx.input.getX() - Gdx.graphics.getWidth() / (float) 2, Gdx.input.getY() - Gdx.graphics.getHeight() / (float) 2));

        float oldMouseX = mouseX;
        float oldMouseY = mouseY;

        mouseX = Gdx.input.getX();
        mouseY = Math.abs(Gdx.input.getY() - Gdx.graphics.getHeight());

        float oldCameraX = cameraX;
        float oldCameraY = cameraY;

        cameraX = (int) expoCamera.camera.position.x;
        cameraY = (int) expoCamera.camera.position.y;

        mouseMoved = (oldMouseX != mouseX) || (oldMouseY != mouseY) || (oldCameraX != cameraX) || (oldCameraY != cameraY);

        if(mouseMoved) {
            mouseWorldX = InputUtils.getMouseWorldX();
            mouseWorldY = InputUtils.getMouseWorldY();

            mouseTileX = ExpoShared.posToTile(mouseWorldX);
            mouseTileY = ExpoShared.posToTile(mouseWorldY);

            mouseWorldGridX = ExpoShared.tileToPos(mouseTileX);
            mouseWorldGridY = ExpoShared.tileToPos(mouseTileY);

            mouseChunkX = ExpoShared.posToChunk(mouseWorldX);
            mouseChunkY = ExpoShared.posToChunk(mouseWorldY);

            int startTileX = ExpoShared.posToTile(ExpoShared.chunkToPos(mouseChunkX));
            int startTileY = ExpoShared.posToTile(ExpoShared.chunkToPos(mouseChunkY));

            mouseRelativeTileX = mouseTileX - startTileX;
            mouseRelativeTileY = mouseTileY - startTileY;

            mouseTileArray = mouseRelativeTileY * ROW_TILES + mouseRelativeTileX;

            ClientPlayer p = ClientPlayer.getLocalPlayer();

            if(p != null) {
                mousePlayerAngle = GenerationUtils.angleBetween360(p.playerReachCenterX, p.playerReachCenterY, mouseWorldX, mouseWorldY);
            }
        }

        drawStartX = InputUtils.getMouseWorldX(0);
        drawEndX = InputUtils.getMouseWorldX(Gdx.graphics.getWidth());
        drawStartY = InputUtils.getMouseWorldY(0);
        drawEndY = InputUtils.getMouseWorldY(Gdx.graphics.getHeight());

        mouseDirection = mouseX < (Gdx.graphics.getWidth() * 0.5f) ? 0 : 1;
    }

    public void reset() {
        batch.totalRenderCalls = 0;
        arraySpriteBatch.totalRenderCalls = 0;
        hudBatch.totalRenderCalls = 0;
    }

    private void createFBOs(int w, int h) {
        mainFbo = createFBO(w, h);
        shadowFbo = createFBO(w, h);
        waterReflectionFbo = createFBO(w, h);
        waterTilesFbo = createFBO(w, h);
        entityFbo = createFBO(w, h);
        blurTargetAFbo = createFBO(w, h);
        blurTargetBFbo = createFBO(w, h);
        hudFbo = createFBO(w, h);
        tilesFbo = createFBO(w, h);
    }

    public boolean inDrawBounds(ClientEntity entity) {
        float x = entity.finalTextureStartX;
        float y = entity.finalTextureStartY;

        return drawStartX <= (x + entity.textureWidth)
                && drawEndX >= x
                && drawStartY <= (y + entity.textureHeight)
                && drawEndY >= y;
    }

    public boolean verticesInBounds(float[] vertices) {
        return vertices[0] < drawEndX
                && vertices[2] > drawStartX
                && vertices[1] < drawEndY
                && vertices[3] > drawStartY;
    }

    public boolean inDrawBounds(ClientChunk chunk) {
        return drawStartX <= chunk.chunkDrawEndX && drawStartY <= chunk.chunkDrawEndY && drawEndX >= chunk.chunkDrawBeginX && drawEndY >= chunk.chunkDrawBeginY;
    }

    public void useRegularBatch() {
        if(arraySpriteBatch.isDrawing()) {
            arraySpriteBatch.end();
            batch.begin();
        } else if(!batch.isDrawing()) {
            batch.begin();
        }
    }

    public void useArrayBatch() {
        if(batch.isDrawing()) {
            batch.end();
            arraySpriteBatch.begin();
        } else if(!arraySpriteBatch.isDrawing()) {
            arraySpriteBatch.begin();
        }
    }

    public void useRegularArrayShader() {
        if(arraySpriteBatch.getShader() != DEFAULT_GLES3_ARRAY_SHADER) arraySpriteBatch.setShader(DEFAULT_GLES3_ARRAY_SHADER);
    }

    public void bindAndSetSelection(Batch useBatch) {
        bindAndSetSelection(useBatch, 2048, Color.WHITE, false);
    }

    public void bindAndSetSelection(Batch useBatch, float textureSize, Color c, boolean disableOutline) {
        float n = textureSize / expoCamera.camera.zoom;

        outlineShader.bind();
        outlineShader.setUniformi("u_outline", disableOutline ? 0 : 1);
        outlineShader.setUniformf("u_progress", ClientEntityManager.get().pulseProgress);
        outlineShader.setUniformf("u_pulseStrength", 1.2f);
        outlineShader.setUniformf("u_pulseMin", 1.0f);

        outlineShader.setUniformf("u_thickness", ClientEntityManager.get().pulseThickness);

        outlineShader.setUniformf("u_textureSize", n, n);
        outlineShader.setUniformf("u_outlineColor", c.r, c.g, c.b, ClientEntityManager.get().pulseProgress * 0.75f * c.a);

        useBatch.setShader(outlineShader);
        useBatch.begin();
    }

    private FrameBuffer createFBO(int width, int height) {
        try {
            log("Creating FBO with size " + width + "x" + height);
            FrameBuffer fbo = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);
            fbo.getColorBufferTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            return fbo;
        } catch (Exception e) {
            log("FATAL ERROR while trying to create frame buffer (" + width + "x" + height + "): " + e.getMessage());
        }

        return null;
    }

    public void updatePreferredFonts(int uiScale) {
        m5x7_use = m5x7_all[uiScale - 1];
        m6x11_use = m6x11_all[uiScale - 1];

        m5x7_border_use = m5x7_border_all[uiScale - 1];
        m6x11_border_use = m6x11_border_all[uiScale - 1];

        m5x7_shadow_use = m5x7_shadow_all[uiScale - 1];
    }

    private void disposeFBOs() {
        if(mainFbo == null) return;
        mainFbo.dispose();
        shadowFbo.dispose();
        waterReflectionFbo.dispose();
        waterTilesFbo.dispose();
        entityFbo.dispose();
        blurTargetAFbo.dispose();
        blurTargetBFbo.dispose();
        hudFbo.dispose();
        tilesFbo.dispose();
    }

    public void takeScreenshot() {
        queueScreenshot = true;
    }

    public void toggleFullscreen() {
        boolean fs = Gdx.graphics.isFullscreen();

        if(fs) {
            // Switch to windowed mode.
            Gdx.graphics.setUndecorated(false);
            Gdx.graphics.setWindowedMode(GameSettings.get().preferredWidth, GameSettings.get().preferredHeight);
        } else {
            // Switch to fullscreen mode.
            Gdx.graphics.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
        }
    }

    public void onResize(int width, int height) {
        if(lastFBOWidth != width || lastFBOHeight != height) {
            disposeFBOs();
            createFBOs(width, height);

            vignetteShader.bind();
            vignetteShader.setUniformf("u_resolution", Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

            lightEngine.resize(width, height);

            if(ExpoClientContainer.get() != null) ExpoClientContainer.get().getPlayerUI().onResize();
        }

        lastFBOWidth = width;
        lastFBOHeight = height;
    }

    public TextureRegion getNumber(int number) {
        return numbers[number];
    }

    public TextureRegion getNumberOutline(int number) {
        return numbersOutline[number];
    }

    private ShaderProgram compileShader(String key) {
        log("Compiling shader " + key);
        String vertex = Gdx.files.internal("shaders/" + key + ".vsh").readString();
        String fragment = Gdx.files.internal("shaders/" + key + ".fsh").readString();
        ShaderProgram shader = new ShaderProgram(vertex, fragment);

        if(!shader.isCompiled()) log("Error while compiling shader " + key + " - Shader Error: " + shader.getLog());
        if(shader.getLog().length() != 0) log("Warning while compiling shader " + key + " - Shader Warnings: " + shader.getLog());

        return shader;
    }

    public static RenderContext get() {
        return INSTANCE;
    }

}

