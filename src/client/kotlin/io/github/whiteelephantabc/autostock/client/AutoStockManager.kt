package io.github.whiteelephantabc.autostock.client

import io.github.whiteelephantabc.autostock.client.collector.MaterialCollector
import io.github.whiteelephantabc.autostock.client.collector.ServuxMaterialCollector
import io.github.whiteelephantabc.autostock.client.container.ContainerSession
import io.github.whiteelephantabc.autostock.client.materials.MaterialRequest
import io.github.whiteelephantabc.autostock.client.servux.ServuxClient
import io.github.whiteelephantabc.autostock.client.util.TickPacer

object AutoStockManager {
	private var collector: MaterialCollector? = null
	private var suppressNextContainer: Boolean = false

	fun start(requests: List<MaterialRequest>) {
		collector?.cancel()
		collector = ServuxMaterialCollector().also { it.start(requests) }
	}

	fun tick() {
		ContainerSession.tick()
		ServuxClient.tick()
		TickPacer.tick()
		collector?.tick()
	}

	fun beginSuppress() {
		suppressNextContainer = true
	}

	fun shouldSuppressScreen(containerId: Int): Boolean {
		val suppress = suppressNextContainer
		suppressNextContainer = false
		return suppress
	}
}
