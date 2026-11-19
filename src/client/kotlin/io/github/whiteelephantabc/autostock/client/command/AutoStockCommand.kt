package io.github.whiteelephantabc.autostock.client.command

import baritone.api.IBaritone
import baritone.api.command.Command
import baritone.api.command.argument.IArgConsumer
import baritone.api.command.datatypes.ItemById
import baritone.api.command.exception.CommandInvalidTypeException
import io.github.whiteelephantabc.autostock.client.AutoStockManager
import io.github.whiteelephantabc.autostock.client.materials.MaterialRequest
import java.util.stream.Stream

class AutoStockCommand(baritone: IBaritone) : Command(baritone, "stock") {
	override fun execute(label: String, args: IArgConsumer) {
		val requests = buildList {
			while (args.hasAny()) {
				val item = args.getDatatypeFor(ItemById.INSTANCE)
				val countArg = args.peek()
				val count = args.getAs(Int::class.javaObjectType)

				if (count <= 0) {
					throw CommandInvalidTypeException(countArg, "positive integer", count.toString())
				}

				add(MaterialRequest(item, count))
			}
		}

		AutoStockManager.start(requests)
	}

	override fun tabComplete(label: String, args: IArgConsumer): Stream<String> {
		var index = 0

		while (args.has(2)) {
			if (index % 2 == 0 && args.peekDatatypeOrNull(ItemById.INSTANCE) == null) {
				return Stream.empty()
			}

			args.get()
			index++
		}

		return if (index % 2 == 0) {
			args.tabCompleteDatatype(ItemById.INSTANCE)
		} else {
			Stream.empty()
		}
	}

	override fun getShortDesc(): String =
		"Collect Litematica materials or requested items from nearby containers"

	override fun getLongDesc(): List<String> =
		listOf(
			"Without arguments, collects the missing materials of the selected Litematica placement.",
			"With arguments, collects the given items and counts instead.",
			"",
			"Usage:",
			"> stock",
			"> stock <item> <count> [<item> <count> ...]"
		)
}
