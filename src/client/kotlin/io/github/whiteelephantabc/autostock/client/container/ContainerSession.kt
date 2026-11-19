package io.github.whiteelephantabc.autostock.client.container

import baritone.api.utils.Helper
import io.github.whiteelephantabc.autostock.client.AutoStockManager
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import org.slf4j.LoggerFactory

object ContainerSession {
	private val logger = LoggerFactory.getLogger("litematica-autostock/container")

	private enum class Phase {
		IDLE,
		OPENING,
		TAKING,
		CLOSING
	}

	private var phase: Phase = Phase.IDLE
	private var timeout: Int = 0
	private var containerId: Int = -1
	private var wanted: Map<Item, Int> = emptyMap()
	private var taken: MutableMap<Item, Int> = mutableMapOf()
	private var onFinished: (Map<Item, Int>) -> Unit = {}
	private var onFailed: (String) -> Unit = {}
	private var closeWarned: Boolean = false

	fun open(
		pos: BlockPos,
		items: Map<Item, Int>,
		onFinished: (Map<Item, Int>) -> Unit,
		onFailed: (String) -> Unit
	): Boolean {
		if (phase != Phase.IDLE) return false

		val mc = Minecraft.getInstance()
		val player = mc.player ?: return false
		val gameMode = mc.gameMode ?: return false

		if (player.containerMenu.containerId != player.inventoryMenu.containerId) {
			closeContainer()

			if (player.containerMenu.containerId != player.inventoryMenu.containerId) return false
		}

		wanted = items.filterValues { it > 0 }
		taken = mutableMapOf()
		closeWarned = false
		this.onFinished = onFinished
		this.onFailed = onFailed
		phase = Phase.OPENING
		timeout = 100
		AutoStockManager.beginSuppress()

		val hit = BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)
		gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit)
		return true
	}

	fun cancel() {
		closeContainer(force = true)
		phase = Phase.IDLE
		containerId = -1
		reset()
	}

	fun tick() {
		val mc = Minecraft.getInstance()
		val player = mc.player ?: return

		when (phase) {
			Phase.OPENING -> tickOpening(player)
			Phase.TAKING -> tickTaking(mc, player)
			Phase.CLOSING -> tickClosing(player)
			Phase.IDLE -> {}
		}
	}

	private fun tickOpening(player: LocalPlayer) {
		if (player.containerMenu.containerId == player.inventoryMenu.containerId) {
			if (--timeout <= 0) fail("container did not open")
			return
		}

		containerId = player.containerMenu.containerId
		phase = Phase.TAKING
	}

	private fun tickTaking(mc: Minecraft, player: LocalPlayer) {
		val menu = player.containerMenu

		wanted.forEach { (item, need) ->
			var left = need

			menu.slots.forEach { slot ->
				if (left <= 0) return@forEach
				if (slot.container === player.inventory) return@forEach

				val stack = slot.item

				if (stack.isEmpty || stack.item !== item) return@forEach

				val take = minOf(stack.count, left)

				if (take <= 0) return@forEach

				if (take == stack.count) {
					val before = stack.count
					click(mc, player, slot.index, 0, ContainerInput.QUICK_MOVE)
					val moved = before - slot.item.count

					if (moved > 0) {
						taken.merge(item, moved) { a, b -> a + b }
						left -= moved
					}
				} else {
					val moved = splitTake(mc, player, menu, slot, take)

					if (moved > 0) {
						taken.merge(item, moved) { a, b -> a + b }
						left -= moved
					}
				}
			}
		}

		phase = Phase.CLOSING
		timeout = 3
	}

	private fun splitTake(
		mc: Minecraft,
		player: LocalPlayer,
		menu: AbstractContainerMenu,
		slot: Slot,
		take: Int
	): Int {
		val source = slot.item

		if (!menu.carried.isEmpty || !slot.mayPlace(source)) return 0

		val stackCount = source.count
		val destinations = findDestinations(menu, player, source)

		if (destinations.isEmpty()) return 0

		click(mc, player, slot.index, 0, ContainerInput.PICKUP)

		if (menu.carried.count != stackCount) return 0

		var toPlace = take

		destinations.forEach { (index, room) ->
			if (toPlace <= 0) return@forEach

			val place = minOf(room, toPlace)

			repeat(place) {
				click(mc, player, index, 1, ContainerInput.PICKUP)
			}

			toPlace -= place
		}

		val placed = stackCount - menu.carried.count
		click(mc, player, slot.index, 0, ContainerInput.PICKUP)

		return placed
	}

	private fun findDestinations(
		menu: AbstractContainerMenu,
		player: LocalPlayer,
		source: ItemStack
	): List<Pair<Int, Int>> {
		val destinations = mutableListOf<Pair<Int, Int>>()

		menu.slots.forEach { slot ->
			if (slot.container === player.inventory) {
				val stack = slot.item

				if (stack.isEmpty && slot.mayPlace(source)) {
					destinations += slot.index to slot.getMaxStackSize(source)
				} else if (ItemStack.isSameItemSameComponents(stack, source) && slot.mayPlace(source)) {
					val room = slot.getMaxStackSize(source) - stack.count

					if (room > 0) destinations += slot.index to room
				}
			}
		}

		return destinations
	}

	private fun click(mc: Minecraft, player: LocalPlayer, slot: Int, button: Int, input: ContainerInput) {
		mc.gameMode?.handleContainerInput(containerId, slot, button, input, player)
	}

	private fun tickClosing(player: LocalPlayer) {
		if (--timeout > 0) return

		closeContainer()

		if (player.containerMenu.containerId != player.inventoryMenu.containerId) {
			timeout = 20
			return
		}

		finish(taken.toMap())
	}

	private fun finish(result: Map<Item, Int>) {
		phase = Phase.IDLE
		containerId = -1
		val callback = onFinished
		reset()
		callback(result)
	}

	private fun fail(reason: String) {
		closeContainer()
		phase = Phase.IDLE
		containerId = -1
		val callback = onFailed
		reset()
		callback(reason)
	}

	private fun reset() {
		wanted = emptyMap()
		taken = mutableMapOf()
		closeWarned = false
		onFinished = {}
		onFailed = {}
	}

	private fun closeContainer(force: Boolean = false) {
		val mc = Minecraft.getInstance()
		val player = mc.player ?: return
		val menu = player.containerMenu

		if (menu.containerId == player.inventoryMenu.containerId) return

		if (!menu.carried.isEmpty) returnCarried(mc, player, menu)

		if (!menu.carried.isEmpty && !force) {
			if (!closeWarned) {
				Helper.HELPER.logDirect("Cannot put the carried stack back, delaying container close")
				closeWarned = true
			}

			logger.warn("Carried stack is not empty, delaying container close")
			return
		}

		if (!menu.carried.isEmpty) Helper.HELPER.logDirect("Closing container with a carried stack, the server may drop it")

		player.closeContainer()
	}

	private fun returnCarried(mc: Minecraft, player: LocalPlayer, menu: AbstractContainerMenu) {
		menu.slots.forEach { slot ->
			if (menu.carried.isEmpty) return

			val stack = slot.item
			val carried = menu.carried

			val canInsert = when {
				stack.isEmpty -> slot.mayPlace(carried)
				ItemStack.isSameItemSameComponents(stack, carried) ->
					slot.mayPlace(carried) && stack.count < slot.getMaxStackSize(carried)
				else -> false
			}

			if (canInsert) {
				mc.gameMode?.handleContainerInput(menu.containerId, slot.index, 0, ContainerInput.PICKUP, player)
			}
		}
	}
}
