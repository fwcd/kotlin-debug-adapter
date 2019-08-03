# KotlinDebugAdapter
A [debug adapter](https://microsoft.github.io/debug-adapter-protocol/) that provides IDE-independent debugging support for Kotlin/JVM.

[![Version](https://vsmarketplacebadge.apphb.com/version-short/fwcd.kotlindebug.svg)](https://marketplace.visualstudio.com/items?itemName=fwcd.kotlindebug)
[![Installs](https://vsmarketplacebadge.apphb.com/installs-short/fwcd.kotlindebug.svg)](https://marketplace.visualstudio.com/items?itemName=fwcd.kotlindebug)
[![Build Status](https://travis-ci.org/fwcd/KotlinDebugAdapter.svg?branch=master)](https://travis-ci.org/fwcd/KotlinDebugAdapter)

![Icon](Icon128.png)

Any editor conforming to DAP is supported, including [VSCode](https://code.visualstudio.com) for which a client extension is provided by this repository.

## Getting Started
* See [BUILDING.md](BUILDING.md) for build instructions
* See [Editor Integration](editors/README.md) for editor-specific instructions
* See [KotlinQuickStart](https://github.com/fwcd/KotlinQuickStart) for a sample project
* See [KotlinLanguageServer](https://github.com/fwcd/KotlinLanguageServer) for smart code completion, diagnostics and more

## Usage

### with VSCode
* Setup:
    * Open the `launch.json` file in your project and invoke code completion to create a new launch configuration (or select `Add Configuration...` in the debug tab)
* Launch:
    * `./gradlew build` your project (before every launch)
	* Click the `Run` button in the `Debug` tab or press `F5`

## Architecture
`DAP client` <= JSON => `KotlinDebugAdapter` <=> `Core abstractions` <=> `Java Debug Interface`
