package dev.michey.expo.render.shadow;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.logic.world.ClientWorld;

import static dev.michey.expo.util.ClientStatic.DEV_MODE;

public class ShadowUtils {

    public static final Affine2 SHEARING_AFFINE = new Affine2();
    public static final Affine2 OFFSET_AFFINE = new Affine2();
    public static final Affine2 ROTATION_AFFINE = new Affine2();
    public static final Affine2 FINAL_AFFINE = new Affine2();

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

        SHEARING_AFFINE.setToShearing(shearX, shearY);
        OFFSET_AFFINE.setToTranslation(offsetX, offsetY);
        ROTATION_AFFINE.idt().translate(originX, originY).rotate(rotation).translate(-originX, -originY).preMul(OFFSET_AFFINE);
        FINAL_AFFINE.idt().preMul(ROTATION_AFFINE);

        if(!Gdx.input.isKeyPressed(Input.Keys.U) || !DEV_MODE) {
            FINAL_AFFINE.preMul(SHEARING_AFFINE).preScale(scaleX, scaleY);
        }

        //Affine2 shearingAffine = new Affine2();
        //shearingAffine.setToShearing(shearX, shearY);

        //Affine2 offsetAffine = new Affine2();
        //offsetAffine.setToTranslation(offsetX, offsetY);

        /*
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
        */

        return new Affine2(FINAL_AFFINE.preTranslate(worldX, worldY));
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

    public static float getWind(float maxStrength, float minStrength, float time, float interval, float detail) {
        double diff = Math.pow(maxStrength - minStrength, 2.0);
        double strength = MathUtils.clamp(minStrength + diff + MathUtils.sin(time / interval) * diff, minStrength, maxStrength) * 100.0f;
        return (float) ((MathUtils.sin(time) + MathUtils.cos(time * detail)) * strength);
    }

}