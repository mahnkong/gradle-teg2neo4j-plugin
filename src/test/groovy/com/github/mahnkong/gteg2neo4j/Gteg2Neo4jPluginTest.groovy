package com.github.mahnkong.gteg2neo4j

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.junit.*
import org.junit.rules.TemporaryFolder

import java.sql.PreparedStatement
import java.sql.ResultSet

import static org.gradle.util.GFileUtils.writeFile
import static org.junit.Assert.*

/**
 * Created by mahnkong on 28.02.2016.
 */
class Gteg2Neo4jPluginTest {

    @ClassRule
    public static final Neo4jTestServer neo4jTestServer = new Neo4jTestServer()

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();

    private static String serverUrl;
    private String pluginClassPath;

    @BeforeClass
    public static void setupClass() {
        serverUrl = neo4jTestServer.getNeo4jControl().httpURI().toString()
    }

    @Before
    public void setup() {
        def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.txt")
        if (pluginClasspathResource == null) {
            throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
        }

        pluginClassPath = pluginClasspathResource.readLines()
                .collect { it.replace('\\', '\\\\') } // escape backslashes in Windows paths
                .collect { "'$it'" }
                .join(", ")
    }

    @Test
    public void pluginAddsExtensionToProject() {
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply "com.github.mahnkong.${Gteg2Neo4jConstants.EXTENSION_NAME.value}"
        assertTrue(project.extensions.getByName(Gteg2Neo4jConstants.EXTENSION_NAME.value) instanceof Gteg2Neo4jExtension)
    }

    @Test
    public void usePluginWithoutConfig() {
        String buildFileContent = "buildscript {\n" +
                "    dependencies {\n" +
                "        classpath files($pluginClassPath)\n" +
                "    }\n" +
                "}\n" +
                "apply plugin: 'com.github.mahnkong.${Gteg2Neo4jConstants.EXTENSION_NAME.value}'\n" +
                "task task1 {\n" +
                "    doFirst {\n" +
                "        println 'Hello 1'\n" +
                "    }\n" +
                "}\n" +
                "task task2(dependsOn:task1) {\n" +
                "    doFirst {\n" +
                "        println 'Hello 2'\n" +
                "    }\n" +
                "}\n" +
                "task task3(dependsOn:task2) {\n" +
                "    doFirst {\n" +
                "        println 'Hello 3'\n" +
                "    }\n" +
                "}";
        writeFile(buildFileContent, new File("${testProjectDir.root.absolutePath}/build.gradle"))

        def buildResult = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("-m")
                .build()
        assertTrue(buildResult.output.contains("BUILD SUCCESS"))
        assertTrue(buildResult.output.contains(Gteg2Neo4jPlugin.CONFIG_INCOMPLETE_ERROR))
    }

    @Test
    public void pluginDisabledViaConfig() {
        String buildFileContent = "buildscript {\n" +
                "    dependencies {\n" +
                "        classpath files($pluginClassPath)\n" +
                "    }\n" +
                "}\n" +
                "apply plugin: 'com.github.mahnkong.${Gteg2Neo4jConstants.EXTENSION_NAME.value}'\n" +
                "${Gteg2Neo4jConstants.EXTENSION_NAME.value} { \n" +
                "    neo4jServer '${serverUrl}'\n" +
                "    disabled true\n" +
                "} \n" +
                "task task1 {\n" +
                "    doFirst {\n" +
                "        println 'Hello 1'\n" +
                "    }\n" +
                "}";
        writeFile(buildFileContent, new File("${testProjectDir.root.absolutePath}/build.gradle"))

        def buildResult = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("task1")
                .build()
        assertTrue(buildResult.output.contains("BUILD SUCCESS"))
        assertTrue(!buildResult.output.contains(Gteg2Neo4jPlugin.BUILD_ID_OUTPUT_PREFIX))
    }

    @Test
    public void pluginDisabledViaSystemProperty() {
        String buildFileContent = "buildscript {\n" +
                "    dependencies {\n" +
                "        classpath files($pluginClassPath)\n" +
                "    }\n" +
                "}\n" +
                "apply plugin: 'com.github.mahnkong.${Gteg2Neo4jConstants.EXTENSION_NAME.value}'\n" +
                "${Gteg2Neo4jConstants.EXTENSION_NAME.value} { \n" +
                "    neo4jServer '${serverUrl}'\n" +
                "    disabled true\n" +
                "} \n" +
                "task task1 {\n" +
                "    doFirst {\n" +
                "        println 'Hello 1'\n" +
                "    }\n" +
                "}";
        writeFile(buildFileContent, new File("${testProjectDir.root.absolutePath}/build.gradle"))

        def buildResult = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("-D${Gteg2Neo4jConstants.DISABLE_GTEG2NEO2J_PROPERTY.value} task1")
                .build()
        assertTrue(buildResult.output.contains("BUILD SUCCESS"))
        assertTrue(!buildResult.output.contains(Gteg2Neo4jPlugin.BUILD_ID_OUTPUT_PREFIX))
    }

    @Test
    public void usePluginWithConfig() {
        String buildFileContent = "buildscript {\n" +
                "    dependencies {\n" +
                "        classpath files($pluginClassPath)\n" +
                "    }\n" +
                "}\n" +
                "apply plugin: 'com.github.mahnkong.${Gteg2Neo4jConstants.EXTENSION_NAME.value}'\n" +
                "${Gteg2Neo4jConstants.EXTENSION_NAME.value} { \n" +
                "    neo4jServer '${serverUrl}'\n" +
                "} \n" +
                "task task1 {\n" +
                "    doFirst {\n" +
                "        println 'Hello 1'\n" +
                "    }\n" +
                "}\n" +
                "task task2(dependsOn:task1) {\n" +
                "    doFirst {\n" +
                "        println 'Hello 2'\n" +
                "    }\n" +
                "}\n" +
                "task task3(dependsOn:task2) {\n" +
                "    doFirst {\n" +
                "        println 'Hello 3'\n" +
                "    }\n" +
                "}\n" +
                "task task4() {\n" +
                "    doFirst {\n" +
                "        println 'Hello 4'\n" +
                "    }\n" +
                "}\n" +
                "task3.finalizedBy('task4')";
        writeFile(buildFileContent, new File("${testProjectDir.root.absolutePath}/build.gradle"))

        def buildResult = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("task3")
                .build()
        assertTrue(buildResult.output.contains("BUILD SUCCESS"))

        //Get build ID from output
        def matcher = (buildResult.output =~ /(?ms).*${Gteg2Neo4jPlugin.BUILD_ID_OUTPUT_PREFIX}\s+(\S+)\r?\n/)
        assertTrue(matcher.matches())
        def buildId = matcher.getAt(0).getAt(1)
        assertNotNull(buildId)

        //Check that tasks and relationships are correct in db
        def neo4jClient = Neo4jClientHelper.getNeo4jClient(serverUrl);
        PreparedStatement findTaskStmt = neo4jClient.connection.prepareStatement("MATCH (n:BuildTask{build:?}) RETURN count(n)")
        findTaskStmt.setString(1, buildId)
        ResultSet r = findTaskStmt.executeQuery();
        assertTrue(r.next())
        assertEquals(4, r.getInt(1))
        findTaskStmt.close()

        PreparedStatement findFinalizesStmt = neo4jClient.connection.prepareStatement("MATCH (a:BuildTask{build: ?})-[:FINALIZES]->(b:BuildTask{name: ?}) RETURN a")
        findFinalizesStmt.setString(1, buildId)
        findFinalizesStmt.setString(2, ':task3')
        r = findFinalizesStmt.executeQuery();
        assertTrue(r.next())
        assertEquals(':task4', r.getObject(1).get('name'))
        findTaskStmt.close()

        PreparedStatement findDependsOnStmt = neo4jClient.connection.prepareStatement("MATCH (a:BuildTask{build: ?})-[:DEPENDS_ON]->(b:BuildTask{name: ?}) RETURN a")
        findDependsOnStmt.setString(1, buildId)
        findDependsOnStmt.setString(2, ':task2')
        r = findDependsOnStmt.executeQuery();
        assertTrue(r.next())
        assertEquals(':task3', r.getObject(1).get('name'))
        findTaskStmt.close()
    }

    @Test
    public void pluginFailuresNeo4jClient() {
        String buildFileContent = "buildscript {\n" +
                "    dependencies {\n" +
                "        classpath files($pluginClassPath)\n" +
                "    }\n" +
                "}\n" +
                "apply plugin: 'com.github.mahnkong.${Gteg2Neo4jConstants.EXTENSION_NAME.value}'\n" +
                "${Gteg2Neo4jConstants.EXTENSION_NAME.value} { \n" +
                "    neo4jServer 'httb://localhost:7474'\n" +
                "} \n" +
                "task task1 {\n" +
                "    doFirst {\n" +
                "        println 'Hello 1'\n" +
                "    }\n" +
                "}";
        writeFile(buildFileContent, new File("${testProjectDir.root.absolutePath}/build.gradle"))

        def buildResult = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("task1")
                .build()
        assertTrue(buildResult.output.contains("BUILD SUCCESS"))
        System.err.println(buildResult.output)
        assertTrue(buildResult.output.contains(Gteg2Neo4jPlugin.EXCEPTION_OCCURED_ERROR))
    }
}
