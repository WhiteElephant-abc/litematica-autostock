package io.github.whiteelephantabc.autostock.client.scan

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.world.Container
import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.level.block.state.properties.ChestType
import net.minecraft.world.level.chunk.status.ChunkStatus
import kotlin.math.abs

object ContainerScanner {
	fun scan(level: ClientLevel, center: BlockPos, radius: Int, verticalRange: Int): List<ContainerTarget> = buildList {
		val minChunkX = (center.x - radius) shr 4
		val maxChunkX = (center.x + radius) shr 4
		val minChunkZ = (center.z - radius) shr 4
		val maxChunkZ = (center.z + radius) shr 4
		val radiusSq = radius.toLong() * radius
		val seen = mutableSetOf<BlockPos>()

		(minChunkX..maxChunkX).forEach { chunkX ->
			(minChunkZ..maxChunkZ).forEach { chunkZ ->
				level.chunkSource.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false)
					?.blockEntities
					?.forEach { (pos, blockEntity) ->
						if (blockEntity is Container && abs(pos.y - center.y) <= verticalRange) {
							val dx = (pos.x - center.x).toLong()
							val dz = (pos.z - center.z).toLong()

							if (dx * dx + dz * dz <= radiusSq) {
								val state = level.getBlockState(pos)

								if (state.block is ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
									val partner = ChestBlock.getConnectedBlockPos(pos, state)
									val partnerState = level.getBlockState(partner)

									if (partnerState.block is ChestBlock && partnerState.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
										val primary = if (pos.x < partner.x || (pos.x == partner.x && pos.z < partner.z)) pos else partner
										val secondary = if (primary == pos) partner else pos

										if (seen.add(primary)) {
											add(ContainerTarget.DoubleChest(primary, secondary))
										}
									} else if (seen.add(pos)) {
										add(ContainerTarget.Single(pos))
									}
								} else if (seen.add(pos)) {
									add(ContainerTarget.Single(pos))
								}
							}
						}
					}
			}
		}
	}
}
