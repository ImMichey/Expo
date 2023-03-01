package dev.michey.expo.render.camera;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.logic.entity.ClientPlayer;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.util.InputUtils;

import static dev.michey.expo.log.ExpoLogger.log;

public class ExpoCamera {

    public OrthographicCamera camera;
    public Viewport viewport;

    public Vector2 movementPosition;
    public Vector2 mousePosition;

    public float goalZoom;
    public float startZoom;
    public float zoomDelta;
    public boolean doZoomAnimation;
    private final float ZOOM_SPEED = 5.0f;

    public ExpoCamera() {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        camera = new OrthographicCamera(100, 100 * (h / w));

        camera.zoom = 1f / 3f;
        camera.position.set(camera.viewportWidth / 2, camera.viewportHeight / 2, 0);
        camera.update();

        viewport = new ScreenViewport(camera);
        viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
    }

    public void resize(int width, int height) {
        log("Resizing ExpoCamera to " + width + "x" + height);

        // Update game camera
        viewport.update(width, height, true);

        resetLerp();

        ClientPlayer p = ClientPlayer.getLocalPlayer();

        if(p != null) {
            centerToEntity(p);
        }
    }

    public void resetLerp() {
        movementPosition = null;
        mousePosition = null;
    }

    public void tick() {
        if(Gdx.graphics.getWidth() == 0 || Gdx.graphics.getHeight() == 0) {
            resetLerp();
            return;
        }

        ClientPlayer p = ClientPlayer.getLocalPlayer();
        if(p == null) return;

        if(doZoomAnimation) {
            zoomDelta += RenderContext.get().delta * ZOOM_SPEED;

            if(zoomDelta >= 1.0f) {
                zoomDelta = 1.0f;
                doZoomAnimation = false;
            }

            float newZoom = Interpolation.pow5Out.apply(startZoom, goalZoom, zoomDelta);
            newZoom = (float) Math.round(newZoom * 100) / 100;
            RenderContext.get().expoCamera.camera.zoom = newZoom;
        }

        cameraLerpTowards(p.toVisualCenterX(), p.toVisualCenterY());
        cameraLerpMouse();

        if(Float.isNaN(camera.position.x)) {
            centerToEntity(p);
        }

        camera.update();
    }

    public void centerToEntity(ClientEntity entity) {
        center(entity.toVisualCenterX(), entity.toVisualCenterY());
    }

    public void center(float x, float y) {
        camera.position.set(x, y, 0);
    }

    private void cameraLerpTowards(float x, float y) {
        final float LERP_FACTOR = 5f;

        // All these values are not actual camera values, they're "movement lerp" values being calculated
        if(movementPosition == null) movementPosition = new Vector2(camera.position.x, camera.position.y);

        float dstX = x - movementPosition.x;
        float dstY = y - movementPosition.y;

        float movementCameraX = dstX;
        float movementCameraY = dstY;

        if(Math.abs(dstX) > 0.003f) movementCameraX *= LERP_FACTOR * RenderContext.get().delta;
        if(Math.abs(dstY) > 0.003f) movementCameraY *= LERP_FACTOR * RenderContext.get().delta * RenderContext.get().aspectRatio;

        movementPosition.add(movementCameraX, movementCameraY);

        // Update actual camera with calculated values
        camera.position.x += movementCameraX;
        camera.position.y += movementCameraY;
    }

    private void cameraLerpMouse() {
        final float LERP_FACTOR = 5f;
        final float MOUSE_MOVEMENT_STRENGTH = 10f;
        float mouseX = InputUtils.getMouseOnScreenXNegPos() * MOUSE_MOVEMENT_STRENGTH;
        float mouseY = InputUtils.getMouseOnScreenYNegPos() * MOUSE_MOVEMENT_STRENGTH;

        if(mousePosition == null) mousePosition = new Vector2(0, 0);

        float mouseCameraX = (mouseX - mousePosition.x) * LERP_FACTOR * RenderContext.get().delta;
        float mouseCameraY = (mouseY - mousePosition.y) * LERP_FACTOR * RenderContext.get().delta * RenderContext.get().aspectRatio;

        boolean addX = Math.abs(mouseCameraX) > 0.003f;
        boolean addY = Math.abs(mouseCameraY / RenderContext.get().aspectRatioInverse) > 0.003f;

        mousePosition.add(addX ? mouseCameraX : 0, addY ? mouseCameraY : 0);

        // Update actual camera with calculated values
        if(addX) camera.position.x += mouseCameraX;
        if(addY) camera.position.y += mouseCameraY;
    }

    public void update() {
        RenderContext.get().batch.setProjectionMatrix(camera.combined);
        RenderContext.get().arraySpriteBatch.setProjectionMatrix(camera.combined);
        RenderContext.get().chunkRenderer.setProjectionMatrix(camera.combined);
        RenderContext.get().lightEngine.updateCamera();
    }

    public void addZoomAnimation(float amount) {
        if(doZoomAnimation) {
            startZoom = camera.zoom;
            goalZoom = goalZoom + amount;
        } else {
            startZoom = camera.zoom;
            goalZoom = startZoom + amount;
            doZoomAnimation = true;
        }

        zoomDelta = 0f;
    }

}
