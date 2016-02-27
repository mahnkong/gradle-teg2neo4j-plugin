package org.mahnkong.gteg2neo4j

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.execution.TaskExecutionGraph

/**
 * Created by mahnkong on 27.02.2016.
 */
class Gteg2Neo4jPlugin implements Plugin<Project> {

    final EXTENSION_NAME = "gteg2neo4j"

    def Map getTaskMapFromTaskGraph(TaskExecutionGraph graph) {
        def taskMap = [:]
        graph.allTasks.each {
            def deps = []
            it.taskDependencies.getDependencies(it).each { deps << it.path }
            taskMap.put(it.path, deps)
        }
        return taskMap
    }

    def validateParams(extension, logger) {
        if (extension.disabled) {
            logger.info("'${EXTENSION_NAME}' has been disabled - won't send data to neo4j!")
            return false
        } else if (!extension.neo4jServer || !extension.neo4jUser || !extension.neo4jPassword) {
            logger.error("neo4jServer, neo4jUser and neo4jPassword must be defined for '${EXTENSION_NAME}' plugin!")
            return false
        }
        return true
    }

    void apply(Project project) {
        project.extensions.create(EXTENSION_NAME, Gteg2Neo4jExtension)
        if (project.gradle.getStartParameter().dryRun) {
            def taskMap = [:]
            project.gradle.getTaskGraph().whenReady {
                taskMap = getTaskMapFromTaskGraph(it)
            }

            project.gradle.buildFinished {
                def extension = project.extensions.getByName(EXTENSION_NAME)
                if (! validateParams(extension, project.logger))
                    return

                def neo4jClient = new Neo4jClient(extension.neo4jServer, extension.neo4jUser, extension.neo4jPassword, project.logger)
                def buildId = "${project.name}::${UUID.randomUUID().toString()}";
                println "This build has the id: ${buildId}"

                taskMap.each { task, dependencyTasks ->
                    neo4jClient.createTaskNode(task, buildId)
                    dependencyTasks.each { dependency ->
                        neo4jClient.createTaskNode(dependency, buildId)
                        neo4jClient.createTaskRelationship(task, dependency, buildId)
                    }
                }
                neo4jClient.close()
            }
        }
    }
}
