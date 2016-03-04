package mahnkong.gteg2neo4j

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph

/**
 * Created by mahnkong on 27.02.2016.
 */
class Gteg2Neo4jPlugin implements Plugin<Project> {

    final static CONFIG_INCOMPLETE_ERROR = "neo4jServer, neo4jUser and neo4jPassword must be defined for '${Gteg2Neo4jConstants.EXTENSION_NAME.value}' plugin!"
    final static BUILD_ID_OUTPUT_PREFIX = "${Gteg2Neo4jConstants.EXTENSION_NAME.value} :: This build has the id:"

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
        if (extension.disabled) {
            logger.info("'${Gteg2Neo4jConstants.EXTENSION_NAME.value}' has been disabled - won't send data to neo4j!")
            return false
        } else if (!extension.neo4jServer || !extension.neo4jUser || !extension.neo4jPassword) {
            logger.error(CONFIG_INCOMPLETE_ERROR)
            return false
        }
        return true
    }

    void apply(Project project) {
        project.extensions.create(Gteg2Neo4jConstants.EXTENSION_NAME.value, Gteg2Neo4jExtension)
        //if (project.gradle.getStartParameter().dryRun) {
            def Map<Task, Map<TaskRelationships, Set<Task>>> taskMap = [:]
            project.gradle.getTaskGraph().whenReady {
                taskMap = getTaskMapFromTaskGraph(it)
            }

            project.gradle.buildFinished {
                def extension = project.extensions.getByName(Gteg2Neo4jConstants.EXTENSION_NAME.value)
                if (! validateParams(extension, project.logger))
                    return

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
                            } else {
                                neo4jClient.createTaskFinalizedByRelationship(task, it, buildId)
                            }
                        }
                    }
                }
                neo4jClient.commitAndClose()
            }
        //}
    }
}
