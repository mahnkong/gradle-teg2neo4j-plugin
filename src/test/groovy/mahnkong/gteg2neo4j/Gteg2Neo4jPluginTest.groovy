package mahnkong.gteg2neo4j

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.gradle.util.GFileUtils.writeFile
import static org.junit.Assert.assertTrue

/**
 * Created by mahnkong on 28.02.2016.
 */
class Gteg2Neo4jPluginTest {

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder();

    private String pluginClassPath;

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
        project.pluginManager.apply "mahnkong.${Gteg2Neo4JConstants.EXTENSION_NAME.value}"
        assertTrue(project.extensions.getByName(Gteg2Neo4JConstants.EXTENSION_NAME.value) instanceof Gteg2Neo4jExtension)
    }

    @Test
    public void usePluginWithoutConfig() {
        String buildFileContent = "buildscript {\n" +
                "            dependencies {\n" +
                "                classpath files($pluginClassPath)\n" +
                "            }\n" +
                "        }\n" +
                "apply plugin: 'mahnkong.${Gteg2Neo4JConstants.EXTENSION_NAME.value}'\n" +
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

    @Test @Ignore("fixme")
    public void usePluginWithConfig() {
        String buildFileContent = "buildscript {\n" +
                "            dependencies {\n" +
                "                classpath files($pluginClassPath)\n" +
                "            }\n" +
                "        }\n" +
                "apply plugin: 'mahnkong.${Gteg2Neo4JConstants.EXTENSION_NAME.value}'\n" +
                "${Gteg2Neo4JConstants.EXTENSION_NAME.value} { \n" +
                "    neo4jUser 'neo4j'\n" +
                "    neo4jPassword 'neo4j'\n" +
                "    neo4jServer 'file:${testProjectDir.root.absolutePath.replaceAll("\\\\", "/")}'\n" +
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
                "}";
        writeFile(buildFileContent, new File("${testProjectDir.root.absolutePath}/build.gradle"))

        def buildResult = GradleRunner.create()
                .withProjectDir(testProjectDir.getRoot())
                .withArguments("-m")
                .build()
        assertTrue(buildResult.output.contains("BUILD SUCCESS"))
    }
}
