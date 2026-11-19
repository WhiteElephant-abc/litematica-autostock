package io.github.whiteelephantabc.autostock.client.materials

import net.minecraft.world.item.Item

data class MaterialRequest(val item: Item, val count: Int)
