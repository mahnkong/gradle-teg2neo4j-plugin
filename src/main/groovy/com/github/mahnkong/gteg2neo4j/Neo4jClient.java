package com.github.mahnkong.gteg2neo4j;

import org.gradle.api.Task;
import org.gradle.api.logging.Logger;
import org.neo4j.jdbc.Driver;
import org.neo4j.jdbc.Neo4jConnection;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Properties;

/**
 * Created by mahnkong on 28.02.2016.
 */
public class Neo4jClient {

    private static final String CYPHER_CREATE_NOTE = "MERGE (n:%s {name: ?, build: ?, executed: ?, didWork: ?, skipped: ?, upToDate: ?, failureMsg: ?, insertedAt: ?}) RETURN n";
    private static final String CYPHER_CREATE_DEPENDSON_RELATIONSHIP = "MATCH (a:BuildTask), (b:BuildTask) WHERE a.name = ? AND a.build = ? AND b.name = ? AND b.build = ? CREATE (a)-[r:DEPENDS_ON]->(b) RETURN r";
    private static final String CYPHER_CREATE_FINALIZES_RELATIONSHIP = "MATCH (a:BuildTask), (b:BuildTask) WHERE a.name = ? AND a.build = ? AND b.name = ? AND b.build = ? CREATE (a)-[r:FINALIZES]->(b) RETURN r";

    Neo4jConnection connection;
    Logger logger;
    private long insertTimestamp = Instant.now().toEpochMilli();

    public Neo4jClient(String server, String user, String password, Logger logger) throws SQLException {
        this.connection = createConnection(server, user, password);
        this.connection.setAutoCommit(false);
        this.logger = logger;
    }

    void createTaskNode(Task task, String buildId) throws SQLException {
        logger.info(String.format("%s :: Adding task '%s' to db", Gteg2Neo4jConstants.EXTENSION_NAME.getValue(), task.getPath()));
        String label = "BuildTask" + (task.getState().getFailure() != null ? ":FailedTask" :
            (task.getState().getSkipped() && ! task.getState().getUpToDate() ? ":SkippedTask" :
                (task.getState().getExecuted() ? ":SuccessfulTask" : "")
            )
        );
        try (PreparedStatement createTaskStmt = connection.prepareStatement(String.format(CYPHER_CREATE_NOTE, label))) {
            createTaskStmt.setString(1, task.getPath());
            createTaskStmt.setString(2, buildId);
            createTaskStmt.setBoolean(3, task.getState().getExecuted());
            createTaskStmt.setBoolean(4, task.getState().getDidWork());
            createTaskStmt.setBoolean(5, task.getState().getSkipped());
            createTaskStmt.setBoolean(6, task.getState().getUpToDate());
            createTaskStmt.setString(7, (task.getState().getFailure() != null ? task.getState().getFailure().getMessage() : ""));
            createTaskStmt.setLong(8, insertTimestamp);
            createTaskStmt.execute();
        }
    }

    private void createTaskRelationship(Task left, Task right, String buildId, String query) throws SQLException {
        try (PreparedStatement createRelationshipStmt = connection.prepareStatement(query)) {
            createRelationshipStmt.setString(1, left.getPath());
            createRelationshipStmt.setString(2, buildId);
            createRelationshipStmt.setString(3, right.getPath());
            createRelationshipStmt.setString(4, buildId);
            createRelationshipStmt.execute();
        }
    }

    void createTaskDependsOnRelationship(Task task, Task dependsOnTask, String buildId) throws SQLException {
        logger.info(String.format("%s :: Adding DEPENDS_ON relationship between tasks '%s' and '%s'", Gteg2Neo4jConstants.EXTENSION_NAME.getValue(),task, dependsOnTask));
        createTaskRelationship(task, dependsOnTask, buildId, CYPHER_CREATE_DEPENDSON_RELATIONSHIP);

    }

    void createTaskFinalizesRelationship(Task finalizerTask, Task task, String buildId) throws SQLException {
        logger.info(String.format("%s :: Adding FINALIZES relationship between tasks '%s' and '%s'", Gteg2Neo4jConstants.EXTENSION_NAME.getValue(),finalizerTask, task));
        createTaskRelationship(finalizerTask, task, buildId, CYPHER_CREATE_FINALIZES_RELATIONSHIP);
    }

    private Neo4jConnection createConnection(String server, String user, String password) throws SQLException {
        Properties connectionProps = new Properties();

        if(user != null && !user.isEmpty() && password != null && !password.isEmpty()) {
            connectionProps.put("user", user);
            connectionProps.put("password", password);
        }
        return new Driver().connect(String.format("jdbc:neo4j:%s", server), connectionProps);
    }

    public void commit() throws SQLException {
        logger.debug(String.format("%s :: Committing..." , Gteg2Neo4jConstants.EXTENSION_NAME.getValue()));
        connection.commit();
    }

    public void commitAndClose() {
        logger.info(String.format("%s :: Committing and closing connection", Gteg2Neo4jConstants.EXTENSION_NAME.getValue()));
        try {
            commit();
            connection.close();
        } catch (SQLException e) {
            logger.error(String.format("%s :: Exception while committing & closing connection! [%s]", Gteg2Neo4jConstants.EXTENSION_NAME.getValue(), e.getMessage()), e);
        }
    }

    public Neo4jConnection getConnection() {
        return connection;
    }

}
