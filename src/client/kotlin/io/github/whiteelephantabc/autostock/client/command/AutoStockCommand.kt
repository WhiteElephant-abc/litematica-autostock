package io.github.whiteelephantabc.autostock.client.command

import baritone.api.IBaritone
import baritone.api.command.Command
import baritone.api.command.argument.IArgConsumer
import baritone.api.command.datatypes.ItemById
import baritone.api.command.exception.CommandInvalidTypeException
import io.github.whiteelephantabc.autostock.client.AutoStockManager
import io.github.whiteelephantabc.autostock.client.config.AutoStockConfig
import io.github.whiteelephantabc.autostock.client.materials.MaterialRequest
import java.util.stream.Stream

class AutoStockCommand(baritone: IBaritone) : Command(baritone, "stock") {
	override fun execute(label: String, args: IArgConsumer) {
		val requests = buildList {
			while (args.hasAny()) {
				val item = args.getDatatypeFor(ItemById.INSTANCE)
				val count = if (args.hasAny() && args.peek().value.toIntOrNull() != null) {
					val countArg = args.peek()
					val value = args.getAs(Int::class.javaObjectType)

					if (value <= 0) {
						throw CommandInvalidTypeException(countArg, "positive integer", value.toString())
					}

					value
				} else {
					AutoStockConfig.defaultItemCount
				}

				add(MaterialRequest(item, count))
			}
		}

		AutoStockManager.start(requests)
	}

	override fun tabComplete(label: String, args: IArgConsumer): Stream<String> {
		while (args.has(2)) {
			if (args.peekDatatypeOrNull(ItemById.INSTANCE) == null) {
				return Stream.empty()
			}

			args.get()

			if (args.has(2) && args.peek().value.toIntOrNull() != null) {
				args.get()
			}
		}

		return args.tabCompleteDatatype(ItemById.INSTANCE)
	}

	override fun getShortDesc(): String =
		"Collect Litematica materials or requested items from nearby containers"

	override fun getLongDesc(): List<String> =
		listOf(
			"Without arguments, collects the missing materials of the selected Litematica placement.",
			"With arguments, collects the given items instead.",
			"An item without an explicit count uses the default item count from the config.",
			"",
			"Usage:",
			"> stock",
			"> stock <item> [count] [<item> [count] ...]"
		)
}
