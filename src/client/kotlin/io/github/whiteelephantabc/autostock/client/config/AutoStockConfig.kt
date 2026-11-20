package io.github.whiteelephantabc.autostock.client.config

object AutoStockConfig {
	val searchRadius: Int
		get() = AutoStockConfigs.SEARCH_RADIUS.integerValue

	val verticalRange: Int
		get() = AutoStockConfigs.VERTICAL_RANGE.integerValue

	val containerWhitelist: List<String>
		get() = AutoStockConfigs.CONTAINER_WHITELIST.strings

	val defaultItemCount: Int
		get() = AutoStockConfigs.DEFAULT_ITEM_COUNT.integerValue

	val autoRefreshMaterialList: Boolean
		get() = AutoStockConfigs.AUTO_REFRESH_MATERIAL_LIST.booleanValue

	val allowBreakDuringPathing: Boolean
		get() = AutoStockConfigs.ALLOW_BREAK_DURING_PATHING.booleanValue

	val allowPlaceDuringPathing: Boolean
		get() = AutoStockConfigs.ALLOW_PLACE_DURING_PATHING.booleanValue

	val showSearchRange: Boolean
		get() = AutoStockConfigs.SHOW_SEARCH_RANGE.booleanValue
}
