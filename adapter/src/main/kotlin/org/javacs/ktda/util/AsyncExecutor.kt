package org.javacs.ktda.util

import java.time.Duration
import java.util.function.Supplier
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

private var threadCount = 0

class AsyncExecutor {
	private val workerThread = Executors.newSingleThreadExecutor { Thread(it, "async${threadCount++}") }
	
	fun run(task: () -> Unit) =
			CompletableFuture.runAsync(Runnable(task), workerThread)
	
	fun <R> compute(task: () -> R) =
			CompletableFuture.supplyAsync(Supplier(task), workerThread)
}
