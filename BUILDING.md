# Building
Describes how to build and run the debug adapter and the editor extensions.

## Setup
* Java 8+ should be installed and located under `JAVA_HOME` or `PATH`
* Note that you might need to use `gradlew` instead of `./gradlew` for the commands on Windows

## Debug Adapter

### Building
If you just want to build the debug adapter and use its binaries in your client of choice, run:

>`./gradlew :adapter:installDist`

The debug adapter executable is now located under `adapter/build/install/adapter/bin/kotlin-debug-adapter`. (Depending on your debug client, you might want to add it to your `PATH`)

Note that there are external dependent libraries, so if you want to put the server somewhere else, you have to move the entire `install`-directory.

## VSCode extension

### Development/Running
First run `npm run watch` from the `editors/vscode` directory in a background shell. The extension will then incrementally build in the background.

Every time you want to run the extension with the language server:
* Prepare the extension using `./gradlew :editors:vscode:prepare` (this automatically build and copies the language server's binaries into the extension folder)
* Open the debug tab in VSCode
* Run the `Extension` launch configuration

### Debugging
>TODO

### Packaging
Run `./gradlew :editors:vscode:packageExtension` from the repository's top-level-directory. The extension will then be located under the name `kotlindebug-[version].vsix` in `editors/vscode`.
