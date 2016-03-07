package com.github.mahnkong.gteg2neo4j

import org.gradle.api.logging.Logger;

/**
 * Created by mahnkong on 05.03.2016.
 */
public class Neo4jClientHelper {

    static Neo4jClient getNeo4jClient(String server) {
        def logger = [info: {}, error: {}, debug: {}, warn: {}] as Logger
        def neo4jClient = new Neo4jClient(server, null, null, logger);
        neo4jClient.connection.autoCommit = true
        return neo4jClient
    }
}
