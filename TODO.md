# TODO

## High Priority
* Pipe stdio to the Debug Console

## Medium Priority
* Stop on exception
* Move the entire Java Debug Interface into the `jdi` package and add facade classes for breakpoints, stack frames, events, ... to decouple the backend from the debug adapter
* Split `KotlinDebugAdapter` into smaller handler classes

## Low Priority
* Add further capabilities using the Capabilities API in `KotlinDebugAdapter`

## Delayed
* Update `KotlinDebugAdapter` class once https://github.com/eclipse/lsp4j/issues/229 is resolved
* Update LSP4J dependency once 0.5.0 is deployed (07.09.2018)
    * See build.gradle
    * See https://projects.eclipse.org/projects/technology.lsp4j/releases/0.5.0
* Replace `adapterExecutableCommand` with new API once it is available
    * See configurationProvider.ts
