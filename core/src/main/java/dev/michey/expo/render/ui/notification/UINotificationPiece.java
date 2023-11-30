package dev.michey.expo.render.ui.notification;

import com.badlogic.gdx.graphics.Color;

public class UINotificationPiece {

    public Color color;
    public String text;

    public UINotificationPiece(String text) {
        this.text = text;
        this.color = new Color(Color.WHITE);
    }

    public UINotificationPiece(String text, Color color) {
        this.text = text;
        this.color = new Color(color);
    }

}
