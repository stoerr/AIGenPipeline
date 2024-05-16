package net.stoerr.ai.aigenpipeline.commandline;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class TopoSortTest {

    private TopoSort<Integer> topoSort;

    @Before
    public void setUp() {
        topoSort = new TopoSort<>();
    }

    @Test
    public void testSort() throws TopoSort.TopoSortCycleException {
        topoSort.addEdge(2, 3);
        topoSort.addEdge(1, 2);
        topoSort.addEdge(3, 4);
        List<Integer> sorted = topoSort.sort();
        assertEquals(Arrays.asList(1, 2, 3, 4), sorted);
    }

    @Test(expected = TopoSort.TopoSortCycleException.class)
    public void testSortWithCycle() throws TopoSort.TopoSortCycleException {
        topoSort.addEdge(1, 2);
        topoSort.addEdge(2, 3);
        topoSort.addEdge(3, 1);
        topoSort.sort();
    }

    @Test
    public void testSortWithMultipleEdges() throws TopoSort.TopoSortCycleException {
        topoSort.addEdge(2, 4);
        topoSort.addEdge(1, 2);
        topoSort.addEdge(1, 3);
        topoSort.addEdge(3, 4);
        List<Integer> sorted = topoSort.sort();
        assertEquals(Arrays.asList(1, 2, 3, 4), sorted);
    }
}
