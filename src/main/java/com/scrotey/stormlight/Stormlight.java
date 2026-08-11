package com.scrotey.stormlight;

import com.scrotey.stormlight.attachment.ModAttachments;
import com.scrotey.stormlight.block.ModBlocks;
import com.scrotey.stormlight.block.entity.ModBlockEntities;
import com.scrotey.stormlight.breathing.StormlightManager;
import com.scrotey.stormlight.component.ModComponents;
import com.scrotey.stormlight.highstorm.HighstormCommands;
import com.scrotey.stormlight.highstorm.HighstormManager;
import com.scrotey.stormlight.item.ModItems;
import com.scrotey.stormlight.network.ModNetworking;
import com.scrotey.stormlight.recipe.ModRecipes;
import com.scrotey.stormlight.screen.ModMenuTypes;
import com.scrotey.stormlight.particle.ModParticles;
import com.scrotey.stormlight.progression.RadiantLecternInteraction;
import com.scrotey.stormlight.progression.RadiantProgression;
import com.scrotey.stormlight.highstorm.SphereDecayManager;
import com.scrotey.stormlight.worldgen.ModWorldGeneration;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Stormlight implements ModInitializer {
    public static final String MOD_ID = "stormlight";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModComponents.initialize();
        ModItems.initialize();
        RadiantProgression.initialize();
        ModAttachments.initialize();
        ModBlocks.initialize();
        ModWorldGeneration.initialize();
        ModBlockEntities.initialize();
        ModMenuTypes.initialize();
        ModRecipes.initialize();
        ModNetworking.initialize();
        RadiantLecternInteraction.initialize();
        ModParticles.initialize();
        HighstormManager.initialize();
        SphereDecayManager.initialize();
        HighstormCommands.initialize();
        StormlightManager.initialize();

        LOGGER.info("Hello Kaladin!");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
