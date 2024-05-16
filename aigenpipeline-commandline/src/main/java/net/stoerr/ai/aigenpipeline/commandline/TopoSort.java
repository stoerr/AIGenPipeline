package net.stoerr.ai.aigenpipeline.commandline;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TopoSort<T> {

    private List<Edge<T>> edges = new ArrayList<>();

    protected static class Edge<T> {
        public final T from;
        public final T to;

        public Edge(T from, T to) {
            this.to = to;
            this.from = from;
        }
    }

    public void addEdge(T from, T to) {
        edges.add(new Edge<>(from, to));
    }

    /**
     * Calculates topologically sorted list of nodes.
     * If a cycle is detected, an IllegalArgumentException is thrown.
     *
     * @return sorted list of nodes - if there is an edge from node A to node B, then A will appear before B in the list
     */
    public List<T> sort() throws TopoSortCycleException {
        List<T> result = new ArrayList<>();
        Set<T> visited = new HashSet<>();
        Set<T> visiting = new HashSet<>();
        for (Edge<T> edge : edges) {
            if (!visited.contains(edge.to)) {
                visit(edge.to, visited, visiting, result);
            }
        }
        return result;
    }

    protected void visit(T node, Set<T> visited, Set<T> visiting, List<T> result) throws TopoSortCycleException {
        if (visiting.contains(node)) {
            throw new TopoSortCycleException(node);
        }
        if (!visited.contains(node)) {
            visiting.add(node);
            for (Edge<T> edge : edges) {
                if (edge.to.equals(node)) {
                    visit(edge.from, visited, visiting, result);
                }
            }
            visiting.remove(node);
            visited.add(node);
            result.add(node);
        }
    }

    /**
     * Is thrown to notify that there is a cycle regarding the given node.
     */
    public static class TopoSortCycleException extends Exception {
        protected Object node;

        public TopoSortCycleException(Object node) {
            this.node = node;
        }

        /**
         * One node involved into a cycle.
         */
        public Object getNode() {
            return node;
        }


    }

}
