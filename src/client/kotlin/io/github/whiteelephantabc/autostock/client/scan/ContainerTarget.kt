package io.github.whiteelephantabc.autostock.client.scan

import net.minecraft.core.BlockPos

sealed interface ContainerTarget {
	val primary: BlockPos

	data class Single(override val primary: BlockPos) : ContainerTarget

	data class DoubleChest(override val primary: BlockPos, val secondary: BlockPos) : ContainerTarget
}
