package io.github.whiteelephantabc.autostock.client.collector

import io.github.whiteelephantabc.autostock.client.materials.MaterialRequest

interface MaterialCollector {
	fun start(requests: List<MaterialRequest>)

	fun tick()

	fun cancel()
}
