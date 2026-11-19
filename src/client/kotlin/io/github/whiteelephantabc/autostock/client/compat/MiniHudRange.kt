package io.github.whiteelephantabc.autostock.client.compat

import fi.dy.masa.malilib.util.position.Vec3d
import fi.dy.masa.minihud.config.RendererToggle
import fi.dy.masa.minihud.renderer.shapes.ShapeCircle
import fi.dy.masa.minihud.renderer.shapes.ShapeManager
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3

object MiniHudRange {
	private var shape: ShapeCircle? = null
	private var previousShapeRenderer: Boolean? = null

	fun show(center: BlockPos, radius: Int, verticalRange: Int) {
		clear()

		val rendererToggle = RendererToggle.SHAPE_RENDERER
		previousShapeRenderer = rendererToggle.booleanValue

		if (!rendererToggle.booleanValue) {
			rendererToggle.setBooleanValue(true)
		}

		val circle = ShapeCircle()
		circle.setCenter(Vec3d.of(Vec3(center.x + 0.5, (center.y - verticalRange).toDouble(), center.z + 0.5)))
		circle.setRadius(radius.toDouble())
		circle.setHeight(verticalRange * 2 + 1)

		if (!circle.isEnabled) {
			circle.toggleEnabled()
		}

		ShapeManager.INSTANCE.addShape(circle)
		shape = circle
	}

	fun clear() {
		val current = shape
		val previous = previousShapeRenderer

		if (current != null) {
			ShapeManager.INSTANCE.removeShape(current)
			shape = null
		}

		if (previous != null) {
			RendererToggle.SHAPE_RENDERER.setBooleanValue(previous)
			previousShapeRenderer = null
		}
	}
}
