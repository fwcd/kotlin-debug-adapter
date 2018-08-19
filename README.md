# KotlinDebugAdapter
A [debug adapter](https://microsoft.github.io/debug-adapter-protocol/) that provides IDE-independent debugging support for Kotlin/JVM and a VSCode extension that uses the debug adapter.

![Icon](Icon128.png)

## Getting Started
* See [BUILDING.md](BUILDING.md) for build instructions
* See [KotlinQuickStart](https://github.com/fwcd/KotlinQuickStart) for a sample project

## Usage
* `./gradlew build` your project
* Invoke the debug adapter with a `launch` request after the initialization procedure [as sketched here](https://microsoft.github.io/debug-adapter-protocol/img/init-launch.png)
    * The `projectRoot` and `mainClass` arguments must be specified
    * The `projectRoot` argument should contain the absolute path to a Maven or a Gradle project folder with
		* a buildfile (`pom.xml` or `build.gradle`)
		* compiled output classes (located in `build/classes/kotlin/main` or `target/classes/kotlin/main`)

## See also
* [KotlinLanguageServer](https://github.com/fwcd/KotlinLanguageServer) for smart code completion, diagnostics and more
