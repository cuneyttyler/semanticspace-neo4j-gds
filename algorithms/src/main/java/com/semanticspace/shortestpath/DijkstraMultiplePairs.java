package com.semanticspace.shortestpath;

import com.carrotsearch.hppc.BitSet;
import com.carrotsearch.hppc.DoubleArrayDeque;
import com.carrotsearch.hppc.LongArrayDeque;
import org.apache.commons.lang3.mutable.MutableInt;
import org.neo4j.gds.Algorithm;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.api.RelationshipIterator;
import org.neo4j.gds.core.concurrency.ParallelUtil;
import org.neo4j.gds.core.utils.TerminationFlag;
import org.neo4j.gds.core.utils.mem.MemoryEstimation;
import org.neo4j.gds.core.utils.mem.MemoryEstimations;
import org.neo4j.gds.core.utils.paged.HugeLongLongMap;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.core.utils.queue.HugeLongPriorityQueue;
import org.neo4j.gds.mem.MemoryUsage;
import org.neo4j.gds.paths.ImmutablePathResult;
import org.neo4j.gds.paths.PathResult;
import org.neo4j.gds.paths.dijkstra.DijkstraResult;

import java.util.*;
import java.util.function.LongToDoubleFunction;
import java.util.concurrent.ExecutorService;

import static com.semanticspace.shortestpath.DijkstraMultiplePairs.TraversalState.CONTINUE;
import static com.semanticspace.shortestpath.DijkstraMultiplePairs.TraversalState.EMIT_AND_STOP;
import static java.util.concurrent.TimeUnit.*;

public class DijkstraMultiplePairs extends Algorithm<DijkstraResult> {
    public static final String DESCRIPTION_SOURCE_TARGET = "The Dijkstra shortest path algorithm computes the shortest (weighted) path between one node and any other node in the graph.";

    private static final long NO_RELATIONSHIP = -1;

    private final Graph graph;

    private static List<Long> sourceNodes;

    private static List<Long> targetNodes;

    private final List<PathResult> allPaths = new ArrayList<>();

    // True, iff the algo should track relationship ids.org.neo4
    // A relationship id is the index of a relationship
    // in the adjacency list of a single node.
    private final boolean trackRelationships;
    // relationship ids (null, if trackRelationships is false)
    private final HugeLongLongMap relationships;

    // path id increasing in order of exploration
    private long pathIndex;
    // returns true if the given relationship should be traversed
    private RelationshipFilter relationshipFilter = (sourceId, targetId, relationshipId) -> true;

    private final ExecutorService executorService;
    private final int concurrency;


    /**
     * Configure Dijkstra to compute at most one source-target shortest path.
     */
    public static DijkstraMultiplePairs createInstance(
            Graph graph,
            DijkstraMultiplePairsConfig config,
            Optional<HeuristicFunction> heuristicFunction,
            ProgressTracker progressTracker,
            ExecutorService executorService,
            int concurrency
    ) {
        sourceNodes = config.sourceNodes();
        targetNodes = config.targetNodes();

        return new DijkstraMultiplePairs(
                graph,
                sourceNodes,
                config.trackRelationships(),
                heuristicFunction,
                progressTracker,
                executorService,
                concurrency
        );
    }

    public static MemoryEstimation memoryEstimation(boolean trackRelationships) {
        var builder = MemoryEstimations.builder(org.neo4j.gds.paths.dijkstra.Dijkstra.class)
                .add("priority queue", HugeLongPriorityQueue.memoryEstimation())
                .add("reverse path", HugeLongLongMap.memoryEstimation());
        if (trackRelationships) {
            builder.add("relationship ids", HugeLongLongMap.memoryEstimation());
        }
        return builder
                .perNode("visited set", MemoryUsage::sizeOfBitset)
                .build();
    }

    private DijkstraMultiplePairs(
            Graph graph,
            List<Long> sourceNodes,
            boolean trackRelationships,
            Optional<HeuristicFunction> heuristicFunction,
            ProgressTracker progressTracker,
            ExecutorService executorService,
            int concurrency
    ) {
        super(progressTracker);
        this.graph = graph;
        this.trackRelationships = trackRelationships;
        this.relationships = trackRelationships ? new HugeLongLongMap() : null;
        this.pathIndex = 0L;
        this.concurrency = concurrency;
        this.executorService = executorService;
    }

    int counter = 0;

    public DijkstraMultiplePairs withRelationshipFilter(RelationshipFilter relationshipFilter) {
        this.relationshipFilter = this.relationshipFilter.and(relationshipFilter);
        return this;
    }


    public DijkstraResult compute() {
        int index = 0;

        List<PairTask> taskList = new ArrayList<>();
        for (int i = 0; i < sourceNodes.size(); i++) {
            PairTask task = new PairTask(i, sourceNodes.get(i), targetNodes.get(i));
            taskList.add(task);
        }

        progressTracker.beginSubTask();
        ParallelUtil.runWithConcurrency(concurrency, taskList, 1, MICROSECONDS, terminationFlag, executorService);

        allPaths.sort((o1, o2) -> o1.index() > o2.index() ? 1 : -1);
        return new DijkstraResult(allPaths.stream(), progressTracker::endSubTask);
    }

    private static final long[] EMPTY_ARRAY = new long[0];


    class PairTask implements Runnable {
        private final int pairIndex;

        private final TraversalPredicate traversalPredicate;

        private TraversalState traversalState;
        private final HugeLongLongMap predecessors;

        private final HugeLongPriorityQueue queue;

        private final BitSet visited;

        private final long sourceNode;

        private final long targetNode;

        private final RelationshipIterator localRelationshipIterator;

        private int iteration = 0;

        public PairTask(int pairIndex, long sourceNode, long targetNode) {
            this.pairIndex = pairIndex;
            this.traversalPredicate = (node) -> node == targetNode ? EMIT_AND_STOP : CONTINUE;
            this.traversalState = CONTINUE;
            this.predecessors = new HugeLongLongMap();
            this.localRelationshipIterator = graph.concurrentCopy();
            this.queue = HugeLongPriorityQueue.min(graph.nodeCount());
            this.visited = new BitSet();
            this.sourceNode = sourceNode;
            this.targetNode = targetNode;

            queue.add(sourceNode, 0.0);
        }

        @Override
        public void run() {
            progressTracker.logMessage("Running task for pair " + this.pairIndex);

            var pathResultBuilder = ImmutablePathResult.builder();

            List<PathResult> paths = new ArrayList<>();

            PathResult path = next(this.pairIndex, traversalPredicate, pathResultBuilder);
            while (path != PathResult.EMPTY) {
                paths.add(path);
                path = next(this.pairIndex, traversalPredicate, pathResultBuilder);
            }

            synchronized (allPaths) {
                allPaths.addAll(paths);
            }

        }

        private PathResult next(int pairIndex, TraversalPredicate traversalPredicate, ImmutablePathResult.Builder pathResultBuilder) {
            var relationshipId = new MutableInt();

            while (!queue.isEmpty() && running() && traversalState != EMIT_AND_STOP) {
                var node = queue.pop();
                var cost = queue.cost(node);
                visited.set(node);

                if(iteration == 0) {
                    progressTracker.logMessage(pairIndex + ". Node: ");
                }

                progressTracker.logProgress();

                localRelationshipIterator.forEachRelationship(
                        node,
                        1.0D,
                        (source, target, weight) -> {
                            if(iteration == 0) {
                                progressTracker.logMessage(pairIndex + ". Source: " + source + ", Target: " + target + ", Cost: " + (cost+weight));
                            }
                            updateCost(pairIndex, source, target, relationshipId.intValue(), weight + cost);
                            relationshipId.increment();
                            return true;
                        }
                );

                if(iteration++ == 0) {
                    progressTracker.logMessage(pairIndex + ". Start node degree: " + relationshipId.intValue());
                }

                traversalState = traversalPredicate.apply(node);
                if (traversalState == EMIT_AND_STOP) {
                    progressTracker.logMessage(pairIndex + ". Returning Result");
                    return pathResult(pairIndex, node, pathResultBuilder);
                }
            }

            if(queue.isEmpty()) {
                progressTracker.logMessage(pairIndex + ". Queue is empty");
            }


            return PathResult.EMPTY;
        }

        private void updateCost(int pairIndex, long source, long target, long relationshipId, double newCost) {
            // target has been visited, we already have a shortest path
            if (visited.get(target)) {
                return;
            }

            if (!queue.containsElement(target)) {
                // we see target for the first time
                queue.add(target, newCost);
                predecessors.put(target, source);
                if (trackRelationships) {
                    relationships.put(target, relationshipId);
                }
            } else if (newCost < queue.cost(target)) {
                // we see target again and found a shorter path to target
                queue.set(target, newCost);
                predecessors.put(target, source);
                if (trackRelationships) {
                    relationships.put(target, relationshipId);
                }
            }
        }


        private PathResult pathResult(int pairIndex, long target, ImmutablePathResult.Builder pathResultBuilder) {
            // TODO: use LongArrayList and then ArrayUtils.reverse
            var pathNodeIds = new LongArrayDeque();
            var relationshipIds = trackRelationships ? new LongArrayDeque() : null;
            var costs = new DoubleArrayDeque();

            // We backtrack until we reach the source node.
            // The source node is either given by Dijkstra
            // or adjusted by Yen's algorithm.
            var pathStart = sourceNode;
            var lastNode = target;
            var prevNode = lastNode;

            while (true) {
                pathNodeIds.addFirst(lastNode);
                costs.addFirst(queue.cost(lastNode));

                // Break if we reach the end by hitting the source node.
                // This happens either by not having a predecessor or by
                // arriving at the predecessor if we are a spur path from
                // Yen's algorithm.
                if (lastNode == pathStart) {
                    break;
                }

                prevNode = lastNode;
                lastNode = predecessors.getOrDefault(lastNode, pathStart);
                if (trackRelationships) {
                    relationshipIds.addFirst(relationships.getOrDefault(prevNode, NO_RELATIONSHIP));
                }
            }

            return pathResultBuilder
                    .index(pairIndex)
                    .sourceNode(pathStart)
                    .targetNode(target)
                    .nodeIds(pathNodeIds.toArray())
                    .relationshipIds(trackRelationships ? relationshipIds.toArray() : EMPTY_ARRAY)
                    .costs(costs.toArray())
                    .build();
        }

    }


    @Override
    public void release() {
        // We do not release, since the result
        // is lazily computed when the consumer
        // iterates over the stream.
    }

    enum TraversalState {
        EMIT_AND_STOP,
        CONTINUE,
    }

    @FunctionalInterface
    public interface TraversalPredicate {
        TraversalState apply(long nodeId);
    }

    @FunctionalInterface
    public interface RelationshipFilter {
        boolean test(long source, long target, long relationshipId);

        default RelationshipFilter and(RelationshipFilter after) {
            return (sourceNodeId, targetNodeId, relationshipId) ->
                    this.test(sourceNodeId, targetNodeId, relationshipId) &&
                            after.test(sourceNodeId, targetNodeId, relationshipId);
        }
    }

    private static HugeLongPriorityQueue minPriorityQueue(long capacity, HeuristicFunction heuristicFunction) {
        return new HugeLongPriorityQueue(capacity) {
            @Override
            protected boolean lessThan(long a, long b) {
                return heuristicFunction.applyAsDouble(a) + costValues.get(a) < heuristicFunction.applyAsDouble(b) + costValues.get(b);
            }
        };
    }

    @FunctionalInterface
    public interface HeuristicFunction extends LongToDoubleFunction {
    }
}