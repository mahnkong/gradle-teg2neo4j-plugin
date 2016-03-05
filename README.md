![Travis build status](https://api.travis-ci.org/mahnkong/gradle-teg2neo4j-plugin.svg?branch=develop)

# gradle-teg2neo4j-plugin

"gradle-teg2neo4j-plugin" (or in short: "gteg2neo4j") is a Gradle plugin providing functionality to store the "Gradle Task Execution Graph" (including information like success, failure, etc. for each task of the current build) of a project's build inside a Neo4j instance.

Once imported, the graph can be seen directly inside the Neo4j browser and detailed data can be queried using the Neo4j Cypher language.

The following image shows such a visualizion. The source was a build execution of the "gradle-teg2neo4j-plugin" project itself (which was actually failing). 

![Task Graph Visualization](https://drive.google.com/uc?export=download&id=0B2Bgx0RONdwIYU9RY04tSS1yWlE)

## How to use

## How to build
