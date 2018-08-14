import * as vscode from "vscode";
import * as path from "path";
import * as fs from "fs";
import * as child_process from "child_process";
import { LOG } from "./logger";
import { isOSUnixoid } from "./osUtils";

// This method is called when your extension is activated
export function activate(context: vscode.ExtensionContext): void {
	LOG.info("Activating Kotlin debug adapter...");
	
	let javaExecutablePath = findJavaExecutable("java");
	if ((javaExecutablePath === null) || (javaExecutablePath === undefined)) {
		vscode.window.showErrorMessage("Couldn't locate java in $JAVA_HOME or $PATH");
		return;
	}
	
	let startScriptPath = path.resolve(context.extensionPath, "build", "install", "KotlinDebugAdapter", "bin", correctScriptName("KotlinDebugAdapter"));
	let args = [];
	
	// Ensure that start script can be executed
	if (isOSUnixoid()) {
		child_process.exec("chmod +x " + startScriptPath);
	}
	
	LOG.info("Launching {} with args {}", startScriptPath, args.join(" "));
	
	context.subscriptions.push(vscode.commands.registerCommand("extension.kotlindebug.adapterexecutablepath", () => startScriptPath));
}

// This method is called when your extension is deactivated
export function deactivate() {}

function findJavaExecutable(rawBinname: string) {
	let binname = correctBinname(rawBinname);

	// First search java.home setting
    let userJavaHome = vscode.workspace.getConfiguration('java').get('home') as string;

	if (userJavaHome != null) {
        LOG.debug("Looking for Java in java.home (settings): {}", userJavaHome);

        let candidate = findJavaExecutableInJavaHome(userJavaHome, binname);

        if (candidate != null)
            return candidate;
	}

	// Then search each JAVA_HOME
    let envJavaHome = process.env['JAVA_HOME'];

	if (envJavaHome) {
        LOG.debug("Looking for Java in JAVA_HOME (environment variable): {}", envJavaHome);

        let candidate = findJavaExecutableInJavaHome(envJavaHome, binname);

        if (candidate != null)
            return candidate;
	}

	// Then search PATH parts
	if (process.env['PATH']) {
        LOG.debug("Looking for Java in PATH");

		let pathparts = process.env['PATH'].split(path.delimiter);
		for (let i = 0; i < pathparts.length; i++) {
			let binpath = path.join(pathparts[i], binname);
			if (fs.existsSync(binpath)) {
				return binpath;
			}
		}
	}

    // Else return the binary name directly (this will likely always fail downstream)
    LOG.debug("Could not find Java, will try using binary name directly");
	return binname;
}

function correctBinname(binname: string) {
	if (process.platform === 'win32')
		return binname + '.exe';
	else
		return binname;
}

function correctScriptName(binname: string) {
	if (process.platform === 'win32')
		return binname + '.bat';
	else
		return binname;
}

function findJavaExecutableInJavaHome(javaHome: string, binname: string) {
    let workspaces = javaHome.split(path.delimiter);

    for (let i = 0; i < workspaces.length; i++) {
        let binpath = path.join(workspaces[i], 'bin', binname);

        if (fs.existsSync(binpath))
            return binpath;
    }
}
