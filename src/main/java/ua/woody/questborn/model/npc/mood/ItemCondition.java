package ua.woody.questborn.model.npc.mood;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ua.woody.questborn.model.NpcConfig;

public class ItemCondition implements MoodCondition {
    private final Material material;
    private final String materialName;
    private final String operator;
    private final int amount;
    private final String amountExpression;

    public ItemCondition(String materialName, String amountExpression) {
        this.materialName = materialName;
        this.amountExpression = amountExpression;
        this.material = Material.matchMaterial(materialName.toUpperCase());

        amountExpression = amountExpression.trim();
        if (amountExpression.startsWith(">=")) {
            this.operator = ">=";
            this.amount = Integer.parseInt(amountExpression.substring(2).trim());
        } else if (amountExpression.startsWith("<=")) {
            this.operator = "<=";
            this.amount = Integer.parseInt(amountExpression.substring(2).trim());
        } else if (amountExpression.startsWith(">")) {
            this.operator = ">";
            this.amount = Integer.parseInt(amountExpression.substring(1).trim());
        } else if (amountExpression.startsWith("<")) {
            this.operator = "<";
            this.amount = Integer.parseInt(amountExpression.substring(1).trim());
        } else if (amountExpression.startsWith("=")) {
            this.operator = "=";
            this.amount = Integer.parseInt(amountExpression.substring(1).trim());
        } else {
            this.operator = ">=";
            this.amount = Integer.parseInt(amountExpression);
        }
    }

    public String getMaterialName() {
        return materialName;
    }

    public String getAmountExpression() {
        return amountExpression;
    }

    @Override
    public boolean check(Player player, NpcConfig npc) {
        if (material == null) return false;

        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }

        switch (operator) {
            case ">=": return count >= amount;
            case "<=": return count <= amount;
            case ">": return count > amount;
            case "<": return count < amount;
            case "=": return count == amount;
            default: return false;
        }
    }
}
