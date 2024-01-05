package dev.michey.expo.console.command;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.noise.BiomeType;
import dev.michey.expo.server.main.logic.world.ServerWorld;
import dev.michey.expo.server.main.logic.world.chunk.ServerChunk;
import dev.michey.expo.util.ExpoShared;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static dev.michey.expo.util.ExpoShared.ROW_TILES;

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

    /** Returns the chunk at chunk coordinates X & Y. */
    public ServerChunk getChunk(int chunkX, int chunkY) {
        ServerChunk chunk = new ServerChunk(ServerWorld.get().getDimension("overworld"), chunkX, chunkY);
        chunk.generate(false, false);
        return chunk;
    }

    @Override
    public void executeCommand(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        final int pxmapsize = 2048;
        int runs = pxmapsize / 16;
        ServerChunk[] chunks = new ServerChunk[runs * runs];
        Pixmap pixmap = new Pixmap(pxmapsize, pxmapsize, Pixmap.Format.RGBA8888);

        ExpoLogger.log("Starting...");

        for(int i = 0; i < runs; i++) {
            for(int j = 0; j < runs; j++) {
                int finalI = i;
                int finalJ = j;

                executorService.execute(() -> {
                    ServerChunk c = getChunk(finalI, finalJ);
                    int index = finalJ * runs + finalI;
                    chunks[index] = c;
                });
            }
        }

        ExpoLogger.log("Started... ");
        executorService.shutdown();

        try {
            if(!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                ExpoLogger.log("Failed to finish task in 60 seconds.");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        ExpoLogger.log("Finished, now writing...");

        for(ServerChunk c : chunks) {
            for(int x = 0; x < c.tiles.length; x++) {
                int tileX = ExpoShared.posToTile(ExpoShared.chunkToPos(c.chunkX)) + x % ROW_TILES;
                int tileY = ExpoShared.posToTile(ExpoShared.chunkToPos(c.chunkY)) + x / ROW_TILES;
                BiomeType b = c.tiles[x].biome;
                float[] colors = b.BIOME_COLOR;
                pixmap.drawPixel(tileX, tileY, Color.rgba8888(colors[0], colors[1], colors[2], 1.0f));
            }
        }

        FileHandle fh = Gdx.files.local("noiseCmd/_" + System.currentTimeMillis() + ".png");
        PixmapIO.writePNG(fh, pixmap);
        pixmap.dispose();

        success("Written noise image to " + fh.path());

        // -----------
        /*
        final int[] modes = new int[] {Noise.PERLIN_FRACTAL};
        final float[] freq = new float[] {0.005f, 0.0025f};
        final int[] octa = new int[] {1, 2, 3};

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
                            noise.setFractalType(Noise.RIDGED_MULTI);

                            Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);

                            for(int i = 0; i < size; i++) {
                                for(int j = 0; j < size; j++) {
                                    float value = noise.getConfiguredNoise(i, j);
                                    float normalized = (value + 1) / 2f;

                                    //normalized = MathUtils.clamp(normalized * 1.15f, 0.0f, 1.0f);

                                    if(normalized > 0.9f) {
                                        pixmap.drawPixel(i, j, Color.rgba8888(0,0.5f,1,1));
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
        */
    }

}
