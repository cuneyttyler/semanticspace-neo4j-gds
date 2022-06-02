package com.semanticspace.shortestpath;

import org.neo4j.gds.annotation.Configuration;
import org.neo4j.gds.annotation.ValueClass;
import org.neo4j.gds.config.AlgoBaseConfig;
import org.neo4j.gds.config.RelationshipWeightConfig;
import org.neo4j.gds.core.CypherMapWrapper;
import org.neo4j.gds.paths.ShortestPathBaseConfig;
import org.neo4j.gds.paths.TrackRelationshipsConfig;

import java.util.List;

@ValueClass
@Configuration
public interface DijkstraMultiplePairsConfig extends AlgoBaseConfig,RelationshipWeightConfig, TrackRelationshipsConfig {
    List<Long> sourceNodes();
    List<Long> targetNodes();

    static DijkstraMultiplePairsConfig of(CypherMapWrapper userInput) {
        return new DijkstraMultiplePairsConfigImpl(userInput);
    }
}
