package dev.michey.expo.render.ui.menu;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.michey.expo.assets.ExpoAssets;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.util.ClientStatic;
import dev.michey.expo.util.GameSettings;

public class BaseMenu extends MenuGroup {

    private MenuButton buttonSingleplayer;
    private MenuButton buttonMultiplayer;
    private MenuButton buttonOptions;
    private MenuButton buttonExit;

    private TextureRegion placeholderLogo;

    public BaseMenu() {
        placeholderLogo = ExpoAssets.get().textureRegion("placeholder_logo");

        setProperty("buttonWidth", 250);
        setProperty("buttonHeight", 30);
        setProperty("padding", 4);
        setProperty("buttonSpacingY", 25);

        RenderContext r = RenderContext.get();
        int scaling = GameSettings.get().uiScale;

        int totalWidth = getPropertyAsInt("buttonWidth") + getPropertyAsInt("padding") * 2 +
                r.buttonParts[0].getRegionWidth() * scaling * 2;
        int _totalHeight = getPropertyAsInt("buttonHeight") + getPropertyAsInt("padding") * 2 +
                r.buttonParts[0].getRegionHeight() * scaling + r.buttonParts[3].getRegionHeight() * scaling;
        int centerStartX = (Gdx.graphics.getWidth() - totalWidth) / 2;
        int centerStartY = (Gdx.graphics.getHeight() - _totalHeight * 4 - (3 * getPropertyAsInt("buttonSpacingY"))) / 2;

        setProperty("buttonFullWidth", totalWidth);
        setProperty("buttonFullHeight", _totalHeight);

        setProperty("centerStartX", centerStartX);
        setProperty("centerStartY", centerStartY);

        buttonSingleplayer = new MenuButton(this, "ui.menu.singleplayer") {
            @Override
            public void update(RenderContext r) {
                x = getPropertyAsInt("centerStartX");
                y = getPropertyAsInt("centerStartY") + 3 * (getPropertyAsInt("buttonSpacingY") + _totalHeight);
            }
        };
        buttonMultiplayer = new MenuButton(this, "ui.menu.multiplayer") {
            @Override
            public void update(RenderContext r) {
                x = getPropertyAsInt("centerStartX");
                y = getPropertyAsInt("centerStartY") + 2 * (getPropertyAsInt("buttonSpacingY") + _totalHeight);
            }
        };
        buttonOptions = new MenuButton(this, "ui.menu.settings") {
            @Override
            public void update(RenderContext r) {
                x = getPropertyAsInt("centerStartX");
                y = getPropertyAsInt("centerStartY") + (getPropertyAsInt("buttonSpacingY") + _totalHeight);
            }
        };
        buttonExit = new MenuButton(this, "ui.menu.exit") {
            @Override
            public void update(RenderContext r) {
                x = getPropertyAsInt("centerStartX");
                y = getPropertyAsInt("centerStartY");
            }

            @Override
            public void onClick(boolean left, boolean middle, boolean right) {
                if(left) {
                    Gdx.app.exit();
                }
            }
        };
    }

    @Override
    public void handleInput(boolean left, boolean right, boolean middle) {
        if(buttonSingleplayer.hovered) buttonSingleplayer.onClick(left, right, middle);
        if(buttonMultiplayer.hovered) buttonMultiplayer.onClick(left, right, middle);
        if(buttonOptions.hovered) buttonOptions.onClick(left, right, middle);
        if(buttonExit.hovered) buttonExit.onClick(left, right, middle);
    }

    @Override
    public void update(RenderContext r) {
        int scaling = GameSettings.get().uiScale;
        setProperty("buttonWidth", 125 * scaling);
        setProperty("buttonHeight", 15 * scaling);
        setProperty("buttonSpacingY", (int) (12.5f * scaling));
        setProperty("padding", scaling * 2);

        int totalWidth = getPropertyAsInt("buttonWidth") + getPropertyAsInt("padding") * 2
                + r.buttonParts[0].getRegionWidth() * scaling * 2;
        int _totalHeight = getPropertyAsInt("buttonHeight") + getPropertyAsInt("padding") * 2
                + r.buttonParts[0].getRegionHeight() * scaling + r.buttonParts[3].getRegionHeight() * scaling;
        int centerStartX = (Gdx.graphics.getWidth() - totalWidth) / 2;
        int centerStartY = (Gdx.graphics.getHeight() - _totalHeight * 4 - (3 * getPropertyAsInt("buttonSpacingY"))) / 2;

        setProperty("buttonFullWidth", totalWidth);
        setProperty("buttonFullHeight", _totalHeight);

        setProperty("centerStartX", centerStartX);
        setProperty("centerStartY", centerStartY);

        buttonSingleplayer.update(r);
        buttonMultiplayer.update(r);
        buttonOptions.update(r);
        buttonExit.update(r);
    }

    @Override
    public void draw(RenderContext r) {
        buttonSingleplayer.draw(r);
        buttonMultiplayer.draw(r);
        buttonOptions.draw(r);
        buttonExit.draw(r);

        int scaling = GameSettings.get().uiScale;
        float logoW = placeholderLogo.getRegionWidth() * scaling;
        float logoH = placeholderLogo.getRegionHeight() * scaling;

        float _y = getPropertyAsInt("centerStartY") + 3 * getPropertyAsInt("buttonSpacingY") + 4 * getPropertyAsInt("buttonFullHeight");
        r.hudBatch.draw(placeholderLogo, (Gdx.graphics.getWidth() - logoW) / 2, Math.abs((Gdx.graphics.getHeight() - (Gdx.graphics.getHeight() - _y + logoH) / 2)), logoW, logoH);

        r.globalGlyph.setText(r.m5x7_border_all[0], "v" + ClientStatic.GAME_VERSION);
        int spacing = 4 * scaling;
        r.m5x7_border_all[0].draw(r.hudBatch, "v" + ClientStatic.GAME_VERSION, Gdx.graphics.getWidth() - r.globalGlyph.width - spacing, r.globalGlyph.height + spacing);
    }

}
