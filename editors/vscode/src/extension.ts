import * as vscode from "vscode";
import * as path from "path";
import * as fs from "fs";
import { LOG } from "./logger";
import { findJavaExecutableInJavaHome, correctBinname } from "./pathUtils";

// This method is called when your extension is activated
export function activate(context: vscode.ExtensionContext): void {
	LOG.info("Activating Kotlin debug adapter...");
	
	let javaExecutablePath = findJavaExecutable("java");
	if ((javaExecutablePath === null) || (javaExecutablePath === undefined)) {
		vscode.window.showErrorMessage("Couldn't locate java in $JAVA_HOME or $PATH");
		return;
	}
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
