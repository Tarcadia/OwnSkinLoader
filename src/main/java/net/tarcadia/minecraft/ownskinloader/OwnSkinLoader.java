package net.tarcadia.minecraft.ownskinloader;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;

public class OwnSkinLoader implements ModInitializer {

    private static final Logger LOGGER = LogUtils.getLogger();
    /**
     * Runs the mod initializer.
     */
    @Override
    public void onInitialize() {
        LOGGER.info("OwnSkinLoader loaded.");
    }
}
