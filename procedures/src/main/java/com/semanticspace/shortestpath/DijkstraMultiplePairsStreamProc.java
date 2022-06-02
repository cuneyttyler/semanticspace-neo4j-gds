package com.semanticspace.shortestpath;

import org.neo4j.gds.GraphAlgorithmFactory;
import org.neo4j.gds.core.CypherMapWrapper;
import org.neo4j.gds.executor.GdsCallable;
import org.neo4j.gds.paths.ShortestPathStreamProc;
import org.neo4j.gds.paths.StreamResult;
import org.neo4j.gds.paths.dijkstra.config.ShortestPathDijkstraStreamConfig;
import org.neo4j.gds.results.MemoryEstimateResult;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.stream.Stream;

import static org.neo4j.gds.executor.ExecutionMode.STREAM;
import static org.neo4j.procedure.Mode.READ;

@GdsCallable(name = "semanticspace.gds.dijkstraMultiplePairs.stream", description = "", executionMode = STREAM)
public class DijkstraMultiplePairsStreamProc extends ShortestPathStreamProc<DijkstraMultiplePairs, DijkstraMultiplePairsConfig> {

    @Procedure(name = "semanticspace.gds.dijkstraMultiplePairs.stream", mode = READ)
    @Description("")
    public Stream<StreamResult> stream(
            @Name(value = "graphName") String graphName,
            @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        return stream(compute(graphName, configuration, false, true));
    }

    @Procedure(name = "semanticspace.gds.dijkstraMultiplePairs.stream.estimate", mode = READ)
    @Description("")
    public Stream<MemoryEstimateResult> estimate(
            @Name(value = "graphNameOrConfiguration") Object graphNameOrConfiguration,
            @Name(value = "algoConfiguration") Map<String, Object> algoConfiguration
    ) {
        return computeEstimate(graphNameOrConfiguration, algoConfiguration);
    }

    @Override
    protected DijkstraMultiplePairsConfig newConfig(String username, CypherMapWrapper config) {
        return DijkstraMultiplePairsConfig.of(config);
    }

    @Override
    public GraphAlgorithmFactory<DijkstraMultiplePairs, DijkstraMultiplePairsConfig> algorithmFactory() {
        return new DijkstraMultiplePairsFactory.MultipleSourceDijkstraFactory<>();
    }
}
