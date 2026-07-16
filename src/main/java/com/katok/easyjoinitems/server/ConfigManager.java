package com.katok.easyjoinitems.server;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class ConfigManager {
    public static final String MESSAGE_ERROR = "&cERROR";

    private final JavaPlugin instance;

    private final HashMap<String, YamlConfiguration> configs = new HashMap<>();

    private Boolean isPlaceholderAPIEnable = null;

    @Inject
    public ConfigManager(JavaPlugin instance) {
        this.instance = instance;
    }

    public static String color(String message) {
        if (message == null) {
            return "";
        }

        Pattern pattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            String color = matcher.group(1);
            String replacement = ChatColor.of("#" + color).toString();
            message = message.replace("&#" + color, replacement);
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static Component colorComponent(String message) {
        Component result = Component.empty();

        if (message == null) {
            return result;
        }

        {
            Pattern pattern = Pattern.compile("&([A-Za-z0-9])");
            Matcher matcher = pattern.matcher(message);
            while (matcher.find()) {
                String color = matcher.group(1);
                message = message.replaceAll("&" + color, "§" + color);
            }
        }

        List<String> colors_text = new ArrayList<>(List.of(message.split("&#")));

        for(int i = 1; i < colors_text.size(); i++) {
            colors_text.set(i, "#" + colors_text.get(i));
        }

        for(String text: colors_text) {
            Pattern pattern = Pattern.compile("#([A-Fa-f0-9]{6})");
            Matcher matcher = pattern.matcher(text);
            if(matcher.find()) {
                String color = matcher.group(1);
                result = result.append(Component.text(text.replaceAll("#" + color, "")).color(TextColor.fromHexString("#" + color)));
            } else {
                result = result.append(Component.text(text));
            }
        }
        return result;
    }

    public String parse(String messages, OfflinePlayer player) {
        if (isPlaceholderAPIEnable == null) {
            isPlaceholderAPIEnable = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
        }

        if (!isPlaceholderAPIEnable ) {
            return messages;
        }

        return PlaceholderAPI.setPlaceholders(player, messages);
    }

    /**
     * Выдает конфигурационный файл, по пути, от папки самого плагина, например - getConfiguration("config.yml")
     * @return конфигурацию
     */
    @Nullable
    public YamlConfiguration getConfiguration(String path) {
        return configs.get(path);
    }

    /**
     * Сканирует конфигурации, и выдает список конфигураций путь которых начинаеться с startWithPath
     * @param startWithPath - путь
     * @return список конфигураций
     */
    public List<YamlConfiguration> getConfigurations(String startWithPath) {
        List<YamlConfiguration> result = new ArrayList<>();
        for(String path: configs.keySet()) {
            if(!path.startsWith(startWithPath)) continue;

            result.add(configs.get(path));
        }

        return result;
    }

    /**
     * Сканирует конфигурации, и выдает список путей конфигураций путь которых начинаеться с startWithPath
     * @param startWithPath - путь
     * @return список путей конфигураций
     */
    public List<String> getConfigurationPaths(String startWithPath) {
        List<String> result = new ArrayList<>();
        for(String path: configs.keySet()) {
            if(!path.startsWith(startWithPath)) continue;

            result.add(path);
        }

        return result;
    }

    /**
     * Загружает определенный файл из resources в папку плагина
     * @param path путь к файлу
     */
    public void loadConfigFile(String path) throws IOException, InvalidConfigurationException {
        File configFile = new File(instance.getDataFolder().getAbsolutePath() + File.separator + path);

        if(!configFile.exists()) instance.saveResource(path, false);

        YamlConfiguration configCfg = new YamlConfiguration();
        configCfg.load(configFile);

        if(configs.containsKey(path)) {
            configs.replace(path, configCfg);
        } else {
            configs.put(path, configCfg);
        }
    }

    /**
     * Сохраняет конфигурационный файл по определенному пути
     * @param path - путь где будет сохранена конфигурация
     * @param configuration - сам конфигурационный файл
     */
    public void saveConfiguration(String path, FileConfiguration configuration) throws IOException {
        File configFile = new File(instance.getDataFolder().toPath().toAbsolutePath() + File.separator + path);

        if(!configFile.exists()) {
            configFile.createNewFile();
        }
        if(configs.containsKey(path)) configs.remove(path);

        configuration.save(configFile);

        configs.put(path, YamlConfiguration.loadConfiguration(configFile));
    }

    /**
     * Сохраняет определенную конфигурацию с оперативной памяти в папку плагина
     * @param path - путь конфигурации
     */
    public void saveConfiguration(String path) throws IOException {
        File configFile = new File(instance.getDataFolder().toPath().toAbsolutePath() + File.separator + path);
        if(!configFile.exists()) {
            configFile.createNewFile();
        }
        YamlConfiguration configuration = new YamlConfiguration();
        if(configs.containsKey(path)) configuration = configs.get(path);

        configuration.save(configFile);
    }

    /**
     * Сохраняет все конфигурационные файлы в папку плагина
     */
    public void saveAll() {
        for(String path: configs.keySet()) {
            File configFile = new File(instance.getDataFolder().toPath().toAbsolutePath() + File.separator + path);
            try {
                configs.get(path).save(configFile);
            } catch (IOException e) {
                throw new RuntimeException("Не удалось сохранить конфиг " + path, e);
            }
        }
    }

    /**
     * Сканирует папку плагина, и сохраняет все файлы с расширением .yml
     */
    public void scanPluginFolder() throws IOException, InvalidConfigurationException {
        readFiles(new File(instance.getDataFolder().toPath().toAbsolutePath().toString()));
    }

    private void readFiles(File baseDirectory) throws IOException, InvalidConfigurationException {
        if (baseDirectory.isDirectory()){
            for (File file : baseDirectory.listFiles()) {
                if(file.isFile() && file.getAbsolutePath().endsWith(".yml")) {
                    String path = file.getAbsolutePath().substring((instance.getDataFolder().toPath().toAbsolutePath() + File.separator).length());
                    loadConfigFile(path);
                } else {
                    readFiles(file);
                }
            }
        }
    }
}