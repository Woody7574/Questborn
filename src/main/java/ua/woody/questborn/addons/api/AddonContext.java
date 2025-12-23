package ua.woody.questborn.addons.api;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.logging.Logger;

public record AddonContext(
        Plugin plugin,
        Logger logger,
        File dataFolder
) {}
