# Building
Contains the commands required to build this project. Note that you might need to use `gradlew` instead of `./gradlew` when running on `cmd.exe`.

## Setting up the development environment
* Java should be installed

### For debug adapter development
* `./gradlew build`

### For extension development
* VSCode is required
* `npm install`
* `npm install -g vsce`

## Building the Debug Adapter
* `./gradlew installDist`
* Start scripts for the debug adapter are located under `build/install/KotlinDebugAdapter/bin/`

## Testing the Debug Adapter
* `./gradlew test`

## Packaging the VSCode extension
* `vsce package -o build.vsix`
* The extension is located as `build.vsix` in the repository folder
