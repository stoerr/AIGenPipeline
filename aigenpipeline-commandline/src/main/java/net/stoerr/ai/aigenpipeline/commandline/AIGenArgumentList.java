package net.stoerr.ai.aigenpipeline.commandline;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A set of arguments that is either given on command line, from the environment or otherwise given, or read from a config file. There are often several such argument sets stacked on each other.
 */
public class AIGenArgumentList {

    /**
     * If it is from a configuration file.
     */
    @Nullable
    protected final File cfgFile;

    /**
     * The actual arguments in series.
     */
    @Nonnull
    protected final String[] args;

    @Nullable
    public File getCfgFile() {
        return cfgFile;
    }

    @Nonnull
    public String[] getArgs() {
        return args;
    }

    /**
     * Unrelated to file.
     * @param args the arguments
     */
    public AIGenArgumentList(@Nonnull String[] args) {
        this.cfgFile = null;
        this.args = args;
    }

    /**
     * Reads the arguments from a file.
     * @param cfgFile the file to read from
     */
    public AIGenArgumentList( @Nonnull File cfgFile) {
        this.cfgFile = cfgFile;
        try (Stream<String> lines = Files.lines(cfgFile.toPath(), StandardCharsets.UTF_8)) {
            String content = lines
                    .map(String::trim)
                    .filter(line -> !line.startsWith("#"))
                    .collect(Collectors.joining(" "));
            this.args = content.split("\\s+");
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read " + cfgFile.getAbsolutePath(), e);
        }
    }

    public boolean hasArgument(String shortform, String longform) {
        return Arrays.asList(args).contains(shortform) || Arrays.asList(args).contains(longform);
    }

}
