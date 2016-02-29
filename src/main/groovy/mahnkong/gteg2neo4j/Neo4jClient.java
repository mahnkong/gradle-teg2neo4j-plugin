package mahnkong.gteg2neo4j;

import org.gradle.api.Task;
import org.gradle.api.logging.Logger;
import org.neo4j.jdbc.Driver;
import org.neo4j.jdbc.Neo4jConnection;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Created by mahnkong on 28.02.2016.
 */
public class Neo4jClient {

    private static final String CYPHER_CREATE_NOTE = "MERGE (n:buildtask {name: ?, build: ?}) RETURN n";
    private static final String CYPHER_CREATE_RELATIONSHIP = "MATCH (a:buildtask), (b:buildtask) WHERE a.name = ? AND a.build = ? AND b.name = ? AND b.build = ? CREATE (a)-[r:dependsOn]->(b) RETURN r";

    Neo4jConnection connection;
    Logger logger;

    public Neo4jClient(String server, String user, String password, Logger logger) throws SQLException {
        this.connection = createConnection(server, user, password);
        this.logger = logger;
    }

    void createTaskNode(Task task, String buildId) throws SQLException {
        logger.info(String.format("%s :: Adding task '%s' to db", Gteg2Neo4jConstants.EXTENSION_NAME.getValue(), task.getPath()));
        try (PreparedStatement createTaskStmt = connection.prepareStatement(CYPHER_CREATE_NOTE)) {
            createTaskStmt.setString(1, task.getPath());
            createTaskStmt.setString(2, buildId);
            createTaskStmt.execute();
        }
    }

    void createTaskRelationship(Task task, Task dependsOnTask, String buildId) throws SQLException {
        logger.info(String.format("%s :: Adding relationship between tasks '%s' and '%s'", Gteg2Neo4jConstants.EXTENSION_NAME.getValue(),task, dependsOnTask));
        try (PreparedStatement createRelationshipStmt = connection.prepareStatement(CYPHER_CREATE_RELATIONSHIP)) {
            createRelationshipStmt.setString(1, task.getPath());
            createRelationshipStmt.setString(2, buildId);
            createRelationshipStmt.setString(3, dependsOnTask.getPath());
            createRelationshipStmt.setString(4, buildId);
            createRelationshipStmt.execute();
        }
    }

    private Neo4jConnection createConnection(String server, String user, String password) throws SQLException {
        Properties connectionProps = new Properties();
        connectionProps.put("user", user);
        connectionProps.put("password", password);
        return new Driver().connect(String.format("jdbc:neo4j://%s", server), connectionProps);
    }

    public void close() {
        logger.info(String.format("%s :: Closing connection", Gteg2Neo4jConstants.EXTENSION_NAME.getValue()));
        try {
            connection.close();
        } catch (SQLException e) {
            logger.error(String.format("%s :: Exception while closing connection! [%s]", Gteg2Neo4jConstants.EXTENSION_NAME.getValue(), e.getMessage()), e);
        }
    }
}