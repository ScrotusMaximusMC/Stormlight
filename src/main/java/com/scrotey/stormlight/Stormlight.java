package com.scrotey.stormlight;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.scrotey.stormlight.component.ModComponents;
import com.scrotey.stormlight.item.ModItems;
import com.scrotey.stormlight.highstorm.HighstormManager;
import com.scrotey.stormlight.block.ModBlocks;
import com.scrotey.stormlight.block.entity.ModBlockEntities;
import com.scrotey.stormlight.screen.ModMenuTypes;

public class Stormlight implements ModInitializer {
	public static final String MOD_ID = "stormlight";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModComponents.initialize();
		ModItems.initialize();
		ModBlocks.initialize();
		ModBlockEntities.initialize();
		ModMenuTypes.initialize();
		HighstormManager.initialize();


		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello Kaladin!");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
