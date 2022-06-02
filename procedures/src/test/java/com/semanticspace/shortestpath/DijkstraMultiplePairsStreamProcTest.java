package com.semanticspace.shortestpath;

import org.junit.jupiter.api.Test;
import org.neo4j.gds.AlgoBaseProc;
import org.neo4j.gds.GdsCypher;
import org.neo4j.gds.compat.Neo4jProxy;
import org.neo4j.gds.compat.TestLog;
import org.neo4j.gds.core.CypherMapWrapper;
import org.neo4j.gds.paths.PathFactory;
import org.neo4j.gds.paths.StreamResult;
import org.neo4j.gds.paths.dijkstra.Dijkstra;
import org.neo4j.gds.paths.dijkstra.DijkstraResult;
import org.neo4j.gds.paths.dijkstra.config.ShortestPathDijkstraStreamConfig;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.logging.Log;
import org.neo4j.logging.LogProvider;
import org.neo4j.test.TestDatabaseManagementServiceBuilder;
import org.neo4j.test.extension.ExtensionCallback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.neo4j.gds.compat.GraphDatabaseApiProxy.runInTransaction;
import static org.neo4j.gds.compat.GraphDatabaseApiProxy.runQueryWithoutClosingTheResult;
import static org.neo4j.gds.utils.StringFormatting.formatWithLocale;

class DijkstraMultiplePairsStreamProcTest extends DijkstraMultiplePairsProcTest<DijkstraMultiplePairsConfig> {

    TestLog testLog;

    @Override
    public Class<? extends AlgoBaseProc<DijkstraMultiplePairs, DijkstraResult, DijkstraMultiplePairsConfig, ?>> getProcedureClazz() {
        return DijkstraMultiplePairsStreamProc.class;
    }

    @Override
    public DijkstraMultiplePairsConfig createConfig(CypherMapWrapper mapWrapper) {
        return DijkstraMultiplePairsConfig.of(mapWrapper);
    }

    @Override
    @ExtensionCallback
    protected void configuration(TestDatabaseManagementServiceBuilder builder) {
        super.configuration(builder);
        testLog = Neo4jProxy.testLog();
        builder.setUserLogProvider(new LogProvider() {
            @Override
            public Log getLog(Class<?> loggingClass) {
                return testLog;
            }

            @Override
            public Log getLog(String name) {
                return testLog;
            }
        });
    }

    @Test
    void testStream() {
        var config = createConfig(createMinimalConfig(CypherMapWrapper.empty()));

        List<Long> sourceNodes = Arrays.asList(idFunction.of("n1"), idFunction.of("n3"));
        List<Long> targetNodes = Arrays.asList(idFunction.of("n7"), idFunction.of("n5"));

        var query = GdsCypher.call("graph")
                .algo("semanticspace.gds.dijkstraMultiplePairs")
                .streamMode()
                .addParameter("sourceNodes", sourceNodes)
                .addParameter("targetNodes", targetNodes)
                .addParameter("relationshipWeightProperty", "cost")
                .yields();

        runInTransaction(db, tx -> {
            PathFactory.RelationshipIds.set(0);
            var expectedPath1 = PathFactory.create(
                    tx,
                    ids0,
                    costs0,
                    RelationshipType.withName(formatWithLocale("PATH_0")), StreamResult.COST_PROPERTY_NAME
            );
            var expectedPath2 = PathFactory.create(
                    tx,
                    ids1,
                    costs1,
                    RelationshipType.withName(formatWithLocale("PATH_0")), StreamResult.COST_PROPERTY_NAME
            );
            var expected = Arrays.asList(
                    Map.of(
                            "index", 0L,
                            "sourceNode", idFunction.of("n1"),
                            "targetNode", idFunction.of("n7"),
                            "totalCost", 11.0D,
                            "costs", Arrays.stream(costs0).boxed().collect(Collectors.toList()),
                            "nodeIds", Arrays.stream(ids0).boxed().collect(Collectors.toList()),
                            "path", expectedPath1
                    ),
                    Map.of(
                            "index", 1L,
                            "sourceNode", idFunction.of("n3"),
                            "targetNode", idFunction.of("n5"),
                            "totalCost", 3.0D,
                            "costs", Arrays.stream(costs1).boxed().collect(Collectors.toList()),
                            "nodeIds", Arrays.stream(ids1).boxed().collect(Collectors.toList()),
                            "path", expectedPath2
                    ));
            PathFactory.RelationshipIds.set(0);
            assertCypherResult(query, expected);
        });
    }




    @Test
    void testLazyComputationLoggingFinishes() {
        var config = createConfig(createMinimalConfig(CypherMapWrapper.empty()));

        List<Long> sourceNodes = Arrays.asList(idFunction.of("n1"), idFunction.of("n3"));
        List<Long> targetNodes = Arrays.asList(idFunction.of("n7"), idFunction.of("n5"));

        var query = GdsCypher.call("graph")
                .algo("semanticspace.gds.dijkstraMultiplePairs.dijkstra")
                .streamMode()
                .addParameter("sourceNodes", config.sourceNodes())
                .addParameter("targetNodes", config.targetNodes())
                .addParameter("relationshipWeightProperty", "cost")
                .yields();

        runInTransaction(db, tx -> runQueryWithoutClosingTheResult(tx, query, Map.of()).next());

        var messages = testLog.getMessages(TestLog.INFO);
        assertThat(messages.get(messages.size() - 1)).contains(":: Finished");
    }
}
