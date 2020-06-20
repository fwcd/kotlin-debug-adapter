# Editor Integration

## Visual Studio Code
See [vscode-kotlin](https://github.com/fwcd/vscode-kotlin) or install the extension from the [marketplace](https://marketplace.visualstudio.com/items?itemName=fwcd.kotlin).

## Other Editors
Install a [Debug Adapter Protocol client](https://microsoft.github.io/debug-adapter-protocol/implementors/tools/) for your tool. Then invoke the debug adapter executable in a client-specific way. The server uses `stdio` to send and receive `JSON` messages.

### Usage
* Start the debugger with a `launch` request after the initialization procedure:

![Sketch](https://microsoft.github.io/debug-adapter-protocol/img/init-launch.png)

* Please note:
	* The `projectRoot` and `mainClass` arguments must be specified
	* The `projectRoot` argument should contain the absolute path to a Maven or a Gradle project folder with
		* a buildfile (`pom.xml` or `build.gradle`)
		* compiled output classes (located in `build/classes/kotlin/main` or `target/classes/kotlin/main`)
