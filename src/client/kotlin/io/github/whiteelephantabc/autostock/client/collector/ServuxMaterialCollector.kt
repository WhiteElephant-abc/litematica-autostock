package io.github.whiteelephantabc.autostock.client.collector

import baritone.api.BaritoneAPI
import baritone.api.IBaritone
import baritone.api.utils.Helper
import io.github.whiteelephantabc.autostock.client.config.AutoStockConfig
import io.github.whiteelephantabc.autostock.client.container.ContainerSession
import io.github.whiteelephantabc.autostock.client.content.ServuxContentSource
import io.github.whiteelephantabc.autostock.client.materials.MaterialMatcher
import io.github.whiteelephantabc.autostock.client.materials.MaterialRequest
import io.github.whiteelephantabc.autostock.client.materials.MaterialSource
import io.github.whiteelephantabc.autostock.client.materials.MissingMaterial
import io.github.whiteelephantabc.autostock.client.pathing.AutoStockProcess
import io.github.whiteelephantabc.autostock.client.pathing.CollectorControlProcess
import io.github.whiteelephantabc.autostock.client.pathing.ContainerPathingListener
import io.github.whiteelephantabc.autostock.client.scan.ContainerScanner
import io.github.whiteelephantabc.autostock.client.scan.ContainerTarget
import io.github.whiteelephantabc.autostock.client.servux.ServuxClient
import io.github.whiteelephantabc.autostock.client.ui.SearchRangeRenderer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import org.slf4j.LoggerFactory
import kotlin.coroutines.CoroutineContext

class ServuxMaterialCollector : MaterialCollector, ContainerPathingListener {
	private val logger = LoggerFactory.getLogger("litematica-autostock/servux-collector")
	private val clientDispatcher = object : CoroutineDispatcher() {
		override fun dispatch(context: CoroutineContext, block: Runnable) {
			Minecraft.getInstance().execute(block)
		}
	}
	private val scope = CoroutineScope(
		SupervisorJob() + clientDispatcher + CoroutineExceptionHandler { _, t ->
			logger.error("servux collector failed", t)
			SearchRangeRenderer.clear()
		}
	)

	private var job: Job? = null
	private var process: AutoStockProcess? = null
	private var scanControl: CollectorControlProcess? = null
	private var baritone: IBaritone? = null
	private var cancelled: Boolean = false
	private var settingsSaved: Boolean = false
	private var oldAllowBreak: Boolean = true
	private var oldAllowPlace: Boolean = true

	private val lootedPositions = mutableSetOf<BlockPos>()
	private var remaining: MutableMap<Item, Int> = mutableMapOf()

	var candidateTargets: List<ContainerTarget> = emptyList()
		private set

	override fun start(requests: List<MaterialRequest>) {
		val mc = Minecraft.getInstance()
		val level = mc.level
		val player = mc.player

		if (level == null || player == null) {
			return
		}

		cancelled = false
		val center = player.blockPosition()
		SearchRangeRenderer.show(center, AutoStockConfig.searchRadius, AutoStockConfig.verticalRange)
		startScanControl()

		if (requests.isNotEmpty()) {
			job = scope.launch {
				collect(requests.map { MissingMaterial(ItemStack(it.item), it.count) }, level, center)
			}
			return
		}

		MaterialSource.refresh(
			onDone = { list ->
				if (cancelled) {
					return@refresh
				}

				job = scope.launch { collect(MaterialMatcher.missing(list), level, center) }
			},
			onError = {
				logger.warn("material list unavailable: {}", it)
				Minecraft.getInstance().execute {
					message("Material list unavailable: $it")
					finishScan()
				}
			}
		)
	}

	private fun startScanControl() {
		val activeBaritone = BaritoneAPI.getProvider().primaryBaritone ?: return
		baritone = activeBaritone

		val control = scanControl ?: CollectorControlProcess { onScanCancelled() }.also {
			scanControl = it
			activeBaritone.pathingControlManager.registerProcess(it)
		}

		control.activate()
	}

	private fun onScanCancelled() {
		cancelled = true
		job?.cancel()
		SearchRangeRenderer.clear()
		candidateTargets = emptyList()
		logger.info("scan cancelled")
	}

	private suspend fun collect(
		missing: List<MissingMaterial>,
		level: ClientLevel,
		center: BlockPos
	) {
		if (cancelled) return

		if (!ServuxClient.available) {
			logger.info("servux unavailable")
			message("Servux unavailable")
			finishScan()
			return
		}

		if (missing.isEmpty()) {
			logger.info("nothing missing")
			message("Nothing missing")
			finishScan()
			return
		}

		val required = missing
			.groupBy { it.stack.item }
			.mapValues { (_, materials) -> materials.sumOf { it.required } }

		remaining = required.toMutableMap()

		val targets = ContainerScanner.scan(
			level,
			center,
			AutoStockConfig.searchRadius,
			AutoStockConfig.verticalRange
		)
			.filter { target -> isAllowedContainer(level, target) }
			.sortedBy { target ->
				val pos = target.primary
				val dx = (pos.x - center.x).toLong()
				val dy = (pos.y - center.y).toLong()
				val dz = (pos.z - center.z).toLong()
				dx * dx + dy * dy + dz * dz
			}

		message("Scanning ${targets.size} container(s), ${missing.size} missing type(s)")

		val found = mutableListOf<ContainerTarget>()
		val foundTargets = mutableSetOf<ContainerTarget>()
		val covered = mutableMapOf<Item, Int>()
		val order = targets.withIndex().associate { it.value to it.index }

		ServuxContentSource.requestAll(targets)
			.transformWhile { content ->
				val matches = MaterialMatcher.matches(content.items, missing)

				if (matches.isNotEmpty()) {
					if (foundTargets.add(content.target)) {
						found += content.target
					}

					matches.forEach { match ->
						covered.merge(match.stack.item, match.available) { a, b -> a + b }
					}
				}

				val unsatisfied = required.any { (item, need) -> (covered[item] ?: 0) < need }

				if (!unsatisfied) logger.info("scan stopped early with {} candidate(s)", found.size)

				emit(content)

				unsatisfied
			}
			.collect()

		if (cancelled) return

		found.sortBy { order[it] ?: Int.MAX_VALUE }

		candidateTargets = found

		if (found.isNotEmpty()) {
			message("Found ${found.size} candidate(s), starting pathing")
			startPathing(found)
		} else {
			message("Found 0 candidate(s)")
			finishScan()
		}
	}

	private fun message(text: String) =
		Helper.HELPER.logDirect(text)

	private fun finishScan() {
		scanControl?.deactivate()
		SearchRangeRenderer.clear()
	}

	private fun isAllowedContainer(level: ClientLevel, target: ContainerTarget): Boolean {
		val whitelist = AutoStockConfig.containerWhitelist

		if (whitelist.isEmpty()) return true

		val id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(target.primary).block).toString()

		return whitelist.any { entry -> normalizeId(entry).equals(id, ignoreCase = true) }
	}

	private fun normalizeId(id: String): String {
		val trimmed = id.trim()

		return if (trimmed.contains(':')) trimmed else "minecraft:$trimmed"
	}

	private fun startPathing(targets: List<ContainerTarget>) {
		if (cancelled) {
			finishScan()
			return
		}

		val activeBaritone = BaritoneAPI.getProvider().primaryBaritone ?: run {
			finishScan()
			return
		}
		baritone = activeBaritone
		scanControl?.deactivate()

		val collectorProcess = process ?: AutoStockProcess(activeBaritone, this).also {
			process = it
			activeBaritone.pathingControlManager.registerProcess(it)
		}

		savePathingSettings()
		activeBaritone.pathingBehavior.cancelEverything()
		collectorProcess.begin(targets.map { it.primary })
	}

	override fun onArrived(pos: BlockPos) {
		val reachable = Minecraft.getInstance().player?.isWithinBlockInteractionRange(pos, 1.0) == true

		if (!reachable) {
			val radius = process?.retryCloser()

			if (radius != null) {
				logger.info("arrived at {} out of reach, retrying with radius {}", pos, radius)
				return
			}

			markLooted(targetAt(pos))
			logger.info("skipped {}: out of reach", pos)
			process?.advance()
			return
		}

		message("Reached target, taking items")

		val wanted = remaining.filterValues { it > 0 }.toMap()

		if (wanted.isEmpty()) {
			stopCollecting()
			return
		}

		val opened = ContainerSession.open(
			pos,
			wanted,
			onFinished = { taken -> onTaken(pos, taken) },
			onFailed = { reason ->
				markLooted(targetAt(pos))
				logger.info("skipped {}: {}", pos, reason)
				process?.advance()
			}
		)

		if (!opened) {
			markLooted(targetAt(pos))
			logger.info("skipped {}: container did not open", pos)
			process?.advance()
		}
	}

	override fun onPathingComplete() {
		restorePathingSettings()
		finishScan()
		reportRemaining()
		logger.info("pathing complete")
	}

	override fun onPathingCancelled() {
		ContainerSession.cancel()
		restorePathingSettings()
		finishScan()
		reportRemaining()
		logger.info("pathing cancelled")
	}

	private fun reportRemaining() {
		if (remaining.isEmpty()) return

		val text = remaining.entries.joinToString(", ") { (item, count) ->
			"${BuiltInRegistries.ITEM.getKey(item)} x$count"
		}

		message("Missing materials: $text")
	}

	override fun onPathingSkipped(pos: BlockPos, reason: String) {
		markLooted(targetAt(pos))
		logger.info("skipped {}: {}", pos, reason)
	}

	private fun onTaken(pos: BlockPos, taken: Map<Item, Int>) {
		taken.forEach { (item, count) ->
			val left = (remaining[item] ?: 0) - count

			if (left > 0) remaining[item] = left
			else remaining.remove(item)
		}

		markLooted(targetAt(pos))

		if (taken.isEmpty()) logger.info("nothing to take at {}", pos)
		else logger.info("took {} at {}", taken, pos)

		if (remaining.isEmpty()) {
			message("All materials satisfied")
			stopCollecting()
		} else {
			process?.advance()
		}
	}

	private fun stopCollecting() {
		process?.stop()
	}

	private fun savePathingSettings() {
		val settings = BaritoneAPI.getSettings()
		oldAllowBreak = settings.allowBreak.value
		oldAllowPlace = settings.allowPlace.value
		settings.allowBreak.value = AutoStockConfig.allowBreakDuringPathing
		settings.allowPlace.value = AutoStockConfig.allowPlaceDuringPathing
		settingsSaved = true
	}

	private fun restorePathingSettings() {
		if (!settingsSaved) {
			return
		}

		val settings = BaritoneAPI.getSettings()
		settings.allowBreak.value = oldAllowBreak
		settings.allowPlace.value = oldAllowPlace
		settingsSaved = false
	}

	private fun targetAt(pos: BlockPos): ContainerTarget? =
		candidateTargets.find { it.primary == pos }

	private fun markLooted(target: ContainerTarget?) {
		when (target) {
			null -> return
			is ContainerTarget.Single -> lootedPositions += target.primary
			is ContainerTarget.DoubleChest -> {
				lootedPositions += target.primary
				lootedPositions += target.secondary
			}
		}
	}

	override fun tick() {
	}

	override fun cancel() {
		cancelled = true
		ContainerSession.cancel()
		scanControl?.deactivate()
		job?.cancel()
		baritone?.pathingBehavior?.cancelEverything()
		restorePathingSettings()
		SearchRangeRenderer.clear()
		candidateTargets = emptyList()
		remaining = mutableMapOf()
	}
}
