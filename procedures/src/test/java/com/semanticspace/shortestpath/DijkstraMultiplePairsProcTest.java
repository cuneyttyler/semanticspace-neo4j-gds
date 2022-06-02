package com.semanticspace.shortestpath;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.neo4j.gds.AlgoBaseProcTest;
import org.neo4j.gds.BaseProcTest;
import org.neo4j.gds.GdsCypher;
import org.neo4j.gds.MemoryEstimateTest;
import org.neo4j.gds.catalog.GraphProjectProc;
import org.neo4j.gds.core.CypherMapWrapper;
import org.neo4j.gds.core.loading.GraphStoreCatalog;
import org.neo4j.gds.extension.Neo4jGraph;
import org.neo4j.gds.paths.dijkstra.DijkstraResult;
import org.neo4j.kernel.internal.GraphDatabaseAPI;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

abstract class DijkstraMultiplePairsProcTest<CONFIG extends DijkstraMultiplePairsConfig> extends BaseProcTest implements
        AlgoBaseProcTest<DijkstraMultiplePairs, CONFIG, DijkstraResult>,
        MemoryEstimateTest<DijkstraMultiplePairs, CONFIG, DijkstraResult> {

    @TestFactory
    final Stream<DynamicTest> configTests() {
        return modeSpecificConfigTests();
    }

    Stream<DynamicTest> modeSpecificConfigTests() {
        return Stream.empty();
    }

    protected static final String GRAPH_NAME = "graph";
    long id1, id2, id3, id4, id5, id6, id7;
    static long[] ids0;
    static long[] ids1;
    static double[] costs0;

    static double[] costs1;

    private static String SOURCES_NODE_KEY = "sourceNodes";
    private static String TARGETS_NODE_KEY = "targetNodes";

    @Neo4jGraph
    private static final String DB_CYPHER =
            "CREATE" +
            "  (n1:Label)" +
            ", (n2:Label)" +
            ", (n3:Label)" +
            ", (n4:Label)" +
            ", (n5:Label)" +
            ", (n6:Label)" +
            ", (n7:Label)" +

            ", (n1)-[:TYPE {cost: 6}]->(n2)" +
            ", (n1)-[:TYPE {cost: 2}]->(n3)" +
            ", (n1)-[:TYPE {cost: 16}]->(n4)" +
            ", (n2)-[:TYPE {cost: 4}]->(n5)" +
            ", (n2)-[:TYPE {cost: 5}]->(n4)" +
            ", (n3)-[:TYPE {cost: 7}]->(n2)" +
            ", (n3)-[:TYPE {cost: 3}]->(n5)" +
            ", (n3)-[:TYPE {cost: 8}]->(n6)" +
            ", (n4)-[:TYPE {cost: 7}]->(n3)" +
            ", (n5)-[:TYPE {cost: 4}]->(n4)" +
            ", (n5)-[:TYPE {cost: 10}]->(n7)" +
            ", (n6)-[:TYPE {cost: 1}]->(n7)";

    @BeforeEach
    void setup() throws Exception {
        registerProcedures(
                getProcedureClazz(),
                GraphProjectProc.class
        );

        id1 = idFunction.of("n1");
        id2 = idFunction.of("n2");
        id3 = idFunction.of("n3");
        id4 = idFunction.of("n4");
        id5 = idFunction.of("n5");
        id6 = idFunction.of("n6");
        id7 = idFunction.of("n7");

        ids0 = new long[]{id1, id3, id6, id7};
        ids1 = new long[]{id3, id5};
        costs0 = new double[]{0.0, 2.0, 10.0, 11.0};
        costs1 = new double[]{0.0, 3.0};

        runQuery(GdsCypher.call(GRAPH_NAME)
                .graphProject()
                .withNodeLabel("Label")
                .withAnyRelationshipType()
                .withRelationshipProperty("cost")
                .yields());
    }

    @AfterEach
    void teardown() {
        GraphStoreCatalog.removeAllLoadedGraphs();
    }

    @Override
    public GraphDatabaseAPI graphDb() {
        return db;
    }

    @Override
    public CypherMapWrapper createMinimalConfig(CypherMapWrapper mapWrapper) {
        return mapWrapper
                .withEntry(SOURCES_NODE_KEY, Arrays.asList(idFunction.of("n1"),idFunction.of("n7")))
                .withEntry(TARGETS_NODE_KEY, Arrays.asList(idFunction.of("n3"),idFunction.of("n5")));
    }

    @Override
    public void assertResultEquals(DijkstraResult result1, DijkstraResult result2) {
        assertEquals(result1.pathSet(), result2.pathSet());
    }

    @Override
    public boolean releaseAlgorithm() {
        return false;
    }

    @Test
    @Disabled
    @Override
    public void testRunOnEmptyGraph() {
        // graph must not be empty
    }
}
