package io.github.whiteelephantabc.autostock.client

import baritone.api.BaritoneAPI
import fi.dy.masa.malilib.config.ConfigManager
import fi.dy.masa.malilib.event.InitializationHandler
import fi.dy.masa.malilib.gui.GuiBase
import fi.dy.masa.malilib.interfaces.IInitializationHandler
import fi.dy.masa.malilib.registry.Registry
import fi.dy.masa.malilib.util.data.ModInfo
import io.github.whiteelephantabc.autostock.LitematicaAutostock
import io.github.whiteelephantabc.autostock.client.command.AutoStockCommand
import io.github.whiteelephantabc.autostock.client.config.AutoStockConfigHandler
import io.github.whiteelephantabc.autostock.client.gui.GuiConfigs
import io.github.whiteelephantabc.autostock.client.servux.ServuxClient
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import java.util.function.Supplier

object LitematicaAutostockClient : ClientModInitializer {
	private var baritoneCommandRegistered: Boolean = false

	override fun onInitializeClient() {
		ServuxClient.init()
		InitializationHandler.getInstance().registerInitializationHandler(object : IInitializationHandler {
			override fun registerModHandlers() {
				ConfigManager.getInstance().registerConfigHandler(LitematicaAutostock.MOD_ID, AutoStockConfigHandler)
				Registry.CONFIG_SCREEN.registerConfigScreenFactory(
					ModInfo(LitematicaAutostock.MOD_ID, LitematicaAutostock.MOD_NAME, Supplier<GuiBase> { GuiConfigs() })
				)
			}
		})
		ClientTickEvents.END_CLIENT_TICK.register { tick() }
	}

	private fun tick() {
		if (!baritoneCommandRegistered) {
			val baritone = BaritoneAPI.getProvider().primaryBaritone

			if (baritone != null) {
				baritone.commandManager.registry.register(AutoStockCommand(baritone))
				baritoneCommandRegistered = true
			}
		}

		AutoStockManager.tick()
	}
}
