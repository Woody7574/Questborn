package ua.woody.questborn.gui;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import ua.woody.questborn.QuestbornPlugin;
import ua.woody.questborn.config.TopConfig;
import ua.woody.questborn.lang.LanguageManager;
import ua.woody.questborn.model.QuestTypeConfig;
import ua.woody.questborn.managers.QuestManager;

import java.util.ArrayList;
import java.util.List;

public class MainMenuGui extends BaseGui {

    private final QuestManager questManager;
    private final LanguageManager lang;
    private final TopConfig topConfig;

    public MainMenuGui(QuestbornPlugin plugin, org.bukkit.entity.Player player) {
        super(plugin, player, 45, plugin.getLanguage().tr("gui.main.title"));
        this.questManager = plugin.getQuestManager();
        this.lang = plugin.getLanguage();
        this.topConfig = plugin.getTopConfig();
        build();
    }

    private void build() {
        fillFrame();

        // Додаємо кнопки типів квестів
        List<QuestTypeConfig> enabledTypes = questManager.getQuestTypeManager().getEnabledTypes();
        for (QuestTypeConfig typeConfig : enabledTypes) {
            addTypeButton(typeConfig);
        }

        // Додаємо кнопку топу, якщо він увімкнений
        if (topConfig.isEnabled()) {
            addTopButton();
        }
    }

    private void addTypeButton(QuestTypeConfig typeConfig) {
        int slot = typeConfig.getSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            plugin.getLogger().warning("Invalid GUI slot for quest type: " + typeConfig.getId() + " (slot: " + slot + ")");
            return;
        }

        ItemStack item = new ItemStack(typeConfig.getMaterial());
        ItemMeta meta = item.getItemMeta();

        // Встановлення Custom Model Data
        if (typeConfig.getCustomModelData() != null) {
            meta.setCustomModelData(typeConfig.getCustomModelData());
        }

        meta.setDisplayName(typeConfig.getDisplayName());

        List<String> lore = new ArrayList<>();

        List<String> customLore = typeConfig.getLore();
        if (customLore != null && !customLore.isEmpty()) {
            int questCount = questManager.getByType(typeConfig).size();

            for (String loreLine : customLore) {
                String processedLine = loreLine.replace("{count}", String.valueOf(questCount));
                lore.add(lang.color(processedLine));
            }
        }

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);

        inventory.setItem(slot, item);
    }

    private void addTopButton() {
        int slot = topConfig.getGuiSlot();
        if (slot < 0 || slot >= inventory.getSize()) {
            plugin.getLogger().warning("Invalid GUI slot for top button: " + slot);
            return;
        }

        // Створюємо предмет для кнопки топу
        ItemStack topItem = topConfig.createGuiItem();
        ItemMeta meta = topItem.getItemMeta();

        if (meta != null) {
            // Додаємо PDC маркер для ідентифікації кнопки топу
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "top-menu"),
                    PersistentDataType.BYTE,
                    (byte) 1
            );

            topItem.setItemMeta(meta);
        }

        inventory.setItem(slot, topItem);
    }
}