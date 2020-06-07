package org.javacs.ktda.core.event

import org.javacs.ktda.core.exception.DebuggeeException

class ExceptionStopEvent(
	val threadID: Long,
	val exception: DebuggeeException
)
