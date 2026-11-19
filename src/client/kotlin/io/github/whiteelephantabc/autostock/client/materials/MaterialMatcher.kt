package io.github.whiteelephantabc.autostock.client.materials

import fi.dy.masa.litematica.materials.MaterialListEntry
import net.minecraft.world.item.ItemStack

data class MissingMaterial(val stack: ItemStack, val required: Int)

data class MatchedMaterial(val stack: ItemStack, val available: Int)

object MaterialMatcher {
	fun missing(list: List<MaterialListEntry>): List<MissingMaterial> =
		list.mapNotNull {
			val required = it.countMissing - it.countAvailable

			if (required > 0) MissingMaterial(it.stack, required) else null
		}

	fun matches(contents: List<ItemStack>, missing: List<MissingMaterial>): List<MatchedMaterial> =
		missing.mapNotNull { material ->
			val available = contents
				.filter { ItemStack.isSameItem(it, material.stack) }
				.sumOf { it.count }

			if (available > 0) MatchedMaterial(material.stack, available) else null
		}
}
