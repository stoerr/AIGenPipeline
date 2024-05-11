package net.stoerr.ai.aigenpipeline.framework.task;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A parsing / creation class for markers like AIGenVersion(ourversion, inputfile1@version1, inputfile2@version2, ...).
 */
public class AIVersionMarker {

    public static final Pattern VERSION_MARKER_PATTERN = Pattern.compile("AIGenVersion\\([^)]+\\)");
    protected final String ourVersion;
    protected final List<String> inputVersions;

    public AIVersionMarker(String ourVersion, List<String> inputVersions) {
        this.ourVersion = ourVersion;
        this.inputVersions = null != inputVersions ? new ArrayList<>(inputVersions) : Collections.emptyList();
    }

    @Nullable
    public static AIVersionMarker find(String content) {
        if (content == null) {
            return null;
        }
        Matcher matcher = VERSION_MARKER_PATTERN.matcher(content);
        if (!matcher.find()) {
            return null;
        }
        String marker = matcher.group();
        marker = marker.substring(13, marker.length() - 1); // Remove 'AIGenVersion(' and ')'
        String[] parts = marker.split(", ");
        if (parts.length < 1) {
            return new AIVersionMarker("", Collections.emptyList());
        }
        String ourVersion = parts[0];
        List<String> inputVersions = Arrays.asList(parts).subList(1, parts.length);
        return new AIVersionMarker(ourVersion, inputVersions);
    }

    @Nullable
    public static String replaceMarkerIn(@Nullable String content, @Nonnull String newMarker) {
        if (content == null) {
            return null;
        }
        Matcher matcher = VERSION_MARKER_PATTERN.matcher(content);
        if (!matcher.find()) {
            return content;
        }
        return matcher.replaceFirst(newMarker);
    }

    /**
     * Determine the version marker for input files / prompt files.
     */
    public static String determineFileVersionMarker(@Nonnull AIInOut inOut) {
        String content = inOut.read();
        requireNonNull(content, "Could not read file " + inOut);
        AIVersionMarker aiVersionMarker = AIVersionMarker.find(content);
        String version;
        if (aiVersionMarker != null) {
            version = aiVersionMarker.getOurVersion();
        } else {
            version = shaHash(content);
        }
        return inOut.getFile().getName() + "-" + version;
    }

    public static String shaHash(String content) {
        String condensedWhitespace = content.replaceAll("\\s+", " ");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(condensedWhitespace.getBytes(StandardCharsets.UTF_8));
            // turn first 4 bytes into hex number
            long hashNumber = ((hash[3] * 256L + hash[2]) * 256L + hash[1]) * 256L + hash[0];
            String hexString = "00000000" + Long.toHexString(Math.abs(hashNumber));
            return hexString.substring(hexString.length() - 8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA256 not available", e);
        }
    }

    public static List<String> calculateInputMarkers(List<AIInOut> inputs, List<String> additionalMarkers) {
        List<String> inputVersions = inputs.stream()
                .map(AIVersionMarker::determineFileVersionMarker)
                .collect(Collectors.toList());
        inputVersions.addAll(additionalMarkers);
        return inputVersions;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ourVersion, inputVersions);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AIVersionMarker other = (AIVersionMarker) obj;
        return Objects.equals(ourVersion, other.ourVersion) && Objects.equals(inputVersions, other.inputVersions);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("AIGenVersion(");
        sb.append(ourVersion);
        inputVersions.forEach(version -> sb.append(", ").append(version));
        sb.append(")");
        return sb.toString();
    }

    public String getOurVersion() {
        return ourVersion;
    }

    public List<String> getInputVersions() {
        return Collections.unmodifiableList(inputVersions);
    }
}
