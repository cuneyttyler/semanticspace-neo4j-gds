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

abstract class DijkstraMultiplePairsProcTest2<CONFIG extends DijkstraMultiplePairsConfig> extends BaseProcTest implements
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

    private static String SOURCES_NODE_KEY = "sourceNodes";
    private static String TARGETS_NODE_KEY = "targetNodes";

    @Neo4jGraph
    private static final String DB_CYPHER =
            "CREATE" +
                    "  (SteveJobs:Label)" +
                    ", (BarackObama:Label)" +
                    ", (Apple:Label)" +
                    ", (Microsoft:Label)" +
                    ", (Linux:Label)" +
                    ", (ISS:Label)" +
                    ", (DemocraticParty:Label)" +
                    ", (MarkKelly:Label)" +
                    ", (ScottKelly:Label)" +
                    ", (Expedition26:Label)" +
                    ", (Arizona:Label)" +
                    ", (HigherLearningCommission:Label)" +
                    ", (UniversityOfWisconsin:Label)" +
                    ", (SteveJobsBiologicalFather:Label)" +
                    ", (Pixar:Label)" +
                    ", (PixarRenderman:Label)" +
                    ", (AppleTV:Label)" +
                    ", (HDMI:Label)" +

                    ", (SteveJobs)-[:LINK {cost: 1}]->(DemocraticParty)" +
                    ", (SteveJobs)-[:LINK {cost: 1}]->(SteveJobsBiologicalFather)" +
                    ", (SteveJobs)-[:LINK {cost: 1}]->(Apple)" +
                    ", (SteveJobs)-[:LINK {cost: 1}]->(Pixar)" +
                    ", (Apple)-[:LINK {cost: 1}]->(SteveJobs)" +
                    ", (DemocraticParty)-[:LINK {cost: 1}]->(BarackObama)" +
                    ", (BarackObama)-[:LINK {cost: 1}]->(DemocraticParty)" +
                    ", (MarkKelly)-[:LINK {cost: 1}]->(DemocraticParty)" +
                    ", (MarkKelly)-[:LINK {cost: 1}]->(ScottKelly)" +
                    ", (Arizona)-[:LINK {cost: 1}]->(MarkKelly)" +
                    ", (ScottKelly)-[:LINK {cost: 1}]->(MarkKelly)" +
                    ", (ScottKelly)-[:LINK {cost: 1}]->(Expedition26)" +
                    ", (Expedition26)-[:LINK {cost: 1}]->(ScottKelly)" +
                    ", (Expedition26)-[:LINK {cost: 1}]->(ISS)" +
                    ", (ISS)-[:LINK {cost: 1}]->(Expedition26)" +
                    ", (HigherLearningCommission)-[:LINK {cost: 1}]->(Arizona)" +
                    ", (UniversityOfWisconsin)-[:LINK {cost: 1}]->(HigherLearningCommission)" +
                    ", (SteveJobsBiologicalFather)-[:LINK {cost: 1}]->(UniversityOfWisconsin)" +
                    ", (Pixar)-[:LINK {cost: 1}]->(PixarRenderman)" +
                    ", (PixarRenderman)-[:LINK {cost: 1}]->(Pixar)" +
                    ", (PixarRenderman)-[:LINK {cost: 1}]->(Linux)" +
                    ", (Apple)-[:LINK {cost: 1}]->(AppleTV)" +
                    ", (AppleTV)-[:LINK {cost: 1}]->(Apple)" +
                    ", (HDMI)-[:LINK {cost: 1}]->(Apple)" +
                    ", (AppleTV)-[:LINK {cost: 1}]->(HDMI)" +
                    ", (HDMI)-[:LINK {cost: 1}]->(Microsoft)";

    @BeforeEach
    void setup() throws Exception {
        registerProcedures(
                getProcedureClazz(),
                GraphProjectProc.class
        );

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
