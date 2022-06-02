package com.semanticspace.shortestpath;

import org.junit.jupiter.api.Test;
import org.neo4j.gds.AlgoBaseProc;
import org.neo4j.gds.GdsCypher;
import org.neo4j.gds.compat.Neo4jProxy;
import org.neo4j.gds.compat.TestLog;
import org.neo4j.gds.core.CypherMapWrapper;
import org.neo4j.gds.paths.PathFactory;
import org.neo4j.gds.paths.StreamResult;
import org.neo4j.gds.paths.dijkstra.DijkstraResult;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.logging.Log;
import org.neo4j.logging.LogProvider;
import org.neo4j.test.TestDatabaseManagementServiceBuilder;
import org.neo4j.test.extension.ExtensionCallback;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.neo4j.gds.compat.GraphDatabaseApiProxy.runInTransaction;
import static org.neo4j.gds.compat.GraphDatabaseApiProxy.runQueryWithoutClosingTheResult;
import static org.neo4j.gds.utils.StringFormatting.formatWithLocale;

class DijkstraMultiplePairsStreamProcTest2 extends DijkstraMultiplePairsProcTest2<DijkstraMultiplePairsConfig> {

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
        List<Long> sourceNodes = Arrays.asList(
                idFunction.of("SteveJobs"), idFunction.of("SteveJobs"),
                idFunction.of("SteveJobs"), idFunction.of("SteveJobs"), idFunction.of("SteveJobs"));
        List<Long> targetNodes = Arrays.asList(
                idFunction.of("Apple"), idFunction.of("Microsoft"),
                idFunction.of("Linux"), idFunction.of("BarackObama"), idFunction.of("ISS"));

        var query = GdsCypher.call("graph")
                .algo("semanticspace.gds.dijkstraMultiplePairs")
                .streamMode()
                .addParameter("sourceNodes", sourceNodes)
                .addParameter("targetNodes", targetNodes)
                .addParameter("relationshipWeightProperty", "cost")
                .yields();

        double[] costs0 = new double[]{0.0, 1.0};
        double[] costs1 = new double[]{0.0, 1.0, 2.0, 3.0, 4.0};
        double[] costs2 = new double[]{0.0, 1.0, 2.0, 3.0};
        double[] costs3 = new double[]{0.0, 1.0, 2.0};
        double[] costs4 = new double[]{0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};

        long[] ids0 = getIds("SteveJobs", "Apple");
        long[] ids1 = getIds("SteveJobs", "Apple", "AppleTV", "HDMI", "Microsoft");
        long[] ids2 = getIds("SteveJobs", "Pixar", "PixarRenderman", "Linux");
        long[] ids3 = getIds("SteveJobs", "DemocraticParty", "BarackObama");
        long[] ids4 = getIds("SteveJobs", "SteveJobsBiologicalFather", "UniversityOfWisconsin", "HigherLearningCommission", "Arizona", "MarkKelly", "ScottKelly", "Expedition26", "ISS");

        runInTransaction(db, tx -> {
            PathFactory.RelationshipIds.set(0);
            var expectedPath0 = PathFactory.create(
                    tx,
                    ids0,
                    costs0,
                    RelationshipType.withName(formatWithLocale("PATH_0")), StreamResult.COST_PROPERTY_NAME
            );
            var expectedPath1 = PathFactory.create(
                    tx,
                    ids1,
                    costs1,
                    RelationshipType.withName(formatWithLocale("PATH_0")), StreamResult.COST_PROPERTY_NAME
            );
            var expectedPath2 = PathFactory.create(
                    tx,
                    ids2,
                    costs2,
                    RelationshipType.withName(formatWithLocale("PATH_0")), StreamResult.COST_PROPERTY_NAME
            );
            var expectedPath3 = PathFactory.create(
                    tx,
                    ids3,
                    costs3,
                    RelationshipType.withName(formatWithLocale("PATH_0")), StreamResult.COST_PROPERTY_NAME
            );
            var expectedPath4 = PathFactory.create(
                    tx,
                    ids4,
                    costs4,
                    RelationshipType.withName(formatWithLocale("PATH_0")), StreamResult.COST_PROPERTY_NAME
            );
            var expected = Arrays.asList(
                    Map.of(
                            "index", 0L,
                            "sourceNode", idFunction.of("SteveJobs"),
                            "targetNode", idFunction.of("Apple"),
                            "totalCost", 1.0D,
                            "costs", Arrays.stream(costs0).boxed().collect(Collectors.toList()),
                            "nodeIds", Arrays.stream(ids0).boxed().collect(Collectors.toList()),
                            "path", expectedPath0
                    ),
                    Map.of(
                            "index", 1L,
                            "sourceNode", idFunction.of("SteveJobs"),
                            "targetNode", idFunction.of("Microsoft"),
                            "totalCost", 4.0D,
                            "costs", Arrays.stream(costs1).boxed().collect(Collectors.toList()),
                            "nodeIds", Arrays.stream(ids1).boxed().collect(Collectors.toList()),
                            "path", expectedPath1
                    ),
                    Map.of(
                            "index", 2L,
                            "sourceNode", idFunction.of("SteveJobs"),
                            "targetNode", idFunction.of("Linux"),
                            "totalCost", 3.0D,
                            "costs", Arrays.stream(costs2).boxed().collect(Collectors.toList()),
                            "nodeIds", Arrays.stream(ids2).boxed().collect(Collectors.toList()),
                            "path", expectedPath2
                    ),
                    Map.of(
                            "index", 3L,
                            "sourceNode", idFunction.of("SteveJobs"),
                            "targetNode", idFunction.of("BarackObama"),
                            "totalCost", 2.0D,
                            "costs", Arrays.stream(costs3).boxed().collect(Collectors.toList()),
                            "nodeIds", Arrays.stream(ids3).boxed().collect(Collectors.toList()),
                            "path", expectedPath3
                    ),
                    Map.of(
                            "index", 4L,
                            "sourceNode", idFunction.of("SteveJobs"),
                            "targetNode", idFunction.of("ISS"),
                            "totalCost", 8.0D,
                            "costs", Arrays.stream(costs4).boxed().collect(Collectors.toList()),
                            "nodeIds", Arrays.stream(ids4).boxed().collect(Collectors.toList()),
                            "path", expectedPath4
                    ));
            PathFactory.RelationshipIds.set(0);
            assertCypherResult(query, expected);
        });
    }

    private long[] getIds(String... nodes) {
        long[] ids = new long[nodes.length];
        int i = 0;
        for(String node: nodes) {
            ids[i++] = idFunction.of(node);
        }

        return ids;
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
