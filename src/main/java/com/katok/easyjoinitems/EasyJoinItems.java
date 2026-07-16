package com.katok.easyjoinitems;

import com.katok.easyjoinitems.server.ConfigManager;
import com.katok.easyjoinitems.server.ConfigurationService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Objects;

public class EasyJoinItems extends JavaPlugin implements CommandExecutor, Listener {
    public final static NamespacedKey PLAYER_COMMAND_KEY = new NamespacedKey("easyjoinitems", "playercommandkey");
    public final static NamespacedKey CONSOLE_COMMAND_KEY = new NamespacedKey("easyjoinitems", "consolecommandkey");
    public final static NamespacedKey CAN_DROP_KEY = new NamespacedKey("easyjoinitems", "candropkey");

    private final ConfigManager configManager = new ConfigManager(this);
    private final ConfigurationService configurationService = new ConfigurationService(configManager);

    @Override
    public void onEnable() {
        reload();

        Objects.requireNonNull(getCommand("easyjoinitemsreload")).setExecutor(this);
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        reload();
        sender.sendMessage(ConfigManager.colorComponent("&aReload successful"));
        return true;
    }

    public void reload() {
        configurationService.init();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PlayerJoinEvent event) {
        YamlConfiguration configuration = Objects.requireNonNull(configManager.getConfiguration("config.yml"));
        ConfigurationSection itemsCFG = Objects.requireNonNull(configuration.getConfigurationSection("items"));
        Player player = event.getPlayer();

        for (String key : itemsCFG.getKeys(false)) {
            ConfigurationSection itemCFG = Objects.requireNonNull(itemsCFG.getConfigurationSection(key));

            ItemStack itemStack = new ItemStack(
                    Objects.requireNonNull(Material.getMaterial(Objects.requireNonNull(itemCFG.getString("material")))),
                    itemCFG.getInt("amount"));

            ItemMeta itemMeta = itemStack.getItemMeta();
            String name = itemCFG.getString("name");
            if (name != null) {
                itemMeta.displayName(ConfigManager.colorComponent(configManager.parse(name, player)));
            }
            String lore = itemCFG.getString("lore");
            if (lore != null && !lore.isEmpty()) {
                itemMeta.lore(new ArrayList<>() {{
                    add(ConfigManager.colorComponent(configManager.parse(lore, player)));
                }});
            }

            StringBuilder playerCommands = new StringBuilder();
            for (String playerCommand : itemCFG.getStringList("commands-player")) {
                if (!playerCommands.isEmpty()) {
                    playerCommands.append("@!@");
                }
                playerCommands.append(playerCommand);
            }
            itemMeta.getPersistentDataContainer().set(PLAYER_COMMAND_KEY, PersistentDataType.STRING, playerCommands.toString());

            StringBuilder consoleCommands = new StringBuilder();
            for (String consoleCommand : itemCFG.getStringList("commands-console")) {
                if (!consoleCommands.isEmpty()) {
                    consoleCommands.append("@!@");
                }
                consoleCommands.append(consoleCommand);
            }
            itemMeta.getPersistentDataContainer().set(CONSOLE_COMMAND_KEY, PersistentDataType.STRING, consoleCommands.toString());
            itemMeta.getPersistentDataContainer().set(CAN_DROP_KEY, PersistentDataType.BOOLEAN, itemCFG.getBoolean("can-drop"));

            itemStack.setItemMeta(itemMeta);

            player.getInventory().setItem(itemCFG.getInt("slot"), itemStack);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack itemStack = event.getItemDrop().getItemStack();

        Boolean canDrop = itemStack.getPersistentDataContainer().get(CAN_DROP_KEY, PersistentDataType.BOOLEAN);

        if (canDrop == null || canDrop) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        ItemStack itemStack = event.getItem();
        if (itemStack == null) {
            return;
        }

        Player player = event.getPlayer();

        String playerCommands = itemStack.getPersistentDataContainer().get(PLAYER_COMMAND_KEY, PersistentDataType.STRING);
        if (playerCommands != null) {
            String[] commands = playerCommands.split("@!@");

            for (String command : commands) {
                if (command.isEmpty()) {
                    continue;
                }

                Bukkit.dispatchCommand(player, configManager.parse(command, player));
            }
        }

        String consoleCommands = itemStack.getPersistentDataContainer().get(CONSOLE_COMMAND_KEY, PersistentDataType.STRING);
        if (consoleCommands != null) {
            String[] commands = consoleCommands.split("@!@");

            for (String command : commands) {
                if (command.isEmpty()) {
                    continue;
                }
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), configManager.parse(command, player));
            }
        }
    }
}
