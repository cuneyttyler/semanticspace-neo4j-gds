package com.semanticspace.shortestpath;

import org.jetbrains.annotations.NotNull;
import org.neo4j.gds.GraphAlgorithmFactory;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.config.AlgoBaseConfig;
import org.neo4j.gds.config.RelationshipWeightConfig;
import org.neo4j.gds.core.concurrency.Pools;
import org.neo4j.gds.core.utils.mem.MemoryEstimation;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.core.utils.progress.tasks.Task;
import org.neo4j.gds.core.utils.progress.tasks.Tasks;
import org.neo4j.gds.paths.AllShortestPathsBaseConfig;
import org.neo4j.gds.paths.ShortestPathBaseConfig;

import java.util.Optional;

public abstract class DijkstraMultiplePairsFactory<T extends AlgoBaseConfig & RelationshipWeightConfig> extends GraphAlgorithmFactory<DijkstraMultiplePairs, T> {

    @Override
    public MemoryEstimation memoryEstimation(T configuration) {
        return DijkstraMultiplePairs.memoryEstimation(false);
    }

    @Override
    public String taskName() {
        return "DijkstraMultiplePairs";
    }

    @Override
    public Task progressTask(Graph graph, T config) {
        return dijkstraProgressTask(taskName(), graph);
    }

    public static Task dijkstraProgressTask(Graph graph) {
        return dijkstraProgressTask("Dijkstra", graph);
    }

    @NotNull
    public static Task dijkstraProgressTask(String taskName, Graph graph) {
        return Tasks.leaf(taskName, graph.relationshipCount());
    }

    public static class MultipleSourceDijkstraFactory<T extends DijkstraMultiplePairsConfig> extends DijkstraMultiplePairsFactory<T> {
        @Override
        public DijkstraMultiplePairs build(
                Graph graph,
                T configuration,
                ProgressTracker progressTracker
        ) {
            return DijkstraMultiplePairs.createInstance(
                    graph,
                    configuration,
                    Optional.empty(),
                    progressTracker,
                    Pools.DEFAULT,
                    configuration.concurrency()
            );
        }
    }
}
