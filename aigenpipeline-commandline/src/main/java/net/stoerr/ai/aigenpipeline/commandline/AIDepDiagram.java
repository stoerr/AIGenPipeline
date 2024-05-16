package net.stoerr.ai.aigenpipeline.commandline;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.stoerr.ai.aigenpipeline.framework.task.AIInOut;

public class AIDepDiagram {

    protected final List<AIGenPipeline> pipelines;
    protected final File rootDir;

    protected int nextId = 1;
    protected Map<String, String> pathToId = new HashMap<>();

    public AIDepDiagram(List<AIGenPipeline> pipelines, File rootDir) {
        this.pipelines = pipelines;
        this.rootDir = rootDir;
    }

    /**
     * Prints a dependency diagram of the pipelines to the given output stream.
     */
    public void printDepDiagram(PrintStream out) {
        out.println("graph TD");
        for (AIGenPipeline pipeline : pipelines) {
            String outId = idForInOut(pipeline.taskOutput);
            String outLabel = labelForInOut(pipeline.taskOutput);
            for (AIInOut input : pipeline.inputFiles) {
                String inId = idForInOut(input);
                String inLabel = labelForInOut(input);
                out.println("    " + inId + "[\"" + inLabel + "\"] --> " + outId + "[\"" + outLabel + "\"]");
            }
            for (AIInOut prompt : pipeline.promptFiles) {
                String inId = idForInOut(prompt);
                String inLabel = labelForInOut(prompt);
                out.println("    " + inId + "([\"" + inLabel + "\"]) --> " + outId + "[\"" + outLabel + "\"]");
            }
        }
    }

    protected String labelForInOut(AIInOut inOut) {
        Path other = inOut.getFile().getAbsoluteFile().toPath();
        return rootDir.getAbsoluteFile().toPath().relativize(other).toString();
    }

    protected String idForInOut(AIInOut inout) {
        String path = inout.getFile().getAbsolutePath();
        String id = pathToId.get(path);
        if (null == id) {
            id = "F" + String.valueOf(1000 + nextId++).substring(1);
            pathToId.put(path, id);
        }
        return id;
    }

}
