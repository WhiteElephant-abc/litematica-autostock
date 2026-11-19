package io.github.whiteelephantabc.autostock.client.pathing

import net.minecraft.core.BlockPos

interface ContainerPathingListener {
	fun onArrived(pos: BlockPos)

	fun onPathingComplete()

	fun onPathingCancelled()

	fun onPathingSkipped(pos: BlockPos, reason: String)
}
