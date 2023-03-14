package dev.michey.expo.render.shadow;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Affine2;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.logic.world.ClientWorld;

import static dev.michey.expo.util.ClientStatic.DEV_MODE;

public class ShadowUtils {

    /*
     *      Creates a custom Affine2 for shadows with rotation
     *
     *      x, y                --> position in world (usually the entity position)
     *      rotation            --> angle in degrees
     *      offsetX, offsetY    --> if the original sprite is drawn with an offset
     *      originX, originY    --> center of the sprite
     *      shearX, shearY      --> shear amount (usually day/night cycle shear values)
     *      scaleX, scaleY      --> scale amount (usually day/night cycle scale values)
     */
    public static Affine2 createAdvancedShadowAffine(float worldX, float worldY,
                                                     float rotation,
                                                     float offsetX, float offsetY,
                                                     float originX, float originY,
                                                     float shearX, float shearY,
                                                     float scaleX, float scaleY) {
        Affine2 shearingAffine = new Affine2();
        shearingAffine.setToShearing(shearX, shearY);

        Affine2 offsetAffine = new Affine2();
        offsetAffine.setToTranslation(offsetX, offsetY);

        Affine2 rotationAffine = new Affine2();
        rotationAffine.translate(originX, originY);
        rotationAffine.rotate(rotation);
        rotationAffine.translate(-originX, -originY);
        rotationAffine.preMul(offsetAffine);

        // Note: You could potentially stop after the rotationAffine and grab the vertices applied with that affine for wind entities

        Affine2 r = new Affine2().preMul(rotationAffine);

        if(!Gdx.input.isKeyPressed(Input.Keys.U) || !DEV_MODE) {
            r.preMul(shearingAffine).preScale(scaleX, scaleY);
        }

        return r.preTranslate(worldX, worldY);
    }

    public static Affine2 createSimpleShadowAffine(float worldX, float worldY) {
        ClientWorld world = ExpoClientContainer.get().getClientWorld();
        return createAdvancedShadowAffine(worldX, worldY, 0, 0, 0, 0, 0, world.worldSunShadowX, 0, 1.0f, world.worldSunShadowY);
    }

    public static Affine2 createSimpleShadowAffineInternalOffset(float worldX, float worldY, float offsetX, float offsetY) {
        ClientWorld world = ExpoClientContainer.get().getClientWorld();
        return createAdvancedShadowAffine(worldX, worldY, 0, offsetX, offsetY, 0, 0, world.worldSunShadowX, 0, 1.0f, world.worldSunShadowY);
    }

    public static Affine2 createSimpleShadowAffineInternalOffsetRotation(float worldX, float worldY, float offsetX, float offsetY, float originX, float originY, float rotation) {
        ClientWorld world = ExpoClientContainer.get().getClientWorld();
        return createAdvancedShadowAffine(worldX, worldY, rotation, offsetX, offsetY, originX, originY, world.worldSunShadowX, 0, 1.0f, world.worldSunShadowY);
    }

    public static Affine2 createSimpleShadowAffine(float worldX, float worldY, float x, float y) {
        return createAdvancedShadowAffine(worldX, worldY, 0, 0, 0, 0, 0, x, 0, 1.0f, y);
    }

}
