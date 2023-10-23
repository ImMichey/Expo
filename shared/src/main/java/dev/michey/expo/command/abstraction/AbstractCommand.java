package dev.michey.expo.command.abstraction;

import dev.michey.expo.command.util.CommandExceptionReason;
import dev.michey.expo.command.CommandResolver;
import dev.michey.expo.command.util.CommandSyntaxException;

public abstract class AbstractCommand {

    /** Parent command resolver */
    private CommandResolver resolver;

    public abstract String getCommandName();
    public abstract String getCommandDescription();
    public abstract String getCommandSyntax();

    public float parseF(String[] array, int pos) throws CommandSyntaxException {
        if(pos >= array.length) throw new CommandSyntaxException("No argument at pos '" + pos + "' present, float required", CommandExceptionReason.OUT_OF_BOUNDS);
        float n;

        try {
            n = Float.parseFloat(array[pos]);
        } catch (NumberFormatException e) {
            throw new CommandSyntaxException("Argument '" + array[pos] + "' at pos '" + pos + "' present, but not a float", CommandExceptionReason.FAULTY_DATATYPE);
        }

        return n;
    }

    public float parseF(String value, int pos) throws CommandSyntaxException {
        float n;

        try {
            n = Float.parseFloat(value);
        } catch (NumberFormatException e) {
            throw new CommandSyntaxException("Argument '" + value + "' at pos '" + pos + "' present, but not a float", CommandExceptionReason.FAULTY_DATATYPE);
        }

        return n;
    }

    public int parseI(String[] array, int pos) throws CommandSyntaxException {
        if(pos >= array.length) throw new CommandSyntaxException("No argument at pos '" + pos + "' present, integer required", CommandExceptionReason.OUT_OF_BOUNDS);
        int n;

        try {
            n = Integer.parseInt(array[pos]);
        } catch (NumberFormatException e) {
            throw new CommandSyntaxException("Argument '" + array[pos] + "' at pos '" + pos + "' present, but not an integer", CommandExceptionReason.FAULTY_DATATYPE);
        }

        return n;
    }

    public boolean parseB(String[] array, int pos) throws CommandSyntaxException {
        if(pos >= array.length) throw new CommandSyntaxException("No argument at pos '" + pos + "' present, boolean required", CommandExceptionReason.OUT_OF_BOUNDS);
        String raw = array[pos].toLowerCase();

        if(raw.equals("true") || raw.equals("1") || raw.equals("yes")) {
            return true;
        }

        if(raw.equals("false") || raw.equals("0") || raw.equals("no")) {
            return false;
        }

        throw new CommandSyntaxException("Argument '" + array[pos] + "' at pos '" + pos + "' present, but not a boolean", CommandExceptionReason.FAULTY_DATATYPE);
    }

    public String parseString(String[] array, int pos) throws CommandSyntaxException {
        if(pos >= array.length) throw new CommandSyntaxException("No argument at pos '" + pos + "' present, string required", CommandExceptionReason.OUT_OF_BOUNDS);
        return array[pos];
    }

    public void requireString(String raw, String... accepted) throws CommandSyntaxException {
        for(String str : accepted) {
            if(raw.equals(str)) return;
        }

        StringBuilder builder = new StringBuilder();

        for(int i = 0; i < accepted.length; i++) {
            builder.append('\'');
            builder.append(accepted[i]);
            builder.append('\'');

            if(i < accepted.length - 1) {
                builder.append(' ');
            }
        }

        throw new CommandSyntaxException("Argument '" + raw + "' has to be one of the following: " + builder, CommandExceptionReason.UNKNOWN);
    }

    public void setResolver(CommandResolver resolver) {
        this.resolver = resolver;
    }

    public CommandResolver getResolver() {
        return resolver;
    }

}
