package dev.michey.expo.screen;

import com.badlogic.gdx.utils.ScreenUtils;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.ui.menu.BaseMenu;
import dev.michey.expo.render.ui.menu.MenuGroup;
import dev.michey.expo.util.ClientStatic;

public class MenuScreen extends AbstractScreen {

    private static MenuScreen INSTANCE;

    // Active menu group
    private MenuGroup activeGroup;

    public MenuScreen() {
        BaseMenu baseMenu = new BaseMenu();
        setActiveGroup(baseMenu);
        INSTANCE = this;
    }

    public static MenuScreen get() {
        return INSTANCE;
    }

    public void setActiveGroup(MenuGroup group) {
        activeGroup = group;
    }

    @Override
    public void render() {
        if(activeGroup != null) {
            ScreenUtils.clear(0.05f, 0.05f, 0.05f, 1.0f);

            RenderContext.get().hudBatch.begin();

            activeGroup.draw(RenderContext.get());

            RenderContext.get().hudBatch.end();
        }
    }

    @Override
    public void resize(int width, int height) {
        if(activeGroup != null) {
            activeGroup.update(RenderContext.get());
        }
    }

    public void handleClick(int button) {
        if(activeGroup != null) {
            activeGroup.handleInput(button == 0, button == 1, button == 2);
        }
    }

    @Override
    public String getScreenName() {
        return ClientStatic.SCREEN_MENU;
    }

}