package dev.michey.expo.console.command;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.TimeUtils;
import dev.michey.expo.noise.BiomeType;
import make.some.noise.Noise;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommandNoise extends AbstractConsoleCommand {

    @Override
    public String getCommandName() {
        return "/noise";
    }

    @Override
    public String getCommandDescription() {
        return "Noise generation debug command";
    }

    @Override
    public String getCommandSyntax() {
        return "/noise";
    }

    @Override
    public void executeCommand(String[] args) {
        final int[] modes = new int[] {Noise.SIMPLEX_FRACTAL};
        final float[] freq = new float[] {0.0075f, 0.01f};
        final int[] octa = new int[] {5};

        int attempts = 1;
        final int[] seeds = new int[attempts];

        for(int i = 0; i < seeds.length; i++) {
            seeds[i] = MathUtils.random.nextInt();
        }

        final int size = 1024;
        final long start = TimeUtils.millis();
        String basePath = "noiseTests/" + start + "/";

        success("Starting " + (freq.length * octa.length * modes.length * attempts) + " thread(s) to generate noise images");
        ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for(int att = 0; att < attempts; att++) {
            for(int a = 0; a < freq.length; a++) {
                for(int b = 0; b < octa.length; b++) {
                    for(int c = 0; c < modes.length; c++) {
                        int finalA = a;
                        int finalB = b;
                        int finalC = c;
                        int finalAtt = att;

                        service.execute(() -> {
                            Noise noise = new Noise(seeds[finalAtt], freq[finalA], modes[finalC], octa[finalB]);

                            Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);

                            for(int i = 0; i < size; i++) {
                                for(int j = 0; j < size; j++) {
                                    float value = noise.getConfiguredNoise(i, j);
                                    float normalized = (value + 1) / 2f;

                                    if(normalized > 0.85) {
                                        pixmap.drawPixel(i, j, Color.rgba8888(150/255f,75f/255f,0,1));
                                    } else {
                                        pixmap.drawPixel(i, j, Color.rgba8888(normalized, normalized, normalized, 1));
                                    }
                                }
                            }

                            String str = basePath + "mode_" + modes[finalC] + "-" + "freq_" + freq[finalA] + "-octa_" + octa[finalB] + "-" + finalAtt + ".png";
                            FileHandle fh = Gdx.files.local(str);
                            PixmapIO.writePNG(fh, pixmap);
                            pixmap.dispose();

                            success("Written noise image to " + fh.path());
                        });
                    }
                }
            }
        }
    }

}
