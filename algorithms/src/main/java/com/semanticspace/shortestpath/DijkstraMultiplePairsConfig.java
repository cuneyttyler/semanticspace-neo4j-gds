package com.semanticspace.shortestpath;

import org.neo4j.gds.annotation.Configuration;
import org.neo4j.gds.annotation.ValueClass;
import org.neo4j.gds.config.AlgoBaseConfig;
import org.neo4j.gds.config.RelationshipWeightConfig;
import org.neo4j.gds.config.SourceNodesConfig;
import org.neo4j.gds.config.TargetNodesConfig;
import org.neo4j.gds.core.CypherMapWrapper;
import org.neo4j.gds.paths.ShortestPathBaseConfig;
import org.neo4j.gds.paths.TrackRelationshipsConfig;

import java.util.List;

@ValueClass
@Configuration
public interface DijkstraMultiplePairsConfig extends SourceNodesConfig, TargetNodesConfig, AlgoBaseConfig,RelationshipWeightConfig, TrackRelationshipsConfig {

    static DijkstraMultiplePairsConfig of(CypherMapWrapper userInput) {
        return new DijkstraMultiplePairsConfigImpl(userInput);
    }
}
