package io.github.whiteelephantabc.autostock.client.ui

import io.github.whiteelephantabc.autostock.client.compat.MiniHudRange
import io.github.whiteelephantabc.autostock.client.config.AutoStockConfig
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.BlockPos
import org.slf4j.LoggerFactory

object SearchRangeRenderer {
	private val logger = LoggerFactory.getLogger("litematica-autostock/search-range")

	fun show(center: BlockPos, radius: Int, verticalRange: Int) {
		if (!AutoStockConfig.showSearchRange || !FabricLoader.getInstance().isModLoaded("minihud")) {
			return
		}

		runCatching { MiniHudRange.show(center, radius, verticalRange) }
			.onFailure { logger.warn("Failed to show MiniHUD search range: {}", it.message) }
	}

	fun clear() {
		if (!FabricLoader.getInstance().isModLoaded("minihud")) {
			return
		}

		runCatching { MiniHudRange.clear() }
			.onFailure { logger.warn("Failed to clear MiniHUD search range: {}", it.message) }
	}
}
