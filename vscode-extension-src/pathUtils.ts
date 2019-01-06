import * as path from "path";
import * as fs from "fs";

export function correctBinname(binname: string) {
	if (process.platform === 'win32')
		return binname + '.exe';
	else
		return binname;
}

export function correctScriptName(binname: string) {
	if (process.platform === 'win32')
		return binname + '.bat';
	else
		return binname;
}

export function findJavaExecutableInJavaHome(javaHome: string, binname: string) {
    let workspaces = javaHome.split(path.delimiter);

    for (let i = 0; i < workspaces.length; i++) {
        let binpath = path.join(workspaces[i], 'bin', binname);

        if (fs.existsSync(binpath))
            return binpath;
    }
}
