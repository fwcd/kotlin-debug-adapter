# Kotlin Debug Adapter

[![Release](https://img.shields.io/github/release/fwcd/kotlin-debug-adapter)](https://github.com/fwcd/kotlin-debug-adapter/releases)
[![Build](https://github.com/fwcd/kotlin-debug-adapter/actions/workflows/build.yml/badge.svg)](https://github.com/fwcd/kotlin-debug-adapter/actions/workflows/build.yml)
[![Downloads](https://img.shields.io/github/downloads/fwcd/kotlin-debug-adapter/total)](https://github.com/fwcd/kotlin-debug-adapter/releases)
[![Chat](https://img.shields.io/badge/chat-on%20discord-7289da)](https://discord.gg/cNtppzN)

A [debug adapter](https://microsoft.github.io/debug-adapter-protocol/) that provides IDE-independent debugging support for Kotlin/JVM.

![Icon](Icon128.png)

Any editor conforming to DAP is supported, including [VSCode](https://github.com/fwcd/vscode-kotlin).

## Getting Started
* See [BUILDING.md](BUILDING.md) for build instructions
* See [Editor Integration](EDITORS.md) for editor-specific instructions
* See [CONFIGURATION.md](CONFIGURATION.md) for examples on debug configurations (mostly editor agnostic).
* See [Kotlin Quick Start](https://github.com/fwcd/kotlin-quick-start) for a sample project
* See [Kotlin Language Server](https://github.com/fwcd/kotlin-language-server) for smart code completion, diagnostics and more

## Architecture
`DAP client` <= JSON => `KotlinDebugAdapter` <=> `Core abstractions` <=> `Java Debug Interface`
