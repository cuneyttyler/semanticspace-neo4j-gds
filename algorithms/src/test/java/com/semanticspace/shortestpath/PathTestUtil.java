package com.semanticspace.shortestpath;


import org.neo4j.gds.extension.IdFunction;
import org.neo4j.gds.paths.ImmutablePathResult;
import org.neo4j.gds.paths.PathResult;

public final class PathTestUtil {

    public static PathResult expected(
            IdFunction idFunction,
            long index,
            double[] costs,
            String... nodes
    ) {
        return expected(idFunction, index, new long[0], costs, nodes);
    }

    public static PathResult expected(
            IdFunction idFunction,
            long index,
            long[] relationshipIds,
            double[] costs,
            String... nodes
    ) {
        var builder = ImmutablePathResult.builder()
                .index(index)
                .sourceNode(idFunction.of(nodes[0]))
                .targetNode(idFunction.of(nodes[nodes.length - 1]));

        var nodeIds = new long[nodes.length];

        for (int i = 0; i < nodes.length; i++) {
            nodeIds[i] = idFunction.of(nodes[i]);
        }

        return builder
                .costs(costs)
                .nodeIds(nodeIds)
                .relationshipIds(relationshipIds)
                .build();
    }

    private PathTestUtil() {}
}
