package dev.michey.expo.console.command;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.TimeUtils;
import make.some.noise.Noise;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommandRiver extends AbstractConsoleCommand {

    @Override
    public String getCommandName() {
        return "/river";
    }

    @Override
    public String getCommandDescription() {
        return "River noise generation debug command";
    }

    @Override
    public String getCommandSyntax() {
        return "/river";
    }

    @Override
    public void executeCommand(String[] args) {
        int seed = MathUtils.random.nextInt();

        final int[] modes = new int[] {Noise.SIMPLEX_FRACTAL, Noise.FOAM_FRACTAL};
        final float[] freq = new float[] {32f, 64f, 128f, 256f, 512f};
        final int[] octa = new int[] {1};
        //final float[] water = new float[] {0.35f, 0.37f, 0.39f, 0.41f, 0.43f, 0.45f, 0.47f, 0.49f, 0.51f, 0.53f, 0.55f, 0.57f};

        final int size = 900;
        final long start = TimeUtils.millis();
        String basePath = "noiseTests/" + start + "/";

        success("Starting " + (freq.length * octa.length * modes.length) + " thread(s) to generate river noise images");
        ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for(int a = 0; a < freq.length; a++) {
            for(int b = 0; b < octa.length; b++) {
                for(int c = 0; c < modes.length; c++) {
                    int finalA = a;
                    int finalB = b;
                    int finalC = c;

                    service.execute(() -> {
                        Noise noise = new Noise(seed, 1f/freq[finalA], modes[finalC], octa[finalB]);
                        noise.setFractalType(Noise.RIDGED_MULTI);
                        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);

                        for(int i = 0; i < size; i++) {
                            for(int j = 0; j < size; j++) {
                                float value = noise.getConfiguredNoise(i, j);
                                float normalized = (value + 1) / 2f;

                                int color = Color.rgba8888(normalized, normalized, normalized, 1.0f);
                                pixmap.drawPixel(i, j, color);
                            }
                        }

                        String str = basePath + "mode_" + modes[finalC] + "-" + "freq_" + freq[finalA] + "-octa_" + octa[finalB] + ".png";
                        FileHandle fh = Gdx.files.local(str);
                        PixmapIO.writePNG(fh, pixmap);
                        pixmap.dispose();

                        success("Written river noise image to " + fh.path());
                    });
                }
            }
        }
    }

}
