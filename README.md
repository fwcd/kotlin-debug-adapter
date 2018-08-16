# KotlinDebugAdapter
A [debug adapter](https://microsoft.github.io/debug-adapter-protocol/) for Kotlin that provides IDE-independent JVM debugging support and a VSCode extension that uses the debug adapter.

## [WIP] Many features are still missing from the current version of the debug adapter

![Icon](Icon128.png)

## Getting Started
* See [BUILDING.md](BUILDING.md) for build instructions
* See [KotlinQuickStart](https://github.com/fwcd/KotlinQuickStart) for a sample project

## Usage
* Invoke the debug adapter with a `launch` request after the initialization procedure [as sketched here](https://microsoft.github.io/debug-adapter-protocol/img/init-launch.png)
    * The `projectRoot` and `mainClass` arguments must be specified
    * The `projectRoot` argument should contain the absolute path to a Maven or a Gradle project folder with
		* a buildfile (`pom.xml` or `build.gradle`)
		* compiled output classes (located in `build/classes/kotlin/main` or `target/classes/kotlin/main`)

## See also
* [KotlinLanguageServer](https://github.com/fwcd/KotlinLanguageServer) for smart code completion, diagnostics and more
