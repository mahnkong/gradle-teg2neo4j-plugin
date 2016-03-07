package com.github.mahnkong.gteg2neo4j

import org.gradle.api.Task
import org.gradle.api.tasks.TaskState
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.neo4j.harness.ServerControls
import org.neo4j.harness.TestServerBuilders

import java.sql.PreparedStatement
import java.sql.ResultSet

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertTrue

/**
 * Created by mahnkong on 05.03.2016.
 */
public class Neo4jClientTest {
    private static ServerControls neo4jControl;
    private static Neo4jClient neo4jClient;

    @ClassRule
    public static final TemporaryFolder testDir = new TemporaryFolder();

    @BeforeClass
    public static void startNeo4j() {
        neo4jControl = TestServerBuilders.newInProcessBuilder(testDir.getRoot()).newServer();
        neo4jClient = Neo4jClientHelper.getNeo4jClient(neo4jControl.httpURI().toString());
    }

    @Test
    public void addTask() {
        def name = 'testTask'
        def buildId = UUID.randomUUID().toString()
        def state = [ getExecuted : {true}, getDidWork : {true}, getSkipped : {true}, getUpToDate : {false}, getFailure: { null} ] as TaskState
        def task = [ getPath : { name }, getState : {state}] as Task

        neo4jClient.createTaskNode(task, buildId)

        PreparedStatement findTaskStmt = neo4jClient.connection.prepareStatement("MATCH (n:BuildTask{name:?,build:?}) RETURN n")
        findTaskStmt.setString(1, name)
        findTaskStmt.setString(2, buildId)
        ResultSet r = findTaskStmt.executeQuery();
        assertTrue(r.next())
        assertEquals(name, r.getObject(1).get('name'))
        assertEquals(buildId, r.getObject(1).get('build'))
    }

    @Test
    public void addFailureTask() {
        def name = 'testTask'
        def buildId = UUID.randomUUID().toString()
        def state = [ getExecuted : {true}, getDidWork : {true}, getSkipped : {false}, getUpToDate : {false}, getFailure: { new Throwable("This is a test") } ] as TaskState
        def task = [ getPath : { name }, getState : {state}] as Task

        neo4jClient.createTaskNode(task, buildId)

        PreparedStatement findTaskStmt = neo4jClient.connection.prepareStatement("MATCH (n:FailedTask{name:?,build:?}) RETURN n")
        findTaskStmt.setString(1, name)
        findTaskStmt.setString(2, buildId)
        ResultSet r = findTaskStmt.executeQuery();
        assertTrue(r.next())
        assertEquals(name, r.getObject(1).get('name'))
        assertEquals(buildId, r.getObject(1).get('build'))
    }

    @Test
    public void addSuccessfulTask() {
        def name = 'testTask'
        def buildId = UUID.randomUUID().toString()
        def state = [ getExecuted : {true}, getDidWork : {true}, getSkipped : {false}, getUpToDate : {false}, getFailure: { null } ] as TaskState
        def task = [ getPath : { name }, getState : {state}] as Task

        neo4jClient.createTaskNode(task, buildId)

        PreparedStatement findTaskStmt = neo4jClient.connection.prepareStatement("MATCH (n:SuccessfulTask{name:?,build:?}) RETURN n")
        findTaskStmt.setString(1, name)
        findTaskStmt.setString(2, buildId)
        ResultSet r = findTaskStmt.executeQuery();
        assertTrue(r.next())
        assertEquals(name, r.getObject(1).get('name'))
        assertEquals(buildId, r.getObject(1).get('build'))
    }

    @Test
    public void addDependsOnRelationship() {
        def taskName1 = 'testTask1'
        def taskName2 = 'testTask2'
        def buildId = UUID.randomUUID().toString()
        def state = [ getExecuted : {true}, getDidWork : {true}, getSkipped : {true}, getUpToDate : {false}, getFailure: { null} ] as TaskState
        def task1 = [ getPath : { taskName1 }, getState : {state}] as Task
        def task2 = [ getPath : { taskName2 }, getState : {state}] as Task

        neo4jClient.createTaskNode(task1, buildId)
        neo4jClient.createTaskNode(task2, buildId)
        //Task 2 depends on Task 1
        neo4jClient.createTaskDependsOnRelationship(task1, task2, buildId)

        PreparedStatement findDependsOnStmt = neo4jClient.connection.prepareStatement("MATCH (a:BuildTask{build: ?})-[:DEPENDS_ON]->(b:BuildTask{build: ?}) RETURN a, b")
        findDependsOnStmt.setString(1, buildId)
        findDependsOnStmt.setString(2, buildId)
        ResultSet r = findDependsOnStmt.executeQuery();
        assertTrue(r.next())
        assertEquals(taskName1, r.getObject(1).get('name'))
        assertEquals(taskName2, r.getObject(2).get('name'))
    }

    @Test
    public void addFinalizesRelationship() {
        def taskName1 = 'testTask1'
        def taskName2 = 'testTask2'
        def buildId = UUID.randomUUID().toString()
        def state = [ getExecuted : {true}, getDidWork : {true}, getSkipped : {true}, getUpToDate : {false}, getFailure: { null} ] as TaskState
        def task1 = [ getPath : { taskName1 }, getState : {state}] as Task
        def task2 = [ getPath : { taskName2 }, getState : {state}] as Task

        neo4jClient.createTaskNode(task1, buildId)
        neo4jClient.createTaskNode(task2, buildId)
        //Task 2 finalizes Task 1
        neo4jClient.createTaskFinalizesRelationship(task2, task1, buildId)

        PreparedStatement findFinalizesStmt = neo4jClient.connection.prepareStatement("MATCH (a:BuildTask{build: ?})-[:FINALIZES]->(b:BuildTask{build: ?}) RETURN a, b")
        findFinalizesStmt.setString(1, buildId)
        findFinalizesStmt.setString(2, buildId)
        ResultSet r = findFinalizesStmt.executeQuery();
        assertTrue(r.next())
        assertEquals(taskName2, r.getObject(1).get('name'))
        assertEquals(taskName1, r.getObject(2).get('name'))
    }

    @AfterClass
    public static void stopNeo4j() {
        neo4jControl.close();
    }
}
