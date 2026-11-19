package io.github.whiteelephantabc.autostock.client.mixin

import io.github.whiteelephantabc.autostock.client.AutoStockManager
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.MenuScreens
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.MenuType
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(MenuScreens::class)
abstract class MenuScreensMixin {
	private companion object {
		@JvmStatic
		@Inject(
			method = ["create(Lnet/minecraft/world/inventory/MenuType;Lnet/minecraft/client/Minecraft;ILnet/minecraft/network/chat/Component;)V"],
			at = [At("RETURN")]
		)
		private fun autostockHideSuppressedContainer(
			type: MenuType<*>,
			minecraft: Minecraft,
			containerId: Int,
			title: Component,
			ci: CallbackInfo
		) {
			if (AutoStockManager.shouldSuppressScreen(containerId)) {
				minecraft.gui.setScreen(null)
			}
		}
	}
}
