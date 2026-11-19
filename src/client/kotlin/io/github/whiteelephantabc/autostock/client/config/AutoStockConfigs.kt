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
	val searchRadius: Int = 16,
	val verticalRange: Int = 16,
	val containerWhitelist: List<String> = DEFAULT_CONTAINER_WHITELIST,
	val allowBreakDuringPathing: Boolean = false,
	val allowPlaceDuringPathing: Boolean = true,
	val showSearchRange: Boolean = true
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
	val ALLOW_BREAK_DURING_PATHING = ConfigBoolean("allowBreakDuringPathing", false).apply(GENERIC_KEY)
	val ALLOW_PLACE_DURING_PATHING = ConfigBoolean("allowPlaceDuringPathing", true).apply(GENERIC_KEY)
	val SHOW_SEARCH_RANGE = ConfigBoolean("showSearchRange", true).apply(GENERIC_KEY)

	val OPTIONS: List<IConfigBase> = listOf(
		SEARCH_RADIUS,
		VERTICAL_RANGE,
		CONTAINER_WHITELIST,
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

		SEARCH_RADIUS.setIntegerValue(data.searchRadius)
		VERTICAL_RANGE.setIntegerValue(data.verticalRange)
		CONTAINER_WHITELIST.setStrings(data.containerWhitelist)
		ALLOW_BREAK_DURING_PATHING.setBooleanValue(data.allowBreakDuringPathing)
		ALLOW_PLACE_DURING_PATHING.setBooleanValue(data.allowPlaceDuringPathing)
		SHOW_SEARCH_RANGE.setBooleanValue(data.showSearchRange)
	}

	fun save() {
		val dir = FileUtils.getConfigDirectory()
		Files.createDirectories(dir)

		val data = AutoStockConfigData(
			searchRadius = SEARCH_RADIUS.integerValue,
			verticalRange = VERTICAL_RANGE.integerValue,
			containerWhitelist = CONTAINER_WHITELIST.strings,
			allowBreakDuringPathing = ALLOW_BREAK_DURING_PATHING.booleanValue,
			allowPlaceDuringPathing = ALLOW_PLACE_DURING_PATHING.booleanValue,
			showSearchRange = SHOW_SEARCH_RANGE.booleanValue
		)

		runCatching { Files.writeString(dir.resolve(CONFIG_FILE_NAME), json.encodeToString(data)) }
			.onFailure { logger.warn("Failed to save config: {}", it.message) }
	}
}
