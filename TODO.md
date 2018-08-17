# TODO

* Wait until https://github.com/eclipse/lsp4j/issues/234 is resolved
* Update KotlinDebugAdapter class once https://github.com/eclipse/lsp4j/issues/229 is resolved
* Update LSP4J dependency once 0.5.0 is deployed (07.09.2018)
    * See build.gradle
    * See https://projects.eclipse.org/projects/technology.lsp4j/releases/0.5.0
* Handle launch requests in KotlinDebugAdapter by using the JAR path and the main class as specified in the clients debug configuration
* Handle stepping, stack traces, breakpoints and more
* Add further capabilities using the Capabilities API in KotlinDebugAdapter
* Replace 'adapterExecutableCommand' with new API once it is available
    * See configurationProvider.ts
