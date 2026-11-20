package io.github.whiteelephantabc.autostock.client.mixin

import fi.dy.masa.litematica.network.ServuxLitematicaHandler
import fi.dy.masa.litematica.network.ServuxLitematicaPacket
import fi.dy.masa.malilib.network.IClientPayloadData
import io.github.whiteelephantabc.autostock.client.servux.ServuxClient
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(ServuxLitematicaHandler::class)
abstract class ServuxLitematicaHandlerMixin {
	@Inject(
		method = ["decodeClientData(Lnet/minecraft/resources/Identifier;Lfi/dy/masa/malilib/network/IClientPayloadData;)V"],
		at = [At("HEAD")]
	)
	private fun autostockHandleServuxPacket(
		channel: Identifier,
		data: IClientPayloadData,
		ci: CallbackInfo
	) {
		if (data is ServuxLitematicaPacket) {
			Minecraft.getInstance().execute { ServuxClient.handlePacket(data) }
		}
	}
}
