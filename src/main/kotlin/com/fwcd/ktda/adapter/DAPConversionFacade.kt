package com.fwcd.ktda.adapter

import java.nio.file.Paths

typealias DAPSource = org.eclipse.lsp4j.debug.Source
typealias InternalSource = com.fwcd.ktda.core.Source

/**
 * Handles conversions between debug adapter types
 * and internal types.
 */
class DAPConversionFacade(
	var lineConverter: LineNumberConverter = LineNumberConverter()
) {
	fun toInternalSource(dapSource: DAPSource) = InternalSource(
		name = dapSource.name,
		filePath = Paths.get(dapSource.path)
	)
	
	fun toDAPSource(internalSource: InternalSource) = DAPSource().apply {
		name = internalSource.name
		path = internalSource.filePath.toAbsolutePath().toString()
	}
}
