package dev.michey.expo.render.camera;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import dev.michey.expo.audio.AudioEngine;
import dev.michey.expo.logic.entity.arch.ClientEntity;
import dev.michey.expo.logic.entity.player.ClientPlayer;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.util.ClientStatic;
import dev.michey.expo.util.GameSettings;
import dev.michey.expo.util.InputUtils;

import static dev.michey.expo.log.ExpoLogger.log;
import static dev.michey.expo.util.ClientStatic.DEV_MODE;

public class ExpoCamera {

    public OrthographicCamera camera;
    public Viewport viewport;

    public Vector2 movementPosition;
    public Vector2 mousePosition;

    public float goalZoom;
    public float startZoom;
    public float zoomDelta;
    public boolean doZoomAnimation;

    public int baseZoomLevelIndex;
    public int currentZoomLevelIndex;

    public ExpoCamera() {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        baseZoomLevelIndex = ClientStatic.DEFAULT_CAMERA_ZOOM_INDEX;
        currentZoomLevelIndex = baseZoomLevelIndex;

        camera = new OrthographicCamera(100, 100 * (h / w));

        camera.zoom = ClientStatic.DEFAULT_CAMERA_ZOOM;
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
            centerToPlayer(p);
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
            float ZOOM_SPEED = 5.0f;
            zoomDelta += RenderContext.get().delta * ZOOM_SPEED;

            if(zoomDelta >= 1.0f) {
                zoomDelta = 1.0f;
                doZoomAnimation = false;
            }

            float newZoom = Interpolation.pow5Out.apply(startZoom, goalZoom, zoomDelta);
            newZoom = (float) Math.round(newZoom * 10000) / 10000;
            RenderContext.get().expoCamera.camera.zoom = newZoom;
            RenderContext.get().zoomNotify = true;
        }

        cameraLerpTowards(p.finalTextureCenterX, p.clientPosY + p.textureOffsetY + 13);

        cameraLerpMouse();
        cameraScreenShake();

        if(Float.isNaN(camera.position.x)) {
            centerToPlayer(p);
        }

        camera.update();
    }

    public void centerToPlayer(ClientPlayer player) {
        center(player.clientPosX, player.clientPosY + player.textureOffsetY + 13);
        resetLerp();
    }

    public void centerToEntity(ClientEntity entity) {
        center(entity.finalTextureCenterX, entity.finalTextureCenterY);
    }

    public void center(float x, float y) {
        camera.position.set(x, y, 0);
    }

    public void resetZoom() {
        if(camera.zoom != ClientStatic.DEFAULT_CAMERA_ZOOM) {
            AudioEngine.get().playSoundGroup("woosh", 0.125f);
        }
        camera.zoom = ClientStatic.DEFAULT_CAMERA_ZOOM;
        currentZoomLevelIndex = baseZoomLevelIndex;

        RenderContext.get().zoomNotify = true;
    }

    public void cycleZoom() {
        float currentZoomLevel = ClientStatic.CAMERA_ZOOM_LEVELS[currentZoomLevelIndex];

        currentZoomLevelIndex--;
        if(currentZoomLevelIndex < 0) {
            currentZoomLevelIndex = ClientStatic.CAMERA_ZOOM_LEVELS.length - 1;
        }

        float desiredZoomLevel = ClientStatic.CAMERA_ZOOM_LEVELS[currentZoomLevelIndex];

        addZoomAnimation(desiredZoomLevel - currentZoomLevel);
        AudioEngine.get().playSoundGroup("woosh", 0.125f);
    }

    private void cameraLerpTowards(float x, float y) {
        final float LERP_FACTOR = 5f;

        // All these values are not actual camera values, they're "movement lerp" values being calculated
        if(movementPosition == null) movementPosition = new Vector2(camera.position.x, camera.position.y);

        float dstX = x - movementPosition.x;
        float dstY = y - movementPosition.y;

        float movementCameraX = dstX;
        float movementCameraY = dstY;

        if(Math.abs(dstX) > 0.002f)
            movementCameraX *= LERP_FACTOR * RenderContext.get().delta;
        if(Math.abs(dstY) > 0.002f)
            movementCameraY *= LERP_FACTOR * RenderContext.get().delta * RenderContext.get().aspectRatio;

        movementPosition.add(movementCameraX, movementCameraY);

        // Update actual camera with calculated values
        camera.position.x += movementCameraX;
        camera.position.y += movementCameraY;
    }

    private void cameraScreenShake() {
        CameraShake.tick(RenderContext.get().delta);

        if(GameSettings.get().enableScreenshake) {
            Vector2 oldPos = CameraShake.getOldPos();
            Vector2 currentPos = CameraShake.getCurrentPos();

            camera.position.x -= oldPos.x;
            camera.position.y -= oldPos.y;

            camera.position.x += currentPos.x;
            camera.position.y += currentPos.y;
        }
    }

    private void cameraLerpMouse() {
        final float LERP_FACTOR = 7.5f;
        final float MOUSE_MOVEMENT_STRENGTH = 15f;
        float mouseX = InputUtils.getMouseOnScreenXNegPos() * MOUSE_MOVEMENT_STRENGTH;
        float mouseY = InputUtils.getMouseOnScreenYNegPos() * MOUSE_MOVEMENT_STRENGTH;

        if(mousePosition == null) mousePosition = new Vector2(0, 0);

        float mouseCameraX = (mouseX - mousePosition.x) * LERP_FACTOR * RenderContext.get().delta;
        float mouseCameraY = (mouseY - mousePosition.y) * LERP_FACTOR * RenderContext.get().delta / RenderContext.get().aspectRatio;

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
        RenderContext.get().polygonTileBatch.setProjectionMatrix(camera.combined);
        RenderContext.get().chunkRenderer.setProjectionMatrix(camera.combined);
        RenderContext.get().aoBatch.setProjectionMatrix(camera.combined);
        RenderContext.get().lightEngine.updateCamera();
    }

    public boolean addZoomAnimation(float amount) {
        RenderContext.get().zoomNotify = true;
        float lowerFloor = 0.1f;
        boolean change = true;

        if(doZoomAnimation) {
            startZoom = camera.zoom;
            goalZoom = goalZoom + amount;

            if(!DEV_MODE && goalZoom < lowerFloor) {
                goalZoom = lowerFloor;

                if(startZoom == goalZoom) {
                    change = false;
                }
            }
        } else {
            startZoom = camera.zoom;
            goalZoom = startZoom + amount;
            doZoomAnimation = true;

            if(!DEV_MODE && goalZoom < lowerFloor) {
                goalZoom = lowerFloor;

                if(startZoom == goalZoom) {
                    change = false;
                }
            }
        }

        zoomDelta = 0f;
        return change;
    }

}
