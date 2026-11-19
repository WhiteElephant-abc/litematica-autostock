package io.github.whiteelephantabc.autostock.client.content

import fi.dy.masa.litematica.config.Configs
import fi.dy.masa.malilib.util.InventoryUtils
import io.github.whiteelephantabc.autostock.client.scan.ContainerTarget
import io.github.whiteelephantabc.autostock.client.servux.ServuxClient
import io.github.whiteelephantabc.autostock.client.util.TickPacer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import net.minecraft.core.BlockPos
import org.slf4j.LoggerFactory

object ServuxContentSource : ContainerContentSource {
	private val logger = LoggerFactory.getLogger("litematica-autostock/servux-content")

	override fun requestAll(targets: List<ContainerTarget>): Flow<ContainerContent> = channelFlow {
		val requests = buildList {
			targets.forEach { target ->
				when (target) {
					is ContainerTarget.Single -> add(target to target.primary)
					is ContainerTarget.DoubleChest -> {
						add(target to target.primary)
						add(target to target.secondary)
					}
				}
			}
		}
		val rate = Configs.Generic.SERVER_NBT_REQUEST_RATE.integerValue.coerceAtLeast(1)

		requests.chunked(rate).forEach { batch ->
			TickPacer.waitTicks(1)

			batch.forEach { (target, pos) ->
				launch {
					val nbt = ServuxClient.requestBlockEntity(pos) ?: return@launch
					val inventory = runCatching { InventoryUtils.getDataInventory(nbt) }
						.onFailure { logger.warn("Failed to read inventory at {}: {}", pos, it.message) }
						.getOrNull()

					if (inventory == null) {
						logger.warn("No inventory data at {}", pos)
						return@launch
					}

					val items = (0 until inventory.containerSize)
						.map { inventory.getItem(it) }
						.filter { !it.isEmpty }

					send(ContainerContent(target, items))
				}
			}
		}
	}
}
