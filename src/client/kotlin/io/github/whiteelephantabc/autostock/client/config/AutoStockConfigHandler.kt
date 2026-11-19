package io.github.whiteelephantabc.autostock.client.config

import fi.dy.masa.malilib.config.IConfigHandler

object AutoStockConfigHandler : IConfigHandler {
	override fun load() = AutoStockConfigs.load()

	override fun save() = AutoStockConfigs.save()
}
