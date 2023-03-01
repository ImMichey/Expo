package dev.michey.expo.debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.profiling.GLProfiler;

import javax.swing.*;
import java.awt.*;

public class DebugGL {

    public GLProfiler profiler;

    public JFrame frame;
    public JLabel fpsLabel;
    public JLabel fpsValue;
    public JLabel drawCallsLabel;
    public JLabel drawCallsValue;
    public JLabel textureBindsLabel;
    public JLabel textureBindsValue;
    public JLabel shaderSwapsLabel;
    public JLabel shaderSwapsValue;

    public DebugGL() {
        Dimension dimension = new Dimension(800, 600);
        frame = new JFrame("GLProfiler");
        frame.setResizable(true);
        frame.setSize(dimension);
        frame.setPreferredSize(dimension);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
        frame.setVisible(true);

        profiler = new GLProfiler(Gdx.graphics);
        profiler.enable();

        fpsLabel = new JLabel("FPS:");
        fpsValue = new JLabel();
        drawCallsLabel = new JLabel("Draw calls:");
        drawCallsValue = new JLabel();
        textureBindsLabel = new JLabel("Texture bindings:");
        textureBindsValue = new JLabel();
        shaderSwapsLabel = new JLabel("Shader switches:");
        shaderSwapsValue = new JLabel();

        frame.add(fpsLabel);
        frame.add(fpsValue);
        frame.add(drawCallsLabel);
        frame.add(drawCallsValue);
        frame.add(textureBindsLabel);
        frame.add(textureBindsValue);
        frame.add(shaderSwapsLabel);
        frame.add(shaderSwapsValue);
    }

    public void preRender() {
        profiler.reset();
    }

    public void postRender() {
        fpsValue.setText(String.valueOf(Gdx.graphics.getFramesPerSecond()));
        drawCallsValue.setText(String.valueOf(profiler.getDrawCalls()));
        textureBindsValue.setText(String.valueOf(profiler.getTextureBindings()));
        shaderSwapsValue.setText(String.valueOf(profiler.getShaderSwitches()));
    }

}