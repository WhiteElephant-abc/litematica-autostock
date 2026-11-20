package io.github.whiteelephantabc.autostock.client.servux

import fi.dy.masa.litematica.network.ServuxLitematicaPacket
import fi.dy.masa.malilib.util.data.tag.CompoundData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.minecraft.core.BlockPos
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

object ServuxClient {
	private val logger = LoggerFactory.getLogger("litematica-autostock/servux")
	private var joined: Boolean = false
	private var retryTicks: Int = 0
	private var attempts: Int = 0
	private val pending = mutableMapOf<BlockPos, CompletableDeferred<CompoundData>>()

	var available: Boolean = false
		private set

	fun init() {
		ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
			joined = true
			available = false
			attempts = 0
			retryTicks = 0
		}

		ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
			joined = false
			available = false
			retryTicks = 0
			pending.clear()
		}
	}

	fun tick() {
		if (!joined || available || attempts >= 10) {
			return
		}

		if (--retryTicks > 0) {
			return
		}

		retryTicks = 40
		attempts++
		sendMetadataRequest()
	}

	suspend fun requestBlockEntity(pos: BlockPos): CompoundData? {
		if (!available) {
			return null
		}

		val deferred = CompletableDeferred<CompoundData>()
		pending[pos] = deferred

		return try {
			ClientPlayNetworking.send(
				ServuxLitematicaPacket.Payload(ServuxLitematicaPacket.BlockEntityRequest(pos))
			)
			withTimeoutOrNull(3.seconds) { deferred.await() }.also {
				if (it == null) {
					logger.warn("Timed out waiting for Servux response at {}", pos)
				}
			}
		} catch (e: CancellationException) {
			throw e
		} catch (e: Throwable) {
			logger.warn("Failed to send Servux block entity request", e)
			null
		} finally {
			pending.remove(pos)
		}
	}

	private fun sendMetadataRequest() {
		try {
			val nbt = CompoundData()
			nbt.putInt("version", ServuxLitematicaPacket.PROTOCOL_VERSION)
			ClientPlayNetworking.send(
				ServuxLitematicaPacket.Payload(ServuxLitematicaPacket.MetadataRequest(nbt))
			)
		} catch (e: Throwable) {
			logger.warn("Failed to send Servux metadata request", e)
		}
	}

	fun handlePacket(data: ServuxLitematicaPacket) {
		when (data.type) {
			ServuxLitematicaPacket.Type.PACKET_S2C_METADATA -> {
				if (available) {
					return
				}

				val version = data.compound.getIntOrDefault("version", -1)
				val servux = data.compound.getStringOrDefault("servux", "?")

				if (version == ServuxLitematicaPacket.PROTOCOL_VERSION) {
					available = true
					logger.info("Servux connected: {}", servux)
				} else {
					logger.warn("Servux protocol mismatch: got {} expected {}", version, ServuxLitematicaPacket.PROTOCOL_VERSION)
				}
			}
			ServuxLitematicaPacket.Type.PACKET_S2C_BLOCK_NBT_RESPONSE_SIMPLE -> {
				pending.remove(data.pos)?.complete(data.compound)
			}
			else -> {
				logger.debug("Ignoring Servux packet type {}", data.type)
			}
		}
	}
}
