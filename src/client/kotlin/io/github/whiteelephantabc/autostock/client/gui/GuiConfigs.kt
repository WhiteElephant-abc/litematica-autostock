package io.github.whiteelephantabc.autostock.client.gui

import fi.dy.masa.malilib.config.gui.GuiModConfigs
import io.github.whiteelephantabc.autostock.LitematicaAutostock
import io.github.whiteelephantabc.autostock.client.config.AutoStockConfigs

class GuiConfigs : GuiModConfigs(
	LitematicaAutostock.MOD_ID,
	AutoStockConfigs.OPTIONS,
	"${LitematicaAutostock.MOD_ID}.gui.title.configs"
)
