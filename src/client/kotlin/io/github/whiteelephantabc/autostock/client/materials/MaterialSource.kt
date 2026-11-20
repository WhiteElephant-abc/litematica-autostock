package io.github.whiteelephantabc.autostock.client.materials

import fi.dy.masa.litematica.data.DataManager
import fi.dy.masa.litematica.materials.MaterialListEntry
import fi.dy.masa.litematica.materials.MaterialListPlacement
import fi.dy.masa.malilib.interfaces.ICompletionListener
import io.github.whiteelephantabc.autostock.client.config.AutoStockConfig

object MaterialSource {
	fun refresh(onDone: (List<MaterialListEntry>) -> Unit, onError: (String) -> Unit) {
		val placement = DataManager.getSchematicPlacementManager().selectedSchematicPlacement

		if (placement == null) {
			onError("no schematic placement selected")
			return
		}

		if (!AutoStockConfig.autoRefreshMaterialList) {
			val list = DataManager.getMaterialList()

			if (list == null || list.name != placement.name) {
				onError("no material list for the selected placement; refresh it in Litematica first")
				return
			}

			if (list.materialsAll.isEmpty()) {
				onError("the material list is empty; refresh it in Litematica first")
				return
			}

			onDone(list.materialsAll)
			return
		}

		val list = MaterialListPlacement(placement, false)

		list.setCompletionListener(object : ICompletionListener {
			override fun onTaskCompleted() {
				onDone(list.materialsAll)
			}

			override fun onTaskAborted() {
				onError("material list refresh aborted")
			}
		})

		list.reCreateMaterialList()
	}
}
