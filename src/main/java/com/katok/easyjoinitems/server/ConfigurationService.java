package com.katok.easyjoinitems.server;

import org.bukkit.configuration.InvalidConfigurationException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;

@Singleton
public class ConfigurationService {
    private final ConfigManager configManager;

    @Inject
    public ConfigurationService(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void init() {
        try {
            configManager.loadConfigFile("config.yml");
            configManager.scanPluginFolder();
        } catch (IOException | InvalidConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
}
