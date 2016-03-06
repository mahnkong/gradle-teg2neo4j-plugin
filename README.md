# gradle-teg2neo4j-plugin
![Travis build status](https://api.travis-ci.org/mahnkong/gradle-teg2neo4j-plugin.svg?branch=develop)

"gradle-teg2neo4j-plugin" (or in short: "gteg2neo4j") is a Gradle plugin providing functionality to store the [Task Execution Graph](https://docs.gradle.org/2.11/javadoc/org/gradle/api/execution/TaskExecutionGraph.html "TaskExecutionGraph interface") (including information like success, failure, etc. for each task of the current build) of a project's gradle build inside a [Neo4j](http://neo4j.com/ "Neo4j Home") instance.

Once imported, the graph can be seen directly inside the Neo4j browser and detailed data can be queried using the Neo4j [Cypher](http://neo4j.com/developer/cypher-query-language/ "Cypher documentation") language.

The following image shows such a visualizion. The source was a build execution of the "gradle-teg2neo4j-plugin" project itself (which was actually failing). 

![Task Graph Visualization](https://drive.google.com/uc?export=download&id=0B2Bgx0RONdwIYU9RY04tSS1yWlE)

## Usage

### Configuration of the plugin
The build.gradle file of the project, which will use the plugin, must be extended with the configuration below:

```gradle
//fetch plugin jar and dependencies
buildscript {
    repositories {
        //only relevant if the plugin is located in the local maven repo
        mavenLocal()
        jcenter()
        //required for the neo4j relevant dependencies
        maven { url "http://m2.neo4j.org/content/groups/public" }
    }
    dependencies {
        classpath 'mahnkong:gradle-teg2neo4j-plugin:$VERSION'
    }
}

//apply the plugin
apply plugin: 'mahnkong.gteg2neo4j'

//configuration of the plugin
gteg2neo4j {
    //required: server url
    neo4jServer "http://localhost:7474"
    //optional: authentication parameters
    neo4jUser "neo4j"
    neo4jPassword "password"
    //optional: disable plugin
    disabled false
}
```

### Execution of a build with the plugin configured

Just execute the build as always. At the end of the build the plugin prints the build id used for the storing of the data in the neo4j instance

```
./gradlew build
:compileJava UP-TO-DATE
:compileGroovy UP-TO-DATE
:processResources UP-TO-DATE
:classes UP-TO-DATE
:jar UP-TO-DATE
:assemble UP-TO-DATE
:createClasspathManifest UP-TO-DATE
:compileTestJava UP-TO-DATE
:compileTestGroovy UP-TO-DATE
:processTestResources UP-TO-DATE
:testClasses UP-TO-DATE
:test UP-TO-DATE
:check UP-TO-DATE
:build UP-TO-DATE

BUILD SUCCESSFUL

Total time: 5.091 secs
gteg2neo4j :: This build has the id: gradle-teg2neo4j-plugin::71592037-771a-4391-b9db-825df301d6a4
```

The plugin execution can be disabled during a build by providing the system property "gteg2neo4j.disabled"

```
./gradlew build -Dgteg2neo4j.disabled
 ...
BUILD SUCCESSFUL

Total time: 3.488 secs
'gteg2neo4j' has been disabled - won't send data to neo4j!
```

### Query the database using the build id

With the build id, the stored task graph and its task data can be accessed using Neo4j.

Example query for the complete task graph of one specific build:

```
MATCH (t:BuildTask {build:'gradle-teg2neo4j-plugin::71592037-771a-4391-b9db-825df301d6a4'}) RETURN t
```

Example query for all tasks stored during the last 10 minutes:

```
MATCH (t:BuildTask) WHERE t.insertedAt + 600000 >= timestamp() RETURN t
```

## Node and Relationship reference

The following labels exist for the task nodes:

- BuildTask: All task nodes 
- SucessfulTask: All successful task nodes 
- FailureTask: All successful task nodes 

The following attributes are set for each task node (based on the task's state after the build finished):

- name: The task's path
- build: the build id (set by the "gradle-teg2neo4j-plugin")
- executed: bool indicating that the task was executed
- didWork: bool indicating that the task did some work
- upToDate: bool indicating that the task was up to date
- failureMsg: the thrown exception's message in case the task failed
- insertedAt: timestamp of the data insertion (set by the "gradle-teg2neo4j-plugin")

The following relationships are set between the task nodes depending on the task execution graph

- DEPENDS_ON: indicates, that one task depends on another task
- FINALIZES: indicates, that one task finalizes another task
 
## How to build the plugin

To build, simply execute the build task from within the project's directory:

```
./gradlew build
```

To publish the build to the local maven repository, execute:

```
./gradlew publishAPTMLR
```
