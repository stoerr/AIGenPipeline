package net.stoerr.ai.aigenpipeline.framework.task;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

public class AIVersionMarkerTest {

    @Test
    public void testFind() {
        String content = "Some text AIGenVersion(1.0, file1@1.1, file2@1.2) more text";
        AIVersionMarker marker = AIVersionMarker.find(content);
        Assert.assertNotNull(marker);
        Assert.assertEquals("1.0", marker.getOurVersion());
        Assert.assertEquals(Arrays.asList("file1@1.1", "file2@1.2"), marker.getInputVersions());
    }

    @Test
    public void testCreate() {
        AIVersionMarker marker = new AIVersionMarker("1.0", Arrays.asList("file1@1.1", "file2@1.2"));
        String expected = "AIGenVersion(1.0, file1@1.1, file2@1.2)";
        Assert.assertEquals(expected, marker.toString());
    }

    @Test
    public void testEqualsAndHashCode() {
        AIVersionMarker marker1 = new AIVersionMarker("1.0", Arrays.asList("file1@1.1", "file2@1.2"));
        AIVersionMarker marker2 = new AIVersionMarker("1.0", Arrays.asList("file1@1.1", "file2@1.2"));
        Assert.assertEquals(marker1, marker2);
        Assert.assertEquals(marker1.hashCode(), marker2.hashCode());
    }

    @Test
    public void testHandlingNullInputs() {
        Assert.assertNull(AIVersionMarker.find(null));
        AIVersionMarker marker = new AIVersionMarker(null, null);
        Assert.assertEquals(null, marker.getOurVersion());
        Assert.assertTrue(marker.getInputVersions().isEmpty());
    }


    @Test
    public void replacesMarkerInContentWithSingleMarker() {
        String content = "Some text AIGenVersion(1.0, file1@1.1, file2@1.2) more text";
        String newMarker = "AIGenVersion(2.0, file3@2.1, file4@2.2)";
        String expected = "Some text AIGenVersion(2.0, file3@2.1, file4@2.2) more text";
        String result = AIVersionMarker.replaceMarkerIn(content, newMarker);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void doesNotReplaceMarkerInContentWithoutMarker() {
        String content = "Some text without marker more text";
        String newMarker = "AIGenVersion(2.0, file3@2.1, file4@2.2)";
        String result = AIVersionMarker.replaceMarkerIn(content, newMarker);
        Assert.assertEquals(content, result);
    }

    @Test
    public void returnsNullWhenContentIsNull() {
        String newMarker = "AIGenVersion(2.0, file3@2.1, file4@2.2)";
        String result = AIVersionMarker.replaceMarkerIn(null, newMarker);
        Assert.assertNull(result);
    }

}
