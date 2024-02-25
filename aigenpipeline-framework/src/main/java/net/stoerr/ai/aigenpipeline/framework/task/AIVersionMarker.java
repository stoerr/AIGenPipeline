package net.stoerr.ai.aigenpipeline.framework.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
        List<String> inputVersions = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            inputVersions.add(parts[i]);
        }
        return new AIVersionMarker(ourVersion, inputVersions);
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
