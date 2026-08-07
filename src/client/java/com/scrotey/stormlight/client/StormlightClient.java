package com.scrotey.stormlight.client;

import com.scrotey.stormlight.screen.ModMenuTypes;
import com.scrotey.stormlight.screen.SphereJarScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class StormlightClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(
				ModMenuTypes.SPHERE_JAR,
				SphereJarScreen::new
		);
	}
}