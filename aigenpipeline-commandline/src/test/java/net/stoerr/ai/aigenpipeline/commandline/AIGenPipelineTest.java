package net.stoerr.ai.aigenpipeline.commandline;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

public class AIGenPipelineTest {

    @Rule
    public ErrorCollector ec = new ErrorCollector();

    @Test
    public void readArguments() throws IOException {
        URL url = getClass().getClassLoader().getResource("cfgfilecheck/sub/subsub/.aigenpipeline");
        assertEquals("file", url.getProtocol());
        File dir = new File(url.getPath()).getParentFile();
        assertTrue(dir.isDirectory());
        AIGenPipeline p = new AIGenPipeline();
        p.readArguments(new String[]{"-m", "xxx"}, dir);
        // top level file should be ignored
        ec.checkThat(p.verbose, is(false));
        ec.checkThat(p.model, is("xxx"));
        ec.checkThat(p.verbose, is(false));
        ec.checkThat(p.apiKey, is("akey"));
        ec.checkThat(p.url,is("https://bottom-url/"));

    }
}
