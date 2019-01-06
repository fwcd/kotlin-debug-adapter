import * as child_process from "child_process";
import * as path from "path";
import { LOG } from "./logger";
import { isOSUnixoid } from "./osUtils";
import { correctScriptName } from "./pathUtils";

function launchDebugAdapter() {
	let startScriptPath = path.resolve(__dirname, "..", "build", "install", "KotlinDebugAdapter", "bin", correctScriptName("KotlinDebugAdapter"));
	let args = [];
	
	// Ensure that start script can be executed
	if (isOSUnixoid()) {
		child_process.exec("chmod +x " + startScriptPath);
	}
	
	LOG.info("Found debug adapter {} with args {}", startScriptPath, args.join(" "));
	
	// TODO: Logging/redirection of stdout
	child_process.spawn(startScriptPath, args, {
		stdio: "inherit"
	})
}

launchDebugAdapter()
