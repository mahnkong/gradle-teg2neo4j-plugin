package mahnkong.gteg2neo4j

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph

import java.time.Instant

/**
 * Created by mahnkong on 27.02.2016.
 */
class Gteg2Neo4jPlugin implements Plugin<Project> {

    final static CONFIG_INCOMPLETE_ERROR = "neo4jServer must be defined for '${Gteg2Neo4jConstants.EXTENSION_NAME.value}' plugin!"
    final static BUILD_ID_OUTPUT_PREFIX = "${Gteg2Neo4jConstants.EXTENSION_NAME.value} :: This build has the id:"
    final static EXCEPTION_OCCURED_ERROR = "${Gteg2Neo4jConstants.EXTENSION_NAME.value} :: An exception occured while trying to store the Gradle Task execution data!"

    def Map getTaskMapFromTaskGraph(TaskExecutionGraph graph) {
        def Map<Task, Map<TaskRelationships, Set<Task>>> taskMap = [:]
        graph.allTasks.each {
            def relationsShips = [:]
            relationsShips.put(TaskRelationships.DEPENDS_ON, it.taskDependencies.getDependencies(it))
            relationsShips.put(TaskRelationships.FINALIZED_BY, it.finalizedBy.getDependencies(it))
            taskMap.put(it, relationsShips)
        }
        return taskMap
    }

    def validateParams(extension, logger) {
        if (extension.disabled || System.properties.containsKey(Gteg2Neo4jConstants.DISABLE_GTEG2NEO2J_PROPERTY.value)) {
            logger.info("'${Gteg2Neo4jConstants.EXTENSION_NAME.value}' has been disabled - won't send data to neo4j!")
            return false
        } else if (!extension.neo4jServer) {
            logger.error(CONFIG_INCOMPLETE_ERROR)
            return false
        }
        return true
    }

    void apply(Project project) {
        project.extensions.create(Gteg2Neo4jConstants.EXTENSION_NAME.value, Gteg2Neo4jExtension)
        def Map<Task, Map<TaskRelationships, Set<Task>>> taskMap = [:]
        project.gradle.getTaskGraph().whenReady {
            taskMap = getTaskMapFromTaskGraph(it)
        }

        project.gradle.buildFinished {
            def extension = project.extensions.getByName(Gteg2Neo4jConstants.EXTENSION_NAME.value)
            if (!validateParams(extension, project.logger)) {
                return
            }

            try {
                def neo4jClient = new Neo4jClient(extension.neo4jServer, extension.neo4jUser, extension.neo4jPassword, project.logger)
                String buildId = new String("${project.name}::${UUID.randomUUID().toString()}")
                println "${BUILD_ID_OUTPUT_PREFIX} ${buildId}"

                taskMap.each { task, relationships ->
                    neo4jClient.createTaskNode(task, buildId)
                    relationships.each { relationshipType, tasks ->
                        tasks.each {
                            neo4jClient.createTaskNode(it, buildId)
                            if (relationshipType.equals(TaskRelationships.DEPENDS_ON)) {
                                neo4jClient.createTaskDependsOnRelationship(task, it, buildId)
                            }
                            if (relationshipType.equals(TaskRelationships.FINALIZED_BY)) {
                                neo4jClient.createTaskFinalizesRelationship(it, task, buildId)
                            }
                        }
                    }
                }
                neo4jClient.commitAndClose()
            } catch (Exception e) {
                project.logger.error("${EXCEPTION_OCCURED_ERROR} [${e.getMessage()}", e)
            }
        }
    }
}
