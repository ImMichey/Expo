package dev.michey.expo.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import dev.michey.expo.client.chat.ExpoClientChat;
import dev.michey.expo.console.GameConsole;
import dev.michey.expo.logic.container.ExpoClientContainer;
import dev.michey.expo.logic.inventory.PlayerInventory;
import dev.michey.expo.render.RenderContext;
import dev.michey.expo.render.ui.PlayerUI;
import dev.michey.expo.util.ClientPackets;
import dev.michey.expo.util.ExpoShared;

public class InputController {

    /** Click operations */
    public void onLeftClickBegin(int x, int y, boolean consoleOpen, boolean chatOpen, boolean inventoryOpen) {
        if(consoleOpen) {
            // Block any input to ingame if console is open
            GameConsole.get().handleTouchDown(x, y);
            return;
        }

        PlayerInventory inv = PlayerInventory.LOCAL_INVENTORY;
        if(inv == null) return;

        PlayerUI ui = ExpoClientContainer.get().getPlayerUI();

        if(ui.hoveredSlot == null) {
            // Clicked while having no slot selected
            ClientPackets.p18PlayerInventoryInteraction(ExpoShared.PLAYER_INVENTORY_ACTION_LEFT, ExpoShared.PLAYER_INVENTORY_SLOT_VOID);
        } else {
            // Clicked on a slot
            ui.hoveredSlot.onLeftClick();
        }
    }

    public void onRightClickBegin(int x, int y, boolean consoleOpen, boolean chatOpen, boolean inventoryOpen) {
        if(consoleOpen) return;

        PlayerInventory inv = PlayerInventory.LOCAL_INVENTORY;
        if(inv == null) return;

        PlayerUI ui = ExpoClientContainer.get().getPlayerUI();

        if(ui.hoveredSlot == null) {
            // Clicked while having no slot selected
            ClientPackets.p18PlayerInventoryInteraction(ExpoShared.PLAYER_INVENTORY_ACTION_RIGHT, ExpoShared.PLAYER_INVENTORY_SLOT_VOID);
        } else {
            ui.hoveredSlot.onRightClick();
        }
    }

    public void onMiddleClickBegin(int x, int y, boolean consoleOpen, boolean chatOpen, boolean inventoryOpen) {

    }

    public void onLeftClickEnd(int x, int y, boolean consoleOpen, boolean chatOpen, boolean inventoryOpen) {
        if(consoleOpen) {
            // Block any input to ingame if console is open
            GameConsole.get().handleTouchUp();
        }
    }

    public void onRightClickEnd(int x, int y, boolean consoleOpen, boolean chatOpen, boolean inventoryOpen) {

    }

    public void onMiddleClickEnd(int x, int y, boolean consoleOpen, boolean chatOpen, boolean inventoryOpen) {

    }

    /** Dragging operations */
    public void onDrag(int x, int y, boolean consoleOpen, boolean chatOpen, boolean inventoryOpen) {
        if(consoleOpen) {
            // Block any input to ingame if console is open
            GameConsole.get().handleDragged(x, y);
        }
    }

    /** Scroll operations */
    public void onScroll(float x, float y, boolean consoleOpen, boolean chatOpen, boolean inventoryOpen) {
        if(consoleOpen) {
            // Block any input to ingame if console is open
            GameConsole.get().onScroll(y);
            return;
        }

        if(chatOpen) {
            // Block any input to ingame if chat is open
            ExpoClientChat.get().onScroll(y);
            return;
        }

        if(Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
            RenderContext.get().expoCamera.addZoomAnimation(y * 0.1f);
        } else {
            if(PlayerInventory.LOCAL_INVENTORY != null) {
                PlayerInventory.LOCAL_INVENTORY.scrollSelectedSlot((int) y);
            }
        }
    }

    /** Typing operations */
    public void onKeyTyped(char character, boolean consoleOpen, boolean chatOpen, boolean inventoryOpen) {
        if(consoleOpen) {
            GameConsole.get().onKeyTyped(character);
            return;
        }

        if(chatOpen) {
            ExpoClientChat.get().onKeyTyped(character);
        }
    }

    public void onKeyDown(int keycode, boolean consoleOpen, boolean chatOpen, boolean inventoryOpen) {
        if(keycode == Input.Keys.F1) {
            GameConsole.get().toggleVisibility();
            return;
        }

        if(consoleOpen) {
            if(keycode >= 19 && keycode <= 22 && !GameConsole.get().isActiveKey(Input.Keys.CONTROL_LEFT)) {
                GameConsole.get().onArrow(keycode);
                GameConsole.get().setActiveKey(keycode, true);
                return;
            }

            if(keycode == Input.Keys.CONTROL_LEFT || keycode == Input.Keys.SHIFT_LEFT) {
                GameConsole.get().setActiveKey(keycode, true);
            } else {
                if(GameConsole.get().isActiveKey(Input.Keys.CONTROL_LEFT)) {
                    GameConsole.get().onKeyCombo(Input.Keys.CONTROL_LEFT, keycode);
                } else if(GameConsole.get().isActiveKey(Input.Keys.SHIFT_LEFT)) {
                    GameConsole.get().onKeyCombo(Input.Keys.SHIFT_LEFT, keycode);
                }
            }
        } else if(chatOpen && keycode != Input.Keys.ENTER) {
            if(keycode >= 19 && keycode <= 22 && !ExpoClientChat.get().isActiveKey(Input.Keys.CONTROL_LEFT)) {
                ExpoClientChat.get().onArrow(keycode);
                ExpoClientChat.get().setActiveKey(keycode, true);
                return;
            }

            if(keycode == Input.Keys.CONTROL_LEFT || keycode == Input.Keys.SHIFT_LEFT) {
                ExpoClientChat.get().setActiveKey(keycode, true);
            } else {
                if(ExpoClientChat.get().isActiveKey(Input.Keys.CONTROL_LEFT)) {
                    ExpoClientChat.get().onKeyCombo(Input.Keys.CONTROL_LEFT, keycode);
                } else if(ExpoClientChat.get().isActiveKey(Input.Keys.SHIFT_LEFT)) {
                    ExpoClientChat.get().onKeyCombo(Input.Keys.SHIFT_LEFT, keycode);
                }
            }
        } else {
            switch(keycode) {
                case Input.Keys.F2 -> RenderContext.get().drawTileInfo = !RenderContext.get().drawTileInfo;
                case Input.Keys.F3 -> RenderContext.get().drawDebugHUD = !RenderContext.get().drawDebugHUD;
                case Input.Keys.F4 -> RenderContext.get().drawImGui = !RenderContext.get().drawImGui;
                case Input.Keys.F5 -> RenderContext.get().drawShapes = !RenderContext.get().drawShapes;
                case Input.Keys.F6 -> RenderContext.get().drawHUD = !RenderContext.get().drawHUD;
                case Input.Keys.TAB -> ExpoClientContainer.get().getPlayerUI().toggleTablist();
                case Input.Keys.ENTER -> ExpoClientChat.get().toggleFocus();
            }
        }
    }

    public void onKeyUp(int keycode, boolean consoleOpen, boolean chatOpen, boolean inventoryOpen) {
        if(consoleOpen) {
            if(keycode >= 19 && keycode <= 22) {
                GameConsole.get().setActiveKey(keycode, false);
                return;
            }

            if(keycode == Input.Keys.CONTROL_LEFT || keycode == Input.Keys.SHIFT_LEFT) {
                GameConsole.get().setActiveKey(keycode, false);
            }
        } else if(chatOpen) {
            if(keycode >= 19 && keycode <= 22) {
                ExpoClientChat.get().setActiveKey(keycode, false);
                return;
            }

            if(keycode == Input.Keys.CONTROL_LEFT || keycode == Input.Keys.SHIFT_LEFT) {
                ExpoClientChat.get().setActiveKey(keycode, false);
            }
        } else {
            if(keycode == Input.Keys.TAB) {
                ExpoClientContainer.get().getPlayerUI().toggleTablist();
            }
        }
    }

}
