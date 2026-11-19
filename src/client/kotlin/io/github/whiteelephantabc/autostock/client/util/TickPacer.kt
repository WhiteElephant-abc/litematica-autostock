package io.github.whiteelephantabc.autostock.client.util

import kotlinx.coroutines.channels.Channel

object TickPacer {
	private val ticks = Channel<Unit>(Channel.CONFLATED)

	fun tick() {
		ticks.trySend(Unit)
	}

	suspend fun waitTicks(count: Int) {
		repeat(count) { ticks.receive() }
	}
}
