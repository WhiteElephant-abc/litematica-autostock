package io.github.whiteelephantabc.autostock.client.pathing

import baritone.api.process.IBaritoneProcess
import baritone.api.process.PathingCommand
import baritone.api.process.PathingCommandType

class CollectorControlProcess(private val onCancelled: () -> Unit) : IBaritoneProcess {
	private var active: Boolean = false

	fun activate() {
		active = true
	}

	fun deactivate() {
		active = false
	}

	override fun isActive(): Boolean {
		return active
	}

	override fun onTick(calcFailed: Boolean, isSafeToCancel: Boolean): PathingCommand {
		return PathingCommand(null, PathingCommandType.REQUEST_PAUSE)
	}

	override fun isTemporary(): Boolean {
		return false
	}

	override fun onLostControl() {
		if (active) {
			active = false
			onCancelled()
		}
	}

	override fun displayName0(): String {
		return "Litematica AutoStock scan"
	}
}
