package org.mahnkong

import org.neo4j.jdbc.Driver
import org.neo4j.jdbc.Neo4jConnection

/**
 * Created by mahnkong on 27.02.2016.
 */
class Neo4jClient {

    final CYPHER_CREATE_NOTE = "MERGE (n:buildtask {name: ?, build: ?}) RETURN n"
    final CYPHER_CREATE_RELATIONSHIP = "MATCH (a:buildtask), (b:buildtask) WHERE a.name = ? AND a.build = ? AND b.name = ? AND b.build = ? CREATE (a)-[r:dependsOn]->(b) RETURN r"

    def connection
    def logger

    Neo4jClient(server, user, password, logger) {
        this.connection = createConnection(server, user, password)
        this.logger = logger
    }

    def createTaskNode(name, buildId) {
        logger.info(":: Adding task '$name' to db")
        def createTaskStmt = connection.prepareStatement(CYPHER_CREATE_NOTE)
        createTaskStmt.setString(1, name)
        createTaskStmt.setString(2, buildId)
        createTaskStmt.execute()
        createTaskStmt.close()
    }

    def createTaskRelationship(task, dependsOnTask, buildId) {
        logger.info(":: Adding relationship between tasks '$task' and '$dependsOnTask'")
        def createRelationshipStmt = connection.prepareStatement(CYPHER_CREATE_RELATIONSHIP)
        createRelationshipStmt.setString(1, task)
        createRelationshipStmt.setString(2, buildId)
        createRelationshipStmt.setString(3, dependsOnTask)
        createRelationshipStmt.setString(4, buildId)
        createRelationshipStmt.execute()
        createRelationshipStmt.close()
    }

    private static Neo4jConnection createConnection(server, user, password) {
        Properties connectionProps = new Properties();
        connectionProps.put("user", user);
        connectionProps.put("password", password);
        return new Driver().connect("jdbc:neo4j://${server}", connectionProps);
    }

    def close() {
        logger.info(":: Closing connection")
        connection.close()
    }
}
