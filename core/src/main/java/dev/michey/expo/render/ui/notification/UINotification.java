package dev.michey.expo.render.ui.notification;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import java.util.Arrays;
import java.util.LinkedList;

public class UINotification {

    public final LinkedList<UINotificationPiece> pieces;
    public TextureRegion icon;
    public float lifetime;
    public String sound;

    // Runtime
    public float delta;
    public boolean playedSound;

    public UINotification() {
        this.pieces = new LinkedList<>();
    }

    public void addPiece(String text, Color color) {
        pieces.add(new UINotificationPiece(text, color));
    }

    public void addPiece(String text) {
        pieces.add(new UINotificationPiece(text, Color.WHITE));
    }

    public void addPieces(UINotificationPiece[] pieces) {
        this.pieces.addAll(Arrays.asList(pieces));
    }

}