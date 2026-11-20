package io.github.whiteelephantabc.autostock.client.config

import com.google.common.collect.ImmutableList
import fi.dy.masa.malilib.config.IConfigBase
import fi.dy.masa.malilib.config.options.ConfigBoolean
import fi.dy.masa.malilib.config.options.ConfigInteger
import fi.dy.masa.malilib.config.options.ConfigStringList
import fi.dy.masa.malilib.util.FileUtils
import io.github.whiteelephantabc.autostock.LitematicaAutostock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.nio.file.Files

private val DEFAULT_CONTAINER_WHITELIST: List<String> = listOf(
	"minecraft:chest",
	"minecraft:trapped_chest",
	"minecraft:barrel",
	"minecraft:shulker_box",
	"minecraft:white_shulker_box",
	"minecraft:orange_shulker_box",
	"minecraft:magenta_shulker_box",
	"minecraft:light_blue_shulker_box",
	"minecraft:yellow_shulker_box",
	"minecraft:lime_shulker_box",
	"minecraft:pink_shulker_box",
	"minecraft:gray_shulker_box",
	"minecraft:light_gray_shulker_box",
	"minecraft:cyan_shulker_box",
	"minecraft:purple_shulker_box",
	"minecraft:blue_shulker_box",
	"minecraft:brown_shulker_box",
	"minecraft:green_shulker_box",
	"minecraft:red_shulker_box",
	"minecraft:black_shulker_box"
)

@Serializable
data class AutoStockConfigData(
	val searchRadius: Int? = null,
	val verticalRange: Int? = null,
	val containerWhitelist: List<String>? = null,
	val defaultItemCount: Int? = null,
	val allowBreakDuringPathing: Boolean? = null,
	val allowPlaceDuringPathing: Boolean? = null,
	val showSearchRange: Boolean? = null
)

object AutoStockConfigs {
	private const val CONFIG_FILE_NAME = "${LitematicaAutostock.MOD_ID}.json"
	private val GENERIC_KEY = "${LitematicaAutostock.MOD_ID}.config.generic"
	private val logger = LoggerFactory.getLogger("${LitematicaAutostock.MOD_ID}/config")
	private val json = Json {
		prettyPrint = true
		encodeDefaults = true
		ignoreUnknownKeys = true
	}

	val SEARCH_RADIUS = ConfigInteger("searchRadius", 16, 1, 64).apply(GENERIC_KEY)
	val VERTICAL_RANGE = ConfigInteger("verticalRange", 16, 0, 64).apply(GENERIC_KEY)
	val CONTAINER_WHITELIST =
		ConfigStringList("containerWhitelist", ImmutableList.copyOf(DEFAULT_CONTAINER_WHITELIST)).apply(GENERIC_KEY)
	val DEFAULT_ITEM_COUNT = ConfigInteger("defaultItemCount", 1, 1, 9999).apply(GENERIC_KEY)
	val ALLOW_BREAK_DURING_PATHING = ConfigBoolean("allowBreakDuringPathing", false).apply(GENERIC_KEY)
	val ALLOW_PLACE_DURING_PATHING = ConfigBoolean("allowPlaceDuringPathing", true).apply(GENERIC_KEY)
	val SHOW_SEARCH_RANGE = ConfigBoolean("showSearchRange", true).apply(GENERIC_KEY)

	val OPTIONS: List<IConfigBase> = listOf(
		SEARCH_RADIUS,
		VERTICAL_RANGE,
		CONTAINER_WHITELIST,
		DEFAULT_ITEM_COUNT,
		ALLOW_BREAK_DURING_PATHING,
		ALLOW_PLACE_DURING_PATHING,
		SHOW_SEARCH_RANGE
	)

	fun load() {
		val file = FileUtils.getConfigDirectory().resolve(CONFIG_FILE_NAME)

		if (!Files.exists(file) || !Files.isReadable(file)) return

		val data = runCatching { json.decodeFromString<AutoStockConfigData>(Files.readString(file)) }
			.getOrElse {
				logger.warn("Failed to load config: {}", it.message)
				return
			}

		data.searchRadius?.let { SEARCH_RADIUS.setIntegerValue(it) }
		data.verticalRange?.let { VERTICAL_RANGE.setIntegerValue(it) }
		data.containerWhitelist?.let { CONTAINER_WHITELIST.setStrings(it) }
		data.defaultItemCount?.let { DEFAULT_ITEM_COUNT.setIntegerValue(it) }
		data.allowBreakDuringPathing?.let { ALLOW_BREAK_DURING_PATHING.setBooleanValue(it) }
		data.allowPlaceDuringPathing?.let { ALLOW_PLACE_DURING_PATHING.setBooleanValue(it) }
		data.showSearchRange?.let { SHOW_SEARCH_RANGE.setBooleanValue(it) }
	}

	fun save() {
		val dir = FileUtils.getConfigDirectory()
		Files.createDirectories(dir)

		val data = AutoStockConfigData(
			searchRadius = SEARCH_RADIUS.integerValue,
			verticalRange = VERTICAL_RANGE.integerValue,
			containerWhitelist = CONTAINER_WHITELIST.strings,
			defaultItemCount = DEFAULT_ITEM_COUNT.integerValue,
			allowBreakDuringPathing = ALLOW_BREAK_DURING_PATHING.booleanValue,
			allowPlaceDuringPathing = ALLOW_PLACE_DURING_PATHING.booleanValue,
			showSearchRange = SHOW_SEARCH_RANGE.booleanValue
		)

		runCatching { Files.writeString(dir.resolve(CONFIG_FILE_NAME), json.encodeToString(data)) }
			.onFailure { logger.warn("Failed to save config: {}", it.message) }
	}
}
