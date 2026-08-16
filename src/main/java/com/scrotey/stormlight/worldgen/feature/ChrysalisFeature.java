package com.scrotey.stormlight.worldgen.feature;

import com.mojang.serialization.Codec;
import com.scrotey.stormlight.Stormlight;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;

import java.util.Optional;

/**
 * Places the authored stormlight:chrysalis structure template.
 *
 * The chrysalis geometry now lives entirely in:
 * data/stormlight/structure/chrysalis.nbt
 *
 * That means changing the model no longer requires touching worldgen code.
 */
public final class ChrysalisFeature
        extends Feature<NoneFeatureConfiguration> {

    private static final Identifier TEMPLATE_ID =
            Stormlight.id("chrysalis");

    /*
     * The template's bottom layer is placed one block into the terrain so the
     * chrysalis feels heavy rather than perched on the surface.
     */
    private static final int BURIAL_DEPTH = 4;

    public ChrysalisFeature(
            Codec<NoneFeatureConfiguration> codec
    ) {
        super(codec);
    }

    @Override
    public boolean place(
            FeaturePlaceContext<NoneFeatureConfiguration> context
    ) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();

        StructureTemplateManager templateManager =
                level.getLevel().getStructureManager();

        Optional<StructureTemplate> optionalTemplate =
                templateManager.get(TEMPLATE_ID);

        if (optionalTemplate.isEmpty()) {
            Stormlight.LOGGER.error(
                    "Could not find structure template {}. Expected data/stormlight/structure/chrysalis.nbt",
                    TEMPLATE_ID
            );
            return false;
        }

        StructureTemplate template = optionalTemplate.get();
        Vec3i size = template.getSize();

        if (size.getX() <= 0 || size.getY() <= 0 || size.getZ() <= 0) {
            Stormlight.LOGGER.error(
                    "Structure template {} is empty.",
                    TEMPLATE_ID
            );
            return false;
        }

        Rotation rotation = switch (random.nextInt(4)) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };

        /*
         * Rotate around the centre of the authored template so all four
         * rotations stay centred on the surface position chosen by worldgen.
         */
        BlockPos pivot = new BlockPos(
                size.getX() / 2,
                0,
                size.getZ() / 2
        );

        BlockPos worldCentre =
                context.origin().below(BURIAL_DEPTH);

        BlockPos placementOrigin =
                worldCentre.offset(
                        -pivot.getX(),
                        0,
                        -pivot.getZ()
                );

        StructurePlaceSettings settings =
                new StructurePlaceSettings()
                        .setMirror(Mirror.NONE)
                        .setRotation(rotation)
                        .setRotationPivot(pivot)
                        .setIgnoreEntities(false)
                        .setRandom(random)
                        .addProcessor(
                                BlockIgnoreProcessor.STRUCTURE_AND_AIR
                        );

        return template.placeInWorld(
                level,
                placementOrigin,
                placementOrigin,
                settings,
                random,
                2
        );
    }
}
