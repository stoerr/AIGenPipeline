package net.stoerr.ai.aigenpipeline.commandline;

import java.io.File;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

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
     * @param out the output stream to print to
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

    /**
     * Does a topological sort to execute tasks that depend on other tasks later.
     * @return the sorted list of pipelines, not null
     */
    @Nonnull
    public List<AIGenPipeline> sortedPipelines() {
        List<AIGenPipeline> sorted = new ArrayList<>();
        TopoSort<String> sort = new TopoSort<>();
        for (AIGenPipeline pipeline : pipelines) {
            String outId = idForInOut(pipeline.taskOutput);
            for (AIInOut input : pipeline.inputFiles) {
                String inId = idForInOut(input);
                if(!inId.equals(outId)) {
                    sort.addEdge(inId, outId);
                }
            }
            for (AIInOut prompt : pipeline.promptFiles) {
                String inId = idForInOut(prompt);
                sort.addEdge(inId, outId);
            }
        }

        List<String> sortedIds = null;
        try {
            sortedIds = sort.sort();
        } catch (TopoSort.TopoSortCycleException e) {
            String id = (String) e.getNode();
            File involvedFile = pipelines.stream()
                    .map(p -> p.taskOutput)
                    .filter(io -> idForInOut(io).equals(id))
                    .findFirst().get().getFile();
            throw new IllegalArgumentException("Cycle detected involving file " + involvedFile.getAbsolutePath());
        }

        Map<String, List<AIGenPipeline>> outIdToPipelines = new HashMap<>();
        List<AIGenPipeline> remainingPipelines = new ArrayList<>(pipelines);
        for (AIGenPipeline pipeline : pipelines) {
            String outId = idForInOut(pipeline.taskOutput);
            List<AIGenPipeline> list = outIdToPipelines.get(outId);
            if (null == list) {
                list = new ArrayList<>();
                outIdToPipelines.put(outId, list);
            }
            list.add(pipeline);
        }
        for (String id : sortedIds) {
            List<AIGenPipeline> list = outIdToPipelines.get(id);
            if (null != list) {
                sorted.addAll(list);
                remainingPipelines.removeAll(list);
            }
        }
        sorted.addAll(remainingPipelines);
        return sorted;
    }

}
