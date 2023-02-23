# Configuration examples
On this page we will collect some configuration examples. This is meant to help you configure debug templates for your editor. The examples are provided in JSON format, but should be easily translatable to VSCode launch.json files, Emacs dap-mode templates, etc.


**NOTE: Some editors like Emacs using lsp-mode will require you to set a `noDebug` argument to `false`/`nil` to start a debug process, if not you will simply run the code.**


**NOTE 2: A general tip for working with the debug adapter is building your codebase before launching the debugger. A lot of people experiencing "class not found"-style issues, and they are in most cases caused by not building before debugging. A simple Maven or Gradle compile should suffice.**


## launch
### Regular main method
Replace the `mainClassName` with your fully qualified class name where your main method resides (e.g, `com.example.MyApplicationKt`), and `projectRootPath` with the root of your project. If your main method resides in a file, the class name will be the package and name of the file (+ Kt at the end). 


**Note:** If you use [the VSCode Kotlin extension](https://github.com/fwcd/vscode-kotlin) or [Emacs lsp-mode](https://emacs-lsp.github.io/lsp-mode/), you will have the option to use code lenses in your editor to run or debug main methods. These will fill out the details below automatically for you.


```json
{
    "type": "kotlin",
    "request": "launch",
    "mainClass": mainClassName,
    "projectRoot": projectRootPath
}
```


### Test
Debugging or running tests using the debug adapter might seem a bit daunting at first. What should the `mainClass` be? Fortunately it is quite simple if you use JUnit, as it provides a [Console Launcher](https://junit.org/junit5/docs/current/user-guide/#running-tests-console-launcher) that we can utilize.

```json
{
    "type": "kotlin",
    "request": "launch",
    "mainClass": "org.junit.platform.console.ConsoleLauncher --scan-class-path",
    "projectRoot": projectRootPath
}
```


For this to work, you will need the console launcher in your classpath during tests. Some JUnit related dependencies may already provide it, but you can also add it explicitly if you get ClassNotFound style errors (Maven example):
```xml
<dependency>
  <groupId>org.junit.platform</groupId>
  <artifactId>junit-platform-console-standalone</artifactId>
  <version>1.9.2</version>
  <scope>test</scope>
 </dependency>
```


There is [a neat plugin for Neovim utilizing this way of running tests already](https://github.com/Mgenuit/nvim-dap-kotlin).



## attach (connecting to an existing debug process)
`attach` configurations are meant for attaching to already running processes. These could be processes you start in the terminal, from your editor or something else. 


The setup will be the same whether you connect to a regular main method debugging session, or a test session.

```json
{
    "type": "kotlin",
    "request": "attach",
    "projectRoot": projectRootPath,
    "hostName": "localhost",
    "port": 5005,
    "timeout": 2000
}
```
(replace `projectRootPath` with the path to your project root)

If you connect to a process on an external machine, then replace `"localhost"` with your hostname. You can also tweak the port (5005 is the default for both Maven and Gradle).


## General: Logging to file
If you want to add Kotlin Debug Adapter logging (client and debug adapter communications) to file, you can also the following parameters to any of the examples above:
```json
{
    "enableJsonLogging": true,
    "jsonLogFile": pathToLogFile
}
```
(where `pathToLogFile` is replaced with the actual path to your log file)
