package ua.woody.questborn.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.config.GuiConfig;

import java.util.List;

public abstract class BaseGui implements InventoryHolder {

    protected final QuestbornPlugin plugin;
    protected final Player player;
    protected final Inventory inventory;
    protected final GuiConfig guiConfig;

    public BaseGui(QuestbornPlugin plugin, Player player, int size, String title) {
        this.plugin = plugin;
        this.player = player;
        this.guiConfig = plugin.getGuiConfig(); // Отримуємо конфігурацію GUI
        this.inventory = plugin.getServer().createInventory(this, size,
                org.bukkit.ChatColor.translateAlternateColorCodes('&', title));
    }

    /**
     * Заповнює рамку GUI згідно з конфігурацією з config.yml
     */
    protected void fillFrame() {
        if (!guiConfig.isFillEnabled()) {
            return; // Якщо заповнення вимкнене в конфігурації
        }

        Material fillMaterial = guiConfig.getFillMaterial();
        List<Integer> fillSlots = guiConfig.getFillSlots();

        // Створюємо предмет для заповнення
        ItemStack pane = new ItemStack(fillMaterial);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" "); // Порожня назва
            pane.setItemMeta(meta);
        }

        // Заповнюємо вказані слоти
        for (int slot : fillSlots) {
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, pane);
            }
        }
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }
}