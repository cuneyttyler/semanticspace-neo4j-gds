package com.semanticspace.shortestpath;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.gds.TestSupport;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.core.concurrency.Pools;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.extension.GdlExtension;
import org.neo4j.gds.extension.GdlGraph;
import org.neo4j.gds.extension.IdFunction;
import org.neo4j.gds.extension.Inject;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static com.semanticspace.shortestpath.PathTestUtil.expected;

@GdlExtension
final class DijkstraMultiplePairsTest {

    @GdlGraph
    private static final String DUMMY = "()";

    static ImmutableDijkstraMultiplePairsConfig.Builder defaultMultiplePairsConfigBuilder() {
        return ImmutableDijkstraMultiplePairsConfig.builder()
                .concurrency(1);
    }

    static Stream<Arguments> expectedMemoryEstimation() {
        return Stream.of(
                // trackRelationships = false
                Arguments.of(1_000, false, 32_744L),
                Arguments.of(1_000_000, false, 32_250_488L),
                Arguments.of(1_000_000_000, false, 32_254_883_400L),
                // trackRelationships = true
                Arguments.of(1_000, true, 48_960L),
                Arguments.of(1_000_000, true, 48_250_704L),
                Arguments.of(1_000_000_000, true, 48_257_325_072L)
        );
    }

    @Nested
    class Graph2 {

        // https://www.cise.ufl.edu/~sahni/cop3530/slides/lec326.pdf without relationship id 14
        @GdlGraph
        private static final String DB_CYPHER2 =
                "CREATE" +
                        "  (n0:Label)" +
                        ", (n1:Label)" +
                        ", (n2:Label)" +
                        ", (n3:Label)" +
                        ", (n4:Label)" +
                        ", (n5:Label)" +
                        ", (n6:Label)" +

                        ", (n0)-[:TYPE {cost: 6}]->(n1)" +
                        ", (n0)-[:TYPE {cost: 2}]->(n2)" +
                        ", (n0)-[:TYPE {cost: 16}]->(n3)" +
                        ", (n1)-[:TYPE {cost: 4}]->(n4)" +
                        ", (n1)-[:TYPE {cost: 5}]->(n3)" +
                        ", (n2)-[:TYPE {cost: 7}]->(n1)" +
                        ", (n2)-[:TYPE {cost: 3}]->(n4)" +
                        ", (n2)-[:TYPE {cost: 8}]->(n5)" +
                        ", (n3)-[:TYPE {cost: 7}]->(n2)" +
                        ", (n4)-[:TYPE {cost: 4}]->(n3)" +
                        ", (n4)-[:TYPE {cost: 10}]->(n6)" +
                        ", (n5)-[:TYPE {cost: 1}]->(n6)";

        @Inject
        private Graph graph;

        @Inject
        private IdFunction idFunction;

        @Test
        void runTest() {
            var expected = Arrays.asList(
                    expected(idFunction, 0, new double[]{0.0, 2.0, 10.0, 11.0}, "n0", "n2", "n5", "n6"),
                    expected(idFunction, 1, new double[]{0.0, 3.0}, "n2", "n4")
            );

            List<Long> sourceNodes = Arrays.asList(idFunction.of("n0"), idFunction.of("n2"));
            List<Long> targetNodes = Arrays.asList(idFunction.of("n6"), idFunction.of("n4"));
            var config = defaultMultiplePairsConfigBuilder()
                    .sourceNodes(sourceNodes)
                    .targetNodes(targetNodes)
                    .build();

            var paths = DijkstraMultiplePairs
                    .createInstance(graph, config, Optional.empty(), ProgressTracker.NULL_TRACKER, Pools.DEFAULT, 4)
                    .compute();

            Iterator it = expected.iterator();
            paths.forEachPath((path) -> assertEquals(it.next(), path));
        }
    }

    @Nested
    class Graph3 {

        @GdlGraph
        private static final String DB_CYPHER2 =
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

        @Inject
        private Graph graph;

        @Inject
        private IdFunction idFunction;

        @Test
        void runTest() {
            var expected = Arrays.asList(
                    expected(idFunction, 0, new double[]{0.0, 1.0}, "SteveJobs", "Apple"),
                    expected(idFunction, 1, new double[]{0.0, 1.0, 2.0, 3.0, 4.0}, "SteveJobs", "Apple", "AppleTV", "HDMI", "Microsoft"),
                    expected(idFunction, 2, new double[]{0.0, 1.0, 2.0, 3.0}, "SteveJobs", "Pixar", "PixarRenderman", "Linux"),
                    expected(idFunction, 3, new double[]{0.0, 1.0, 2.0}, "SteveJobs", "DemocraticParty", "BarackObama"),
                    expected(idFunction, 4, new double[]{0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0}, "SteveJobs", "SteveJobsBiologicalFather", "UniversityOfWisconsin", "HigherLearningCommission", "Arizona", "MarkKelly", "ScottKelly", "Expedition26", "ISS")
            );

            List<Long> sourceNodes = Arrays.asList(
                    idFunction.of("SteveJobs"), idFunction.of("SteveJobs"),
                    idFunction.of("SteveJobs"), idFunction.of("SteveJobs"), idFunction.of("SteveJobs"));
            List<Long> targetNodes = Arrays.asList(
                    idFunction.of("Apple"), idFunction.of("Microsoft"),
                    idFunction.of("Linux"), idFunction.of("BarackObama"), idFunction.of("ISS"));
            var config = defaultMultiplePairsConfigBuilder()
                    .sourceNodes(sourceNodes)
                    .targetNodes(targetNodes)
                    .build();

            var paths = DijkstraMultiplePairs
                    .createInstance(graph, config, Optional.empty(), ProgressTracker.NULL_TRACKER, Pools.DEFAULT, 4)
                    .compute();

            Iterator it = expected.iterator();
            paths.forEachPath((path) -> assertEquals(it.next(), path));
        }
    }
}
