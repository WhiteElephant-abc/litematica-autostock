package io.github.whiteelephantabc.autostock.client.pathing

import baritone.api.IBaritone
import baritone.api.pathing.goals.GoalNear
import baritone.api.process.IBaritoneProcess
import baritone.api.process.PathingCommand
import baritone.api.process.PathingCommandType
import net.minecraft.core.BlockPos

class AutoStockProcess(
	private val baritone: IBaritone,
	private val listener: ContainerPathingListener
) : IBaritoneProcess {
	private var active: Boolean = false
	private var waitingContainer: Boolean = false
	private var targets: List<BlockPos> = emptyList()
	private var index: Int = 0
	private var goalRadius: Int = MAX_GOAL_RADIUS
	private var goal: GoalNear? = null

	fun begin(newTargets: List<BlockPos>) {
		targets = newTargets
		index = 0
		goalRadius = MAX_GOAL_RADIUS
		goal = null
		waitingContainer = false
		active = true
	}

	fun advance() {
		moveNext()
	}

	fun stop() {
		if (!active) return

		active = false
		waitingContainer = false
		listener.onPathingComplete()
	}

	fun retryCloser(): Int? {
		if (goalRadius <= MIN_GOAL_RADIUS) {
			return null
		}

		goalRadius--
		waitingContainer = false
		goal = null
		return goalRadius
	}

	private fun moveNext() {
		waitingContainer = false
		goal = null
		goalRadius = MAX_GOAL_RADIUS
		index++

		if (index >= targets.size) {
			finish()
		}
	}

	private fun finish() {
		active = false
		waitingContainer = false
		listener.onPathingComplete()
	}

	private fun goalForNext(): GoalNear? {
		val target = targets.getOrNull(index) ?: return null
		return GoalNear(target, goalRadius).also { goal = it }
	}

	override fun isActive(): Boolean {
		return active
	}

	override fun onTick(calcFailed: Boolean, isSafeToCancel: Boolean): PathingCommand? {
		if (!active) {
			return null
		}

		if (waitingContainer) {
			return PathingCommand(null, PathingCommandType.REQUEST_PAUSE)
		}

		val target = targets.getOrNull(index)

		if (target == null) {
			finish()
			return null
		}

		val feet = baritone.playerContext.playerFeet()
		val currentGoal = goal ?: GoalNear(target, goalRadius).also { goal = it }

		if (currentGoal.isInGoal(feet.x, feet.y, feet.z)) {
			waitingContainer = true
			listener.onArrived(target)
			return PathingCommand(null, PathingCommandType.REQUEST_PAUSE)
		}

		if (calcFailed) {
			listener.onPathingSkipped(target, "no path")
			moveNext()

			return if (active) {
				PathingCommand(goalForNext(), PathingCommandType.SET_GOAL_AND_PATH)
			} else {
				null
			}
		}

		return PathingCommand(currentGoal, PathingCommandType.SET_GOAL_AND_PATH)
	}

	override fun isTemporary(): Boolean {
		return false
	}

	override fun onLostControl() {
		if (active) {
			active = false
			waitingContainer = false
			listener.onPathingCancelled()
		}
	}

	override fun displayName0(): String {
		return "Litematica AutoStock"
	}

	private companion object {
		private const val MAX_GOAL_RADIUS = 5
		private const val MIN_GOAL_RADIUS = 3
	}
}
