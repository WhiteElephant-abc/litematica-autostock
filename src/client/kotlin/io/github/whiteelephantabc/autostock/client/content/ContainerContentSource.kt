package io.github.whiteelephantabc.autostock.client.content

import io.github.whiteelephantabc.autostock.client.scan.ContainerTarget
import kotlinx.coroutines.flow.Flow
import net.minecraft.world.item.ItemStack

data class ContainerContent(val target: ContainerTarget, val items: List<ItemStack>)

interface ContainerContentSource {
	fun requestAll(targets: List<ContainerTarget>): Flow<ContainerContent>
}
