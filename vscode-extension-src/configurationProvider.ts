
// TODO: Use this class to provide the executable path once the new API is added
// See https://github.com/Microsoft/vscode/issues/43101#issuecomment-364068789
// See https://code.visualstudio.com/updates/v1_20#_debug-api
// See https://github.com/Microsoft/vscode/blob/7636a7d6f7d2749833f783e94fd3d48d6a1791cb/src/vs/vscode.proposed.d.ts#L388-L395
// =============================================================================

// import * as vscode from "vscode";

// export class KotlinDebugConfigurationProvider implements vscode.DebugConfigurationProvider {
// 	private readonly extensionContext: vscode.ExtensionContext;
	
// 	public constructor(extensionContext: vscode.ExtensionContext) {
// 		this.extensionContext = extensionContext;
// 	}
	
// 	public debugAdapterExecutable?(folder: WorkspaceFolder | undefined, token?: CancellationToken): ProviderResult<DebugAdapterExecutable> {
// 		return new vscode.DebugAdapterExecutable('node', [ join(this.extensionContext.extensionPath, "bin", "build", "install", "KotlinDebugAdapter", "bin", correctScriptName("KotlinDebugAdapter") ]);
// 	}
// }
