package dev.michey.expo.console.command;

import com.badlogic.gdx.Gdx;
import dev.michey.expo.command.util.CommandSyntaxException;
import dev.michey.expo.server.fs.world.WorldSaveFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public class CommandDelsave extends AbstractConsoleCommand {

    @Override
    public String getCommandName() {
        return "/delsave";
    }

    @Override
    public String getCommandDescription() {
        return "Deletes the specified world folder";
    }

    @Override
    public String getCommandSyntax() {
        return "/delsave <world>";
    }

    @Override
    public void executeCommand(String[] args) throws CommandSyntaxException {
        String worldName = parseString(args, 1);

        File savesFolder = new File(WorldSaveFile.getPathSaveFolder());
        File[] folders = savesFolder.listFiles();

        if(folders == null) {
            error("Saves folder doesn't exist.");
            return;
        }

        File saveFolder = new File(savesFolder.getAbsolutePath() + File.separator + worldName);

        if(!saveFolder.exists()) {
            error("Folder " + worldName + " doesn't exist.");
            return;
        }

        try(Stream<Path> pathStream = Files.walk(saveFolder.toPath())) {
            pathStream.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);

            success("Successfully deleted save folder " + worldName + ".");
        } catch (IOException e) {
            error("Failed to delete save folder " + worldName + ":");
            e.printStackTrace();
        }
    }

}
