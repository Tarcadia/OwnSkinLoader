package net.tarcadia.minecraft.ownskinloader;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OwnSkinLoader implements ModInitializer {

    private static final Logger LOGGER = LogManager.getLogger();
    /**
     * Runs the mod initializer.
     */
    @Override
    public void onInitialize() {
        LOGGER.info("OwnSkinLoader version ${version} loaded.");
    }
}
