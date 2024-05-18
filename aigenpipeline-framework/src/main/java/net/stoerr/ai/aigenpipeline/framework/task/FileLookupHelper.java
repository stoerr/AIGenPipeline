package net.stoerr.ai.aigenpipeline.framework.task;

import java.io.File;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Makes it easy to find files to process with {@link AIGenerationTask} etc.
 */
public class FileLookupHelper {

    protected static final Logger LOG = Logger.getLogger(FileLookupHelper.class.getName());

    public static final String HTML_PATTERN = ".*\\.html";

    /**
     * A pattern matching a prefix of a file name that ends with a / and has no meta characters of the
     * {@link java.nio.file.FileSystem#getPathMatcher(String)} in it.
     */
    protected static final Pattern NOMETAPREFIXPATTERN = Pattern.compile("^/?([^/*?{}\\[\\]]+/)+");

    /**
     * The maximum filesize we search in.
     */
    protected static final int FILE_MAXSIZE = 50 * 1024;

    /**
     * Pattern matching file names of files which have binary content (*.jpg, *.gif, *.jar etc.), which we ignore in the search.
     */
    protected static final Pattern BINARYFILEPATTERN = Pattern.compile(
            ".*\\.(jpg|gif|png|[jwet]ar|class|zip|gz|tgz|pdf|doc|xls|ppt|docx|xlsx|pptx|odt|ods)");

    protected final File directory;

    protected FileLookupHelper(String path) {
        try {
            directory = new File(path).getAbsoluteFile().getCanonicalFile();
        } catch (IOException e) {
            throw new IllegalStateException("Path " + path + " does not exist", e);
        }
        if (!directory.isDirectory()) {
            throw new IllegalStateException("Directory " + directory + " does not exist");
        }
    }

    public static FileLookupHelper fromPath(String... relativePaths) {
        StringBuilder path = new StringBuilder();
        for (String relativePath : relativePaths) {
            path.append(relativePath).append("/");
        }
        return new FileLookupHelper(path.toString());
    }

    /**
     * Make repository from environment variable.
     */
    public static FileLookupHelper fromEnv(@Nonnull String envVar, @Nullable String relativePath) {
        String path = System.getenv(envVar);
        if (path == null) {
            throw new IllegalStateException("Environment variable " + envVar + " not set");
        }
        if (relativePath != null) {
            path = path + File.pathSeparator + relativePath;
        }
        return new FileLookupHelper(path);
    }

    /**
     * File relative to repository root - that doesn't need to exist (might be output file).
     */
    public File file(String relpath) {
        return new File(directory, relpath);
    }

    /**
     * Files in a directory, matching an ant style pattern - more specifically like
     * {@link java.nio.file.FileSystem#getPathMatcher(String)} glob patterns (without "glob:" prefix).
     *
     * @param relpathDirectory the directory relative to the repository root
     * @param filePathPattern  the file pattern to match, see {@link java.nio.file.FileSystem#getPathMatcher(String)} glob pattern
     * @param recursive        whether to recurse into subdirectories
     * @return a list of files
     * @see java.nio.file.FileSystem#getPathMatcher(String)
     */
    @Nonnull
    public List<File> files(@Nonnull String relpathDirectory, @Nullable String filePathPattern, boolean recursive) {
        if (filePathPattern != null) {
            Matcher fixedPrefixMatcher = NOMETAPREFIXPATTERN.matcher(filePathPattern);
            if (fixedPrefixMatcher.find()) {
                String prefix = fixedPrefixMatcher.group();
                relpathDirectory = Path.of(relpathDirectory).resolve(prefix).normalize().toString();
                filePathPattern = filePathPattern.substring(prefix.length());
            }
        }

        File dir = new File(directory, relpathDirectory);
        if (!dir.isDirectory()) {
            throw new IllegalStateException("Directory " + dir + " does not exist");
        }
        Path dirPath = dir.toPath().normalize();
        PathMatcher pathMatcher = null != filePathPattern && !filePathPattern.isEmpty() ?
                dirPath.getFileSystem().getPathMatcher("glob:" + filePathPattern) : null;
        List<File> result = new ArrayList<>();
        if (!recursive) {
            File[] files = dir.listFiles(file -> pathMatcher == null || pathMatcher.matches(file.toPath())
                    || pathMatcher.matches(dirPath.relativize(file.toPath())));
            Arrays.stream(Objects.requireNonNull(files, dir.toString()))
                    .filter(File::isFile).forEach(result::add);
        } else {
            try {
                Files.walkFileTree(dirPath, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Path relativePath = dirPath.relativize(file);
                        if (Files.isRegularFile(file) &&
                                (pathMatcher == null || pathMatcher.matches(relativePath) || pathMatcher.matches(file))) {
                            result.add(file.toFile());
                        }
                        return super.visitFile(file, attrs);
                    }
                });
            } catch (IOException e) {
                LOG.severe("for " + dir + ":" + e);
            }
        }
        return result;
    }

    /**
     * All files matching a filePathRegex that contain a pattern.
     *
     * @param relpathDirectory the directory relative to the repository root
     * @param filePathPattern  the file pattern to match
     * @param pattern          the regex to look for in the file content
     * @param recursive        whether to recurse into subdirectories
     * @return a list of files
     */
    @Nonnull
    public List<File> filesContaining(@Nonnull String relpathDirectory, @Nonnull String filePathPattern, @Nonnull Pattern pattern, boolean recursive) {
        List<File> candidates = files(relpathDirectory, filePathPattern, recursive);
        List<File> result = new ArrayList<>();
        for (File file : candidates) {
            if (file.length() > FILE_MAXSIZE || BINARYFILEPATTERN.matcher(file.getName()).matches()) {
                continue;
            }
            try {
                String content = Files.readString(file.toPath());
                if (pattern.matcher(content).find()) {
                    result.add(file);
                }
            } catch (MalformedInputException e) { // skip binary file
                // ignore
            } catch (IOException e) {
                LOG.severe("for " + file + ":" + e);
            }
        }
        return result;
    }

    /**
     * File from full java class name.
     */
    @Nonnull
    public File javaFile(@Nonnull String fullName) {
        return new File(directory, fullName.replaceAll("[.]", "/") + ".java");
    }

    /**
     * File for documenting a full java class name.
     */
    @Nonnull
    public File javaMdFile(@Nonnull String fullName) {
        return new File(directory, fullName.replaceAll("[.]", "/") + ".md");
    }

}
