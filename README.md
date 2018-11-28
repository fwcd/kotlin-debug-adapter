# KotlinDebugAdapter
A [debug adapter](https://microsoft.github.io/debug-adapter-protocol/) that provides IDE-independent debugging support for Kotlin/JVM and a VSCode extension that uses the debug adapter.

[![Version](https://vsmarketplacebadge.apphb.com/version-short/fwcd.kotlindebug.svg)](https://marketplace.visualstudio.com/items?itemName=fwcd.kotlindebug)
[![Installs](https://vsmarketplacebadge.apphb.com/installs-short/fwcd.kotlindebug.svg)](https://marketplace.visualstudio.com/items?itemName=fwcd.kotlindebug)
[![Build Status](https://travis-ci.org/fwcd/KotlinDebugAdapter.svg?branch=master)](https://travis-ci.org/fwcd/KotlinDebugAdapter)

![Icon](Icon128.png)

## Getting Started
* See [BUILDING.md](BUILDING.md) for build instructions
* See [KotlinQuickStart](https://github.com/fwcd/KotlinQuickStart) for a sample project
* See [KotlinLanguageServer](https://github.com/fwcd/KotlinLanguageServer) for smart code completion, diagnostics and more

## Usage

### with VSCode
* Setup:
    * Open the `launch.json` file in your project and invoke code completion to create a new launch configuration (or select `Add Configuration...` in the debug tab)
* Launch:
    * `./gradlew build` your project (before every launch)
	* Click the `Run` button in the `Debug` tab or press `F5`

### with any editor (JSON-RPC)
* Setup:
    * Install a [Debug Adapter Protocol client](https://microsoft.github.io/debug-adapter-protocol/implementors/tools/) for your tool
* Launch:
    * `./gradlew build` your project (before every launch)
    * Invoke the debug adapter through JSON-RPC with a `launch` request after the initialization procedure [as sketched here](https://microsoft.github.io/debug-adapter-protocol/img/init-launch.png)
        * The `projectRoot` and `mainClass` arguments must be specified
        * The `projectRoot` argument should contain the absolute path to a Maven or a Gradle project folder with
		    * a buildfile (`pom.xml` or `build.gradle`)
		    * compiled output classes (located in `build/classes/kotlin/main` or `target/classes/kotlin/main`)

## Architecture
`DAP client` <= JSON => `KotlinDebugAdapter` <=> `Core abstractions` <=> `Java Debug Interface`
