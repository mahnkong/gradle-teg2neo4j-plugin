package com.github.mahnkong.gteg2neo4j

import org.junit.rules.ExternalResource
import org.neo4j.harness.ServerControls
import org.neo4j.harness.TestServerBuilders

import java.nio.file.Files
import java.nio.file.Path

/**
 * Created by mahnkong on 07.03.2016.
 */
public class Neo4jTestServer extends ExternalResource {

    private ServerControls neo4jControl;
    private Path dataDirectory;

    @Override
    protected void before() throws Throwable {
        dataDirectory = Files.createTempDirectory("neo4jTempDir");
        neo4jControl = TestServerBuilders.newInProcessBuilder(dataDirectory.toFile()).newServer()
    }

    @Override
    protected void after() {
        neo4jControl.close()
        Files.delete(dataDirectory)
    }

    ServerControls getNeo4jControl() {
        return neo4jControl
    }
}
